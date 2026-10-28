/* V10: Generated training sessions */
CREATE TABLE training_sessions(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_date        DATE         NOT NULL,
    -- null if the agent wrote a session outside the template
    template_day_id     BIGINT       REFERENCES template_days (id),
    tier_id             BIGINT       NOT NULL REFERENCES training_tiers (id),
    location_id         BIGINT       REFERENCES locations (id) ON DELETE SET NULL,
    -- who wrote it: the rules engine or the AI agent
    source              VARCHAR(10)  NOT NULL,
    rationale           VARCHAR(1000),
    created_at          timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_training_sessions_user_date UNIQUE (user_id, session_date),
    CONSTRAINT ck_training_sessions_source CHECK (source IN ('USER', 'RULES', 'AGENT'))
);

CREATE INDEX ix_training_sessions_template_day ON training_sessions (template_day_id);

CREATE TABLE session_exercises(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id          BIGINT        NOT NULL REFERENCES training_sessions (id) ON DELETE CASCADE,
    position            INTEGER       NOT NULL,
    exercise_id         BIGINT        NOT NULL REFERENCES exercises (id),
    -- the template exercise it replaced, null if unchanged
    substituted_for_id  BIGINT        REFERENCES exercises (id),
    sets                INTEGER       NOT NULL,
    target_load_lb      NUMERIC(6,2),
    main_lift           BOOLEAN       NOT NULL DEFAULT false,
    reason              VARCHAR(200),

    CONSTRAINT uq_session_exercises_position UNIQUE (session_id, position),
    CONSTRAINT ck_session_exercises_position CHECK (position > 0),
    CONSTRAINT ck_session_exercises_sets CHECK (sets BETWEEN 1 AND 10),
    CONSTRAINT ck_session_exercises_load CHECK (target_load_lb >= 0)
);

CREATE INDEX ix_session_exercises_exercise ON session_exercises (exercise_id);
CREATE INDEX ix_session_exercises_substituted_for ON session_exercises (substituted_for_id);
