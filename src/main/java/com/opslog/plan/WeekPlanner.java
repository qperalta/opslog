package com.opslog.plan;

import com.opslog.common.Require;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeekPlanner {

    // days in date order: previous = last workout before the first day, null value starts the rotation
    public void plan(List<PlanDay> days, ProgramTemplate template, TemplateDay previous) {
        Require.that(template != null, "Template is required");
        TemplateDay last = previous;
        for(PlanDay day:days) {
            if(!day.isLocked()) day.schedule(pick(day, template, last));
            if(day.isTraining()) last = day.getWorkout();
        }
    }

    TemplateDay pick(PlanDay day, ProgramTemplate template, TemplateDay last) {
        return switch(day.getMode()) {
            case REST -> null;
            case TRAINING -> template.contains(day.getWorkout())
                    ? day.getWorkout()
                    : next(template, last);
            case AUTO -> template.trainsOn(day.getDate().getDayOfWeek())
                    ? next(template, last)
                    : null;
        };
    }

    TemplateDay next(ProgramTemplate template, TemplateDay last) {
        List<TemplateDay> workouts = template.getDays();
        Require.that(!workouts.isEmpty(), "The program has no workouts");
        int index = workouts.indexOf(last);
        return workouts.get((index + 1) % workouts.size());
    }
}
