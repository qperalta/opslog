/* V11: Sets logged against a session exercise */
CREATE TABLE logged_sets(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_exercise_id BIGINT        NOT NULL REFERENCES session_exercises (id) ON DELETE CASCADE,
    set_number          INTEGER       NOT NULL,
    -- strength sets log reps, timed and cardio sets log seconds
    reps                INTEGER,
    duration_seconds    INTEGER,
    load_lb             NUMERIC(6,2),
    clean               BOOLEAN       NOT NULL DEFAULT true,
    logged_at           timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT uq_logged_sets_number UNIQUE (session_exercise_id, set_number),
    CONSTRAINT ck_logged_sets_number CHECK (set_number BETWEEN 1 AND 10),
    CONSTRAINT ck_logged_sets_reps CHECK (reps BETWEEN 0 AND 100),
    CONSTRAINT ck_logged_sets_duration CHECK (duration_seconds BETWEEN 1 AND 7200),
    CONSTRAINT ck_logged_sets_load CHECK (load_lb >= 0),
    CONSTRAINT ck_logged_sets_measure CHECK (reps IS NOT NULL OR duration_seconds IS NOT NULL)
);
