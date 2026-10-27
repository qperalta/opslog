/* V8: Program templates and the week planner */
CREATE TABLE program_templates(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(60)  NOT NULL,
    description         VARCHAR(200) NOT NULL,

    CONSTRAINT uq_program_templates_name UNIQUE (name)
);

-- ISO day of week, 1 = Monday
CREATE TABLE template_weekdays(
    template_id         BIGINT   NOT NULL REFERENCES program_templates (id) ON DELETE CASCADE,
    day_of_week         INTEGER  NOT NULL,

    PRIMARY KEY (template_id, day_of_week),
    CONSTRAINT ck_template_weekdays_day CHECK (day_of_week BETWEEN 1 AND 7)
);

-- workouts in rotation order
CREATE TABLE template_days(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id         BIGINT      NOT NULL REFERENCES program_templates (id) ON DELETE CASCADE,
    position            INTEGER     NOT NULL,
    name                VARCHAR(40) NOT NULL,

    CONSTRAINT uq_template_days_position UNIQUE (template_id, position),
    CONSTRAINT uq_template_days_name UNIQUE (template_id, name),
    CONSTRAINT ck_template_days_position CHECK (position > 0)
);

CREATE TABLE template_exercises(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_day_id     BIGINT  NOT NULL REFERENCES template_days (id) ON DELETE CASCADE,
    position            INTEGER NOT NULL,
    exercise_id         BIGINT  NOT NULL REFERENCES exercises (id),
    sets                INTEGER NOT NULL,
    main_lift           BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT uq_template_exercises_position UNIQUE (template_day_id, position),
    CONSTRAINT ck_template_exercises_position CHECK (position > 0),
    CONSTRAINT ck_template_exercises_sets CHECK (sets BETWEEN 1 AND 10)
);

CREATE INDEX ix_template_exercises_exercise ON template_exercises (exercise_id);

-- one active program per user
CREATE TABLE user_programs(
    user_id             BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    template_id         BIGINT      NOT NULL REFERENCES program_templates (id),
    location_id         BIGINT      REFERENCES locations (id) ON DELETE SET NULL,
    started_on          DATE        NOT NULL,
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE plan_days(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_date           DATE        NOT NULL,
    mode                VARCHAR(10) NOT NULL,
    template_day_id     BIGINT      REFERENCES template_days (id),
    location_id         BIGINT      REFERENCES locations (id) ON DELETE SET NULL,
    locked              BOOLEAN     NOT NULL DEFAULT false,
    -- who changed it last: the user, the rules engine, or the AI agent
    updated_by          VARCHAR(10) NOT NULL,
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_plan_days_user_date UNIQUE (user_id, plan_date),
    CONSTRAINT ck_plan_days_mode CHECK (mode IN ('AUTO', 'TRAINING', 'REST')),
    CONSTRAINT ck_plan_days_rest CHECK (mode <> 'REST' OR template_day_id IS NULL),
    CONSTRAINT ck_plan_days_updated_by CHECK (updated_by IN ('USER', 'RULES', 'AGENT'))
);

CREATE INDEX ix_plan_days_template_day ON plan_days (template_day_id);
