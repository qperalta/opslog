package com.opslog.plan;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* one workout in a template, e.g. Upper A */
@Entity
@Table(name="template_days")
public class TemplateDay {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="template_id")
    private ProgramTemplate template;

    @Column(nullable=false)
    private int position;

    @Column(nullable=false, length=40)
    private String name;

    @OneToMany(mappedBy="day", cascade=CascadeType.ALL)
    @OrderBy("position")
    private List<TemplateExercise> exercises = new ArrayList<>();

    protected TemplateDay() {}

    TemplateDay(ProgramTemplate template, int position, String name) {
        Require.that(template != null, "Template is required");
        Require.that(position > 0, "Position must be greater than zero");
        Require.that(name != null && !name.isBlank(), "Workout name is required");
        this.template = template;
        this.position = position;
        this.name = name.trim();
    }

    public Long getId() { return id; }
    public ProgramTemplate getTemplate() { return template; }
    public int getPosition() { return position; }
    public String getName() { return name; }
    public List<TemplateExercise> getExercises() { return Collections.unmodifiableList(exercises); }

    /* by id so a lazy proxy equals the loaded workout */
    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof TemplateDay other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() { return TemplateDay.class.hashCode(); }

    public TemplateExercise addExercise(Exercise exercise, int sets, boolean mainLift) {
        TemplateExercise item = new TemplateExercise(this, exercises.size() + 1, exercise, sets, mainLift);
        exercises.add(item);
        return item;
    }
}
