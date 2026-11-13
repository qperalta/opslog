/* V20: the member's own hold and rest times per exercise; null = the default (the exercise's target time, the rest rule) */
CREATE TABLE exercise_timings(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id     BIGINT       NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    hold_seconds    INTEGER,
    rest_seconds    INTEGER,
    updated_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_exercise_timings_user_exercise UNIQUE (user_id, exercise_id),
    CONSTRAINT ck_exercise_timings_hold CHECK (hold_seconds BETWEEN 1 AND 600),
    CONSTRAINT ck_exercise_timings_rest CHECK (rest_seconds BETWEEN 0 AND 900)
);

CREATE INDEX ix_exercise_timings_exercise ON exercise_timings (exercise_id);
