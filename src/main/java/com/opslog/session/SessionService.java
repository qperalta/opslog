package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.checkin.CheckIn;
import com.opslog.checkin.CheckInRepository;
import com.opslog.checkin.TrainingTier;
import com.opslog.location.Location;
import com.opslog.plan.PlanDay;
import com.opslog.plan.PlanDayRepository;
import com.opslog.plan.PlanService;
import com.opslog.plan.ProgramService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional
public class SessionService {

    private static final int MAX_RATIONALE = 1000;

    private final TrainingSessionRepository sessions;
    private final SessionCatalog catalog;
    private final PlanDayRepository days;
    private final PlanService planService;
    private final ProgramService programService;
    private final CheckInRepository checkIns;
    private final SessionGenerator generator;

    SessionService(TrainingSessionRepository sessions, SessionCatalog catalog, PlanDayRepository days, PlanService planService,
                   ProgramService programService, CheckInRepository checkIns, SessionGenerator generator) {
        this.sessions = sessions;
        this.catalog = catalog;
        this.days = days;
        this.planService = planService;
        this.programService = programService;
        this.checkIns = checkIns;
        this.generator = generator;
    }

    /* the stored session, or a new one the first time */
    public SessionResponse get(long userId, LocalDate date) {
        Optional<TrainingSession> stored = sessions.findByUserIdAndDate(userId, date);
        return stored.map(SessionResponse::from).orElseGet(() -> build(userId, date));
    }

    /* after the check-in or the plan changes */
    public SessionResponse rebuild(long userId, LocalDate date) {
        sessions.findByUserIdAndDate(userId, date).ifPresent(existing -> {
            sessions.delete(existing);
            /* the unique (user, date) row must be gone before the insert */
            sessions.flush();
        });
        return build(userId, date);
    }

    private SessionResponse build(long userId, LocalDate date) {
        /* loaded first so template exercises resolve to these instances, not lazy proxies */
        List<Exercise> candidates = catalog.findVisibleTo(userId).stream().map(SessionService::unproxy).toList();

        PlanDay day = planDay(userId, date);
        if(!day.isTraining()) return SessionResponse.rest(date);

        CheckIn checkIn = checkIns.findByUserIdAndDate(userId, date).orElse(null);
        if(checkIn == null) return SessionResponse.needsCheckIn(date, day.getWorkout());

        /* where you are this morning, then the planned day, then the program default */
        Location location = Stream.of(checkIn.getLocation(), day.getLocation(), programService.find(userId).getLocation())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if(location == null) return SessionResponse.needsCheckIn(date, day.getWorkout());

        TrainingTier tier = Hibernate.unproxy(checkIn.getTier(), TrainingTier.class);
        List<PlannedExercise> planned = day.getWorkout().getExercises().stream()
                .map(item -> new PlannedExercise(unproxy(item.getExercise()), item.getSets(), item.isMainLift()))
                .toList();

        SessionDraft draft = generator.generate(new SessionInput(planned, tier, location, candidates, Map.of()));

        TrainingSession session = new TrainingSession(userId, date, day.getWorkout(), tier, location, generator.source(),
                truncate(draft.rationale()));
        for(DraftExercise item : draft.exercises()) {
            session.addExercise(item.exercise(), item.substitutedFor(), item.sets(), item.targetLoadLb(), item.mainLift(), item.reason());
        }
        return SessionResponse.from(sessions.saveAndFlush(session));
    }

    /* planning the week creates the day if the planner has not run yet */
    private PlanDay planDay(long userId, LocalDate date) {
        return days.findByUserIdAndDate(userId, date).orElseGet(() -> {
            planService.week(userId, date);
            return days.findByUserIdAndDate(userId, date).orElseThrow();
        });
    }

    private static Exercise unproxy(Exercise exercise) { return Hibernate.unproxy(exercise, Exercise.class); }

    private static String truncate(String text) {
        return text.length() <= MAX_RATIONALE ? text : text.substring(0, MAX_RATIONALE - 3) + "...";
    }
}
