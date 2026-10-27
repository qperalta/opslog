package com.opslog.plan;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;
import jakarta.persistence.*;

@Entity
@Table(name="template_exercises")
public class TemplateExercise {

    private static final int MAX_SETS = 10;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="template_day_id")
    private TemplateDay day;

    @Column(nullable=false)
    private int position;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="exercise_id")
    private Exercise exercise;

    @Column(nullable=false)
    private int sets;

    /* main lifts keep their sets on Reduced days */
    @Column(name="main_lift", nullable=false)
    private boolean mainLift;

    protected TemplateExercise() {}

    TemplateExercise(TemplateDay day, int position, Exercise exercise, int sets, boolean mainLift) {
        Require.that(day != null, "Workout is required");
        Require.that(position > 0, "Position must be greater than zero");
        Require.that(exercise != null, "Exercise is required");
        Require.that(sets >= 1 && sets <= MAX_SETS, "Sets must be between 1 and " + MAX_SETS);
        this.day = day;
        this.position = position;
        this.exercise = exercise;
        this.sets = sets;
        this.mainLift = mainLift;
    }

    public Long getId() { return id; }
    public TemplateDay getDay() { return day; }
    public int getPosition() { return position; }
    public Exercise getExercise() { return exercise; }
    public int getSets() { return sets; }
    public boolean isMainLift() { return mainLift; }
}
