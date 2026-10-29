/* V12: Push or pull for the Training Balance Report; other patterns stay null */
ALTER TABLE movement_patterns ADD COLUMN direction VARCHAR(10);
ALTER TABLE movement_patterns ADD CONSTRAINT ck_movement_patterns_direction CHECK (direction IN ('PUSH', 'PULL'));

UPDATE movement_patterns mp
SET direction = v.direction
FROM (VALUES
    ('Horizontal Push', 'PUSH'),
    ('Vertical Push',   'PUSH'),
    ('Horizontal Pull', 'PULL'),
    ('Vertical Pull',   'PULL')
) AS v (pattern, direction)
WHERE mp.name = v.pattern;

-- a typo in a name above updates nothing, so fail the migration instead
DO $$
BEGIN
    IF (SELECT count(*) FROM movement_patterns WHERE direction IS NOT NULL) <> 4 THEN
        RAISE EXCEPTION 'Expected 4 push/pull movement patterns';
    END IF;
END $$;
