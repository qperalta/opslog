/* V4: Locations and their equipment */
CREATE TABLE locations(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(60) NOT NULL,
    owner_id            BIGINT REFERENCES users (id) ON DELETE CASCADE,
    created_at          timestamptz NOT NULL DEFAULT now()
);

-- owner_id NULL = shared Kaimana gym
CREATE UNIQUE INDEX uq_locations_owner_name ON locations (owner_id, lower(name)) NULLS NOT DISTINCT;

CREATE TABLE location_equipment(
    location_id         BIGINT NOT NULL REFERENCES locations (id) ON DELETE CASCADE,
    equipment_id        BIGINT NOT NULL REFERENCES equipment (id),

    PRIMARY KEY (location_id, equipment_id)
);

CREATE INDEX ix_location_equipment_equipment ON location_equipment (equipment_id);
