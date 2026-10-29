package com.opslog.report;

import com.opslog.catalog.LookupResponse;
import com.opslog.logging.SetResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

@Component
public class ReportCalculator {

    public TrainingReportRow trainingRow(SessionFacts session) {
        List<SetResult> sets = session.exercises().stream()
                .flatMap(exercise -> exercise.sets().stream())
                .toList();
        int done = (int) session.exercises().stream()
                .filter(exercise -> !exercise.sets().isEmpty())
                .count();
        long clean = sets.stream().filter(SetResult::clean).count();
        Integer cleanPercent = sets.isEmpty() ? null : percent(clean, sets.size());
        BigDecimal volume = sets.stream()
                .filter(set -> set.reps() != null && set.loadLb() != null)
                .map(set -> set.loadLb().multiply(BigDecimal.valueOf(set.reps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TrainingReportRow(session.date(), session.workout(), session.tier(), session.location(),
                done, session.exercises().size(), sets.size(), cleanPercent, volume);
    }

    public List<MuscleBalanceRow> muscleBalance(List<LookupResponse> muscles, List<SessionFacts> sessions) {
        int total = tally(sessions, exercise -> true).sets();
        return muscles.stream()
                .map(muscle -> {
                    Tally tally = tally(sessions, exercise -> exercise.muscleId() == muscle.id());
                    return new MuscleBalanceRow(muscle.name(), tally.sets(), percent(tally.sets(), total),
                            tally.sessions(), tally.lastTrained());
                })
                .sorted(Comparator.comparingInt(MuscleBalanceRow::sets).reversed()
                        .thenComparing(MuscleBalanceRow::muscle))
                .toList();
    }

    public List<DirectionBalanceRow> directionBalance(List<SessionFacts> sessions) {
        int total = tally(sessions, exercise -> exercise.direction() != null).sets();
        return List.of(Direction.values()).stream()
                .map(direction -> {
                    Tally tally = tally(sessions, exercise -> exercise.direction() == direction);
                    return new DirectionBalanceRow(direction.label(), tally.sets(), percent(tally.sets(), total),
                            tally.sessions());
                })
                .toList();
    }

    public BigDecimal pushPullRatio(List<SessionFacts> sessions) {
        int push = tally(sessions, exercise -> exercise.direction() == Direction.PUSH).sets();
        int pull = tally(sessions, exercise -> exercise.direction() == Direction.PULL).sets();
        if(pull == 0) return null;
        return BigDecimal.valueOf(push).divide(BigDecimal.valueOf(pull), 2, RoundingMode.HALF_UP);
    }

    private record Tally(int sets, int sessions, LocalDate lastTrained) {}

    private Tally tally(List<SessionFacts> sessions, Predicate<ExerciseFacts> matches) {
        int sets = 0;
        TreeSet<LocalDate> days = new TreeSet<>();
        for(SessionFacts session : sessions) {
            for(ExerciseFacts exercise : session.exercises()) {
                if(matches.test(exercise) && !exercise.sets().isEmpty()) {
                    sets += exercise.sets().size();
                    days.add(session.date());
                }
            }
        }
        return new Tally(sets, days.size(), days.isEmpty() ? null : days.last());
    }

    private int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }
}
