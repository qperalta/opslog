package com.opslog.plan;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.util.*;

/* shared Kaimana program; workouts run in rotation */
@Entity
@Table(name="program_templates")
public class ProgramTemplate {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=60)
    private String name;

    @Column(nullable=false, length=200)
    private String description;

    /* ISO day of week, 1 = Monday */
    @ElementCollection
    @CollectionTable(name="template_weekdays", joinColumns=@JoinColumn(name="template_id"))
    @Column(name="day_of_week")
    private Set<Integer> weekdays = new HashSet<>();

    @OneToMany(mappedBy="template", cascade=CascadeType.ALL)
    @OrderBy("position")
    private List<TemplateDay> days = new ArrayList<>();

    protected ProgramTemplate() {}

    public ProgramTemplate(String name, String description, Collection<DayOfWeek> trainingDays) {
        Require.that(name != null && !name.isBlank(), "Template name is required");
        Require.that(description != null && !description.isBlank(), "Template description is required");
        Require.that(trainingDays != null && !trainingDays.isEmpty(), "A template needs at least one training day");
        this.name = name.trim();
        this.description = description.trim();
        trainingDays.forEach(day -> weekdays.add(day.getValue()));
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<TemplateDay> getDays() { return Collections.unmodifiableList(days); }

    public List<DayOfWeek> getTrainingDays() { return weekdays.stream().sorted().map(DayOfWeek::of).toList(); }

    public boolean trainsOn(DayOfWeek day) { return weekdays.contains(day.getValue()); }

    public boolean contains(TemplateDay day) { return day != null && days.contains(day); }

    public TemplateDay addDay(String name) {
        TemplateDay day = new TemplateDay(this, days.size() + 1, name);
        days.add(day);
        return day;
    }
}
