package com.opslog.plan;

import com.opslog.catalog.LookupResponse;

import java.time.DayOfWeek;
import java.util.List;

public record ProgramTemplateResponse(Long id, String name, String description, List<DayOfWeek> trainingDays,
                                      List<WorkoutResponse> workouts) {

    public record WorkoutResponse(Long id, int position, String name, List<ItemResponse> exercises) {}

    public record ItemResponse(Long id, int position, LookupResponse exercise, LookupResponse primaryMuscle,
                               String target, int sets, boolean mainLift) {}

    static ProgramTemplateResponse from(ProgramTemplate template) {
        List<WorkoutResponse> workouts = template.getDays().stream().map(ProgramTemplateResponse::workout).toList();
        return new ProgramTemplateResponse(template.getId(), template.getName(), template.getDescription(),
                template.getTrainingDays(), workouts);
    }

    private static WorkoutResponse workout(TemplateDay day) {
        List<ItemResponse> items = day.getExercises().stream().map(ProgramTemplateResponse::item).toList();
        return new WorkoutResponse(day.getId(), day.getPosition(), day.getName(), items);
    }

    private static ItemResponse item(TemplateExercise item) {
        var exercise = item.getExercise();
        var muscle = exercise.getPrimaryMuscle();
        return new ItemResponse(item.getId(), item.getPosition(),
                new LookupResponse(exercise.getId(), exercise.getName()),
                new LookupResponse(muscle.getId(), muscle.getName()),
                exercise.describeTarget(), item.getSets(), item.isMainLift());
    }
}
