package com.opslog.plan;

import com.opslog.common.Require;
import com.opslog.location.Location;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/* the user's active template and default location */
@Entity
@Table(name="user_programs")
public class UserProgram {

    @Id
    @Column(name="user_id")
    private Long userId;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="template_id")
    private ProgramTemplate template;

    /* null if the location was deleted */
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="location_id")
    private Location location;

    @Column(name="started_on", nullable=false)
    private LocalDate startedOn;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected UserProgram() {}

    public UserProgram(Long userId) {
        Require.that(userId != null, "User is required");
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public ProgramTemplate getTemplate() { return template; }
    public Location getLocation() { return location; }
    public LocalDate getStartedOn() { return startedOn; }
    public Instant getUpdatedAt() { return updatedAt; }

    /* switching templates restarts the program */
    public void choose(ProgramTemplate template, Location location, LocalDate today) {
        Require.that(template != null, "Template is required");
        Require.that(today != null, "Start date is required");
        if(this.template == null || !Objects.equals(this.template.getId(), template.getId())) this.startedOn = today;
        this.template = template;
        this.location = location;
        this.updatedAt = Instant.now();
    }
}
