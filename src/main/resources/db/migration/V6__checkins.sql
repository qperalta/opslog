/* V6: Tiers and check-ins */
CREATE TABLE training_tiers(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind                VARCHAR(20)  NOT NULL,
    name                VARCHAR(40)  NOT NULL,
    description         VARCHAR(200) NOT NULL,
    intensity           INTEGER      NOT NULL,
    min_sleep           INTEGER      NOT NULL,
    max_soreness        INTEGER      NOT NULL,
    load_factor         NUMERIC(3,2),

    CONSTRAINT uq_training_tiers_kind UNIQUE (kind),
    CONSTRAINT uq_training_tiers_name UNIQUE (name),
    CONSTRAINT uq_training_tiers_intensity UNIQUE (intensity),
    CONSTRAINT ck_training_tiers_kind CHECK (kind IN ('RECOVERY', 'REDUCED', 'FULL')),
    CONSTRAINT ck_training_tiers_min_sleep CHECK (min_sleep BETWEEN 1 AND 5),
    CONSTRAINT ck_training_tiers_max_soreness CHECK (max_soreness BETWEEN 1 AND 5),
    CONSTRAINT ck_training_tiers_load_factor CHECK ((kind = 'RECOVERY') = (load_factor IS NOT NULL)),
    CONSTRAINT ck_training_tiers_load_factor_range CHECK (load_factor > 0 AND load_factor < 1)
);

CREATE TABLE check_ins(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    check_in_date       DATE        NOT NULL,
    sleep_quality       INTEGER     NOT NULL,
    soreness            INTEGER     NOT NULL,
    -- keeps history if location is deleted
    location_id         BIGINT      REFERENCES locations (id) ON DELETE SET NULL,
    tier_id             BIGINT      NOT NULL REFERENCES training_tiers (id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_check_ins_user_date UNIQUE (user_id, check_in_date),
    CONSTRAINT ck_check_ins_sleep CHECK (sleep_quality BETWEEN 1 AND 5),
    CONSTRAINT ck_check_ins_soreness CHECK (soreness BETWEEN 1 AND 5)
);
