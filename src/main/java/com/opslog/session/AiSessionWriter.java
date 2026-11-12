package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;
import com.opslog.location.Location;
import com.opslog.plan.Actor;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/* the agent's proposal as a SessionGenerator, so it saves through the same SessionService path as the rules engine
 *  the agent suggests, this class enforces: catalog, equipment at the location, sets, and a cap on load jumps */
public class AiSessionWriter implements SessionGenerator {

    static final int MAX_EXERCISES = 12;
    static final int MAX_SETS = 10;
    static final int MAX_REASON = 200;
    static final BigDecimal MAX_JUMP = new BigDecimal("1.10");

    private final List<ProposedExercise> exercises;
    private final List<String> notes;

    public AiSessionWriter(List<ProposedExercise> exercises, List<String> notes) {
        Require.that(exercises != null && !exercises.isEmpty(), "A session needs at least one exercise");
        Require.that(exercises.size() <= MAX_EXERCISES, "A session can have at most " + MAX_EXERCISES + " exercises");
        Require.that(exercises.stream().allMatch(Objects::nonNull), "Every exercise needs an exerciseId and sets");
        this.exercises = List.copyOf(exercises);
        this.notes = notes == null
                ? List.of()
                : notes.stream().filter(Objects::nonNull).map(String::strip).filter(note -> !note.isEmpty()).toList();
    }

    @Override
    public Actor source() { return Actor.AGENT; }

    @Override
    public SessionDraft generate(SessionInput input) {
        Map<Long, Exercise> visible = input.catalog().stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        Set<Long> mainLifts = input.planned().stream()
                .filter(PlannedExercise::mainLift)
                .map(planned -> planned.exercise().getId())
                .collect(Collectors.toSet());
        Location location = input.location();
        Set<Long> used = new HashSet<>();
        List<DraftExercise> drafts = new ArrayList<>();

        for(ProposedExercise item : exercises) {
            Require.that(item.exerciseId() != null, "Every exercise needs an exerciseId from search_exercises");
            Exercise exercise = visible.get(item.exerciseId());
            Require.that(exercise != null, "Exercise " + item.exerciseId() + " is not in the catalog");
            Require.that(used.add(exercise.getId()), exercise.getName() + " is listed twice");
            Require.that(location.hasAll(exercise.getEquipment()),
                    exercise.getName() + " needs equipment that " + location.getName() + " does not have");
            Require.that(item.sets() != null && item.sets() >= 1 && item.sets() <= MAX_SETS,
                    exercise.getName() + ": sets must be 1 to " + MAX_SETS);
            BigDecimal load = checkLoad(exercise, item.targetLoadLb(), input.plannedLoads().get(exercise.getId()));
            String reason = item.reason() == null || item.reason().isBlank() ? null : item.reason().strip();
            Require.that(reason == null || reason.length() <= MAX_REASON,
                    exercise.getName() + ": reason must be " + MAX_REASON + " characters or fewer");
            drafts.add(new DraftExercise(exercise, null, item.sets(), load, mainLifts.contains(exercise.getId()), reason));
        }

        List<String> draftNotes = new ArrayList<>();
        draftNotes.add("Written by the AI assistant for the " + input.tier().getName() + " tier");
        draftNotes.addAll(notes);
        return new SessionDraft(drafts, draftNotes);
    }

    /* progressive overload guard: no more than 10% above the load the progression rule would give */
    private static BigDecimal checkLoad(Exercise exercise, BigDecimal load, BigDecimal progression) {
        if(load == null) return null;
        Require.that(load.signum() >= 0, exercise.getName() + ": load cannot be negative");
        if(progression != null) {
            Require.that(load.compareTo(progression.multiply(MAX_JUMP)) <= 0, exercise.getName() + ": " + plain(load)
                    + " lb is more than 10% above the progression load of " + plain(progression) + " lb");
        }
        return load;
    }

    private static String plain(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
