package com.opslog.session;

import com.opslog.catalog.CustomExerciseService;
import com.opslog.catalog.Exercise;
import com.opslog.catalog.ExerciseNotFoundException;
import com.opslog.checkin.CheckIn;
import com.opslog.checkin.CheckInRepository;
import com.opslog.checkin.TrainingTier;
import com.opslog.common.Require;
import com.opslog.location.Location;
import com.opslog.logging.ProgressionService;
import com.opslog.logging.SessionExerciseNotFoundException;
import com.opslog.plan.Actor;
import com.opslog.plan.LockedDayException;
import com.opslog.plan.PlanDay;
import com.opslog.plan.PlanDayRepository;
import com.opslog.plan.PlanService;
import com.opslog.plan.ProgramService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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
    private final ProgressionService progression;
    private final CustomExerciseService customExercises;

    SessionService(TrainingSessionRepository sessions, SessionCatalog catalog, PlanDayRepository days, PlanService planService,
                   ProgramService programService, CheckInRepository checkIns, SessionGenerator generator,
                   ProgressionService progression, CustomExerciseService customExercises) {
        this.sessions = sessions;
        this.catalog = catalog;
        this.days = days;
        this.planService = planService;
        this.programService = programService;
        this.checkIns = checkIns;
        this.generator = generator;
        this.progression = progression;
        this.customExercises = customExercises;
    }

    /* the stored session, or a new one the first time */
    public SessionResponse get(long userId, LocalDate date) {
        Optional<TrainingSession> stored = sessions.findByUserIdAndDate(userId, date);
        return stored.map(this::respond).orElseGet(() -> build(userId, date, generator));
    }

    /* after the check-in or the plan changes; refused once sets are logged */
    public SessionResponse rebuild(long userId, LocalDate date) {
        removeUnlogged(userId, date);
        return build(userId, date, generator);
    }

    /* the agent's session (AiSessionWriter) for a checked-in training day; locked days are read-only to the agent */
    public SessionResponse write(long userId, LocalDate date, SessionGenerator writer) {
        PlanDay day = planDay(userId, date);
        if(writer.source() == Actor.AGENT && day.isLocked()) throw new LockedDayException(date);
        Require.that(day.isTraining(), date + " is a rest day in the plan");
        removeUnlogged(userId, date);
        SessionResponse written = build(userId, date, writer);
        // rolls back the removal too
        Require.that(written.status() == SessionStatus.READY, "Check in for " + date + " first: the check-in picks the tier");
        return written;
    }

    /* skip or re-activate one exercise; only the user's own stored session */
    public SessionExerciseResponse setSkipped(long userId, LocalDate date, int position, boolean skipped) {
        SessionExercise item = sessions.findByUserIdAndDate(userId, date)
                .flatMap(session -> session.findExercise(position))
                .orElseThrow(() -> new SessionExerciseNotFoundException(date, position));
        if(skipped) item.skip();
        else item.reactivate();
        return SessionExerciseResponse.from(item);
    }

    /* the member inserts an exercise mid-workout; a new name becomes their own custom exercise */
    public SessionResponse insertExercise(long userId, LocalDate date, InsertExerciseRequest request) {
        TrainingSession session = sessions.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException("There is no session for " + date + " yet"));
        Require.that((request.exerciseId() == null) != (request.newExercise() == null),
                "Pick an exercise from the list or name a new one, not both");
        Exercise exercise = request.exerciseId() != null
                ? catalog.findVisible(request.exerciseId(), userId).orElseThrow(() -> new ExerciseNotFoundException(request.exerciseId()))
                : findOrCreate(userId, request);
        session.insertExercise(request.position(), unproxy(exercise), request.sets(), request.targetLoadLb());
        return respond(sessions.saveAndFlush(session));
    }

    /* a name already in the list (any case) is reused instead of duplicated */
    private Exercise findOrCreate(long userId, InsertExerciseRequest request) {
        return catalog.findVisibleByName(request.newExercise().name(), userId).orElseGet(() -> {
            long id = customExercises.create(userId, request.newExercise()).id();
            return catalog.findVisible(id, userId).orElseThrow();
        });
    }

    private void removeUnlogged(long userId, LocalDate date) {
        sessions.findByUserIdAndDate(userId, date).ifPresent(existing -> {
            if(existing.hasLoggedSets()) throw new SessionHasLogsException(date);
            sessions.delete(existing);
            /* the unique (user, date) row must be gone before the insert */
            sessions.flush();
        });
    }

    private SessionResponse build(long userId, LocalDate date, SessionGenerator generator) {
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

        SessionDraft draft = generator.generate(new SessionInput(planned, tier, location, candidates,
                progression.plannedLoads(userId, date)));

        TrainingSession session = new TrainingSession(userId, date, day.getWorkout(), tier, location, generator.source(),
                truncate(draft.rationale()));
        for(DraftExercise item : draft.exercises()) {
            session.addExercise(item.exercise(), item.substitutedFor(), item.sets(), item.targetLoadLb(), item.mainLift(), item.reason());
        }
        return respond(sessions.saveAndFlush(session));
    }

    private SessionResponse respond(TrainingSession session) {
        return SessionResponse.from(session, progression.lastSets(session.getUserId(), session.getDate()));
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
