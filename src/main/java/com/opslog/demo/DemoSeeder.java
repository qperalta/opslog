package com.opslog.demo;

import com.opslog.auth.AuthService;
import com.opslog.auth.RegisterRequest;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.catalog.CatalogService;
import com.opslog.catalog.CustomExerciseService;
import com.opslog.catalog.EquipmentResponse;
import com.opslog.catalog.ExerciseResponse;
import com.opslog.checkin.CheckInRequest;
import com.opslog.checkin.CheckInService;
import com.opslog.location.LocationResponse;
import com.opslog.location.LocationService;
import com.opslog.logging.LoggedSetRequest;
import com.opslog.logging.LoggingService;
import com.opslog.plan.Actor;
import com.opslog.plan.PlanDayRequest;
import com.opslog.plan.PlanDayService;
import com.opslog.plan.PlanMode;
import com.opslog.plan.ProgramService;
import com.opslog.plan.ProgramTemplateResponse;
import com.opslog.plan.ProgramTemplateResponse.WorkoutResponse;
import com.opslog.plan.UserProgramRequest;
import com.opslog.session.SessionExerciseResponse;
import com.opslog.session.SessionResponse;
import com.opslog.session.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/* two weeks of history for the demo account, built through the same services the app uses:
 *  plan the day, check in, let the rules engine build the session, then log sets, so tiers,
 *  substitutions, and progression are real */
@Service
public class DemoSeeder {

    static final int DAYS = 14;
    static final String PROGRAM = "Upper/Lower 4-Day";
    static final String HOME = "Kaimana Downtown";
    static final String SMALL_GYM = "Kaimana Kapolei";

    /* sleep and soreness per day, oldest first: mostly Full, some Reduced and Recovery mornings */
    private static final int[][] MORNINGS = {
            {4, 2}, {5, 1}, {3, 3}, {4, 2}, {2, 4}, {4, 1}, {5, 2},
            {4, 2}, {3, 3}, {5, 1}, {4, 2}, {4, 2}, {2, 5}, {5, 1}};
    /* by training day: one at the small gym (substitutions), one skipped accessory, one hard top set (load holds) */
    private static final int SMALL_GYM_DAY = 4;
    private static final int SKIP_DAY = 2;
    private static final int HARD_DAY = 5;

    public record Account(String email, String password, String displayName) {}

    private final TransactionTemplate transaction;
    private final UserAccountRepository users;
    private final AuthService auth;
    private final ProgramService programs;
    private final PlanDayService planDays;
    private final CheckInService checkIns;
    private final LocationService locations;
    private final SessionService sessions;
    private final LoggingService logging;
    private final CustomExerciseService exercises;
    private final CatalogService catalog;

    DemoSeeder(PlatformTransactionManager transactions, UserAccountRepository users, AuthService auth, ProgramService programs,
               PlanDayService planDays, CheckInService checkIns, LocationService locations, SessionService sessions,
               LoggingService logging, CustomExerciseService exercises, CatalogService catalog) {
        this.transaction = new TransactionTemplate(transactions);
        this.users = users;
        this.auth = auth;
        this.programs = programs;
        this.planDays = planDays;
        this.checkIns = checkIns;
        this.locations = locations;
        this.sessions = sessions;
        this.logging = logging;
        this.exercises = exercises;
        this.catalog = catalog;
    }

    /* one transaction, so a failure leaves no half-built account; false = already there and reset is off */
    public boolean seed(Account account, LocalDate today, boolean reset) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            var existing = users.findByEmail(UserAccount.normalizeEmail(account.email()));
            if(existing.isPresent()) {
                if(!reset) return false;
                // every user table cascades on delete
                users.delete(existing.get());
                users.flush();
            }
            long userId = auth.register(new RegisterRequest(account.email(), account.password(), account.displayName())).id();
            history(userId, today);
            return true;
        }));
    }

    private void history(long userId, LocalDate today) {
        ProgramTemplateResponse template = programs.templates().stream()
                .filter(t -> t.name().equals(PROGRAM)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Program " + PROGRAM + " is not seeded"));
        Map<String, Long> locationIds = locations.list(userId).stream()
                .collect(Collectors.toMap(LocationResponse::name, LocationResponse::id));
        Map<Long, EquipmentResponse> equipment = catalog.equipment().stream()
                .collect(Collectors.toMap(EquipmentResponse::id, Function.identity()));
        programs.choose(userId, new UserProgramRequest(template.id(), locationIds.get(HOME)));

        List<WorkoutResponse> workouts = template.workouts().stream()
                .sorted(Comparator.comparingInt(WorkoutResponse::position)).toList();
        int trainingDay = 0;
        for(int day = 0; day < DAYS; day++) {
            LocalDate date = today.minusDays(DAYS - day);
            if(!template.trainingDays().contains(date.getDayOfWeek())) {
                planDays.setDay(userId, date, new PlanDayRequest(PlanMode.REST, null, null, false), Actor.USER);
                continue;
            }
            WorkoutResponse workout = workouts.get(trainingDay % workouts.size());
            planDays.setDay(userId, date, new PlanDayRequest(PlanMode.TRAINING, workout.id(), null, false), Actor.USER);
            String gym = trainingDay == SMALL_GYM_DAY ? SMALL_GYM : HOME;
            checkIns.save(userId, date, new CheckInRequest(MORNINGS[day][0], MORNINGS[day][1], locationIds.get(gym)));
            logSession(userId, date, sessions.get(userId, date), trainingDay, equipment);
            trainingDay++;
        }
    }

    private void logSession(long userId, LocalDate date, SessionResponse session, int trainingDay, Map<Long, EquipmentResponse> equipment) {
        int last = session.exercises().getLast().position();
        for(SessionExerciseResponse item : session.exercises()) {
            if(trainingDay == SKIP_DAY && item.position() == last && !item.mainLift()) {
                sessions.setSkipped(userId, date, item.position(), true);
                continue;
            }
            ExerciseResponse exercise = exercises.get(userId, item.exerciseId());
            BigDecimal load = item.targetLoadLb() != null ? item.targetLoadLb() : startingLoad(exercise, item.mainLift(), equipment);
            for(int set = 1; set <= item.sets(); set++) {
                boolean hard = trainingDay == HARD_DAY && item.mainLift() && set == item.sets();
                logging.log(userId, date, item.position(), set(exercise, load, hard));
            }
        }
    }

    /* top of the rep range on clean sets, so the progression rule adds load next time; a hard set holds it */
    private static LoggedSetRequest set(ExerciseResponse exercise, BigDecimal load, boolean hard) {
        return switch(exercise.kind()) {
            case "STRENGTH" -> new LoggedSetRequest(hard ? exercise.repMax() - 2 : exercise.repMax(), null, load, !hard);
            case "TIMED" -> new LoggedSetRequest(null, exercise.targetSeconds(), null, true);
            default -> new LoggedSetRequest(null, exercise.durationMinutes() * 60, null, true);
        };
    }

    /* the first time an exercise shows up there is no progression load yet */
    private static BigDecimal startingLoad(ExerciseResponse exercise, boolean mainLift, Map<Long, EquipmentResponse> equipment) {
        List<EquipmentResponse> loadable = exercise.equipment().stream()
                .map(item -> equipment.get(item.id()))
                .filter(item -> item.loadIncrementLb() != null && item.loadIncrementLb().signum() > 0)
                .toList();
        if(loadable.isEmpty()) return null;
        if(loadable.stream().anyMatch(item -> item.kind().equals("MACHINE"))) return new BigDecimal("70");
        return new BigDecimal(mainLift ? "95" : "25");
    }
}
