/* V18: exercises the member inserts during a workout; inserting shifts later positions, so uniqueness is checked at commit */
ALTER TABLE session_exercises ADD COLUMN ad_hoc BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE session_exercises DROP CONSTRAINT uq_session_exercises_position;
ALTER TABLE session_exercises ADD CONSTRAINT uq_session_exercises_position
    UNIQUE (session_id, position) DEFERRABLE INITIALLY DEFERRED;
