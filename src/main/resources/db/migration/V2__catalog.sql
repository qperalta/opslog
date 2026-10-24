/* V2: Exercise catalog - lookup tables, equipment and exercise hierarchies */
CREATE TABLE muscle_groups(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(40) NOT NULL,

    CONSTRAINT uq_muscle_groups_name UNIQUE (name)
);

CREATE TABLE movement_patterns(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(40) NOT NULL,

    CONSTRAINT uq_movement_patterns_name UNIQUE (name)
);

CREATE TABLE equipment(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind                VARCHAR(20)     NOT NULL,
    name                VARCHAR(80)     NOT NULL,
    load_increment_lb   NUMERIC(5,2),
    max_load_lb         NUMERIC(6,2),

    CONSTRAINT uq_equipment_name UNIQUE (name),
    CONSTRAINT ck_equipment_kind CHECK (kind IN ('FREE_WEIGHT', 'MACHINE', 'BODYWEIGHT', 'ACCESSORY')),
    CONSTRAINT ck_equipment_increment CHECK ((kind IN ('FREE_WEIGHT', 'MACHINE')) = (load_increment_lb IS NOT NULL)),
    CONSTRAINT ck_equipment_increment_positive CHECK (load_increment_lb > 0),
    CONSTRAINT ck_equipment_max_load CHECK (max_load_lb IS NULL OR (kind = 'MACHINE' AND max_load_lb > 0))
);

CREATE TABLE exercises(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind                VARCHAR(20) NOT NULL,
    name                VARCHAR(80) NOT NULL,
    movement_pattern_id BIGINT      NOT NULL REFERENCES movement_patterns (id),
    primary_muscle_id   BIGINT      NOT NULL REFERENCES muscle_groups (id),
    owner_id            BIGINT REFERENCES users (id) ON DELETE CASCADE,
    rep_min             INTEGER,
    rep_max             INTEGER,
    target_seconds      INTEGER,
    duration_minutes    INTEGER,
    created_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ck_exercises_kind CHECK (kind IN ('STRENGTH', 'TIMED', 'CARDIO')),
    CONSTRAINT ck_exercises_strength CHECK ((kind = 'STRENGTH') = (rep_min IS NOT NULL AND rep_max IS NOT NULL)),
    CONSTRAINT ck_exercises_rep_range CHECK (rep_min > 0 AND rep_max >= rep_min),
    CONSTRAINT ck_exercises_timed CHECK ((kind = 'TIMED') = (target_seconds IS NOT NULL)),
    CONSTRAINT ck_exercises_target_seconds CHECK (target_seconds > 0),
    CONSTRAINT ck_exercises_cardio CHECK ((kind = 'CARDIO') = (duration_minutes IS NOT NULL)),
    CONSTRAINT ck_exercises_duration CHECK (duration_minutes > 0)
);

CREATE UNIQUE INDEX  uq_exercises_owner_name ON exercises(owner_id, lower(name)) NULLS NOT DISTINCT;
CREATE INDEX ix_exercises_movement_pattern ON exercises (movement_pattern_id);
CREATE INDEX ix_exercises_primary_muscle ON exercises (primary_muscle_id);

CREATE TABLE exercise_equipment(
    exercise_id         BIGINT  NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    equipment_id        BIGINT  NOT NULL    REFERENCES equipment(id),

    PRIMARY KEY (exercise_id, equipment_id)
);

CREATE INDEX ix_exercise_equipment_equipment ON exercise_equipment (equipment_id);