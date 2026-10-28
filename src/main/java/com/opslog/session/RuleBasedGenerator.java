package com.opslog.session;

import com.opslog.catalog.Equipment;
import com.opslog.catalog.Exercise;
import com.opslog.catalog.StrengthExercise;
import com.opslog.checkin.TrainingTier;
import com.opslog.location.Location;
import com.opslog.plan.Actor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RuleBasedGenerator implements SessionGenerator {

    @Override
    public Actor source() { return Actor.RULES; }

    @Override
    public SessionDraft generate(SessionInput input) {
        TrainingTier tier = input.tier();
        Location location = input.location();
        List<DraftExercise> exercises = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        notes.add(tier.getName() + " tier: " + tier.getDescription());

        Set<Long> used = new HashSet<>();
        for(PlannedExercise planned:input.planned()) used.add(planned.exercise().getId());

        for(PlannedExercise planned:input.planned()) {
            Exercise original = planned.exercise();
            List<String> reasons = new ArrayList<>();
            Exercise chosen = original;

            if (!location.hasAll(original.getEquipment())) {
                Optional<Exercise> substitute = substitute(original, input.catalog(), location, used);
                if (substitute.isEmpty()) {
                    notes.add(original.getName() + " skipped: no substitute at " + location.getName());
                    continue;
                }
                chosen = substitute.get();
                used.add(chosen.getId());
                reasons.add("No " + missing(original, location) + " at " + location.getName());
                notes.add(original.getName() + " -> " + chosen.getName());
            }

            int sets = tier.scaleSets(planned.sets(), planned.mainLift());
            if(sets != planned.sets()) reasons.add(planned.sets() + " -> " + sets + " sets");
            BigDecimal load = tier.scaleLoad(input.plannedLoads().get(chosen.getId()));

            exercises.add(new DraftExercise(chosen, chosen == original
                    ? null
                    : original,
                    sets, load, planned.mainLift(), reasons.isEmpty()
                    ? null :
                    String.join("; ", reasons))
            );
        }
        return new SessionDraft(exercises, notes);
    }

    Optional<Exercise> substitute(Exercise original, List<Exercise> catalog, Location location, Set<Long> used) {
        return catalog.stream()
                .filter(c -> c.getClass() == original.getClass())
                .filter(c -> Objects.equals(c.getMovementPattern().getId(), original.getMovementPattern().getId()))
                .filter(c -> !used.contains(c.getId()))
                .filter(c -> location.hasAll(c.getEquipment()))
                .min(Comparator.comparing((Exercise c) -> !samePrimaryMuscle(c, original))
                        .thenComparingInt(c -> repDistance(c, original))
                        .thenComparing(Exercise::getName));
    }

    private static boolean samePrimaryMuscle(Exercise a, Exercise b) {
        return Objects.equals(a.getPrimaryMuscle().getId(), b.getPrimaryMuscle().getId());
    }

    private static int repDistance(Exercise a, Exercise b) {
        if(a instanceof StrengthExercise sa && b instanceof StrengthExercise sb)
            return Math.abs(sa.getRepMin() - sb.getRepMin());
        return 0;
    }

    private static String missing(Exercise exercise, Location location) {
        return exercise.getEquipment().stream()
                .filter(item -> !location.has(item))
                .map(Equipment::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
