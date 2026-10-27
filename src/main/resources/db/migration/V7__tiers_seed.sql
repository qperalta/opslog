/* V7: Tiers seed */
INSERT INTO training_tiers (kind, name, description, intensity, min_sleep, max_soreness, load_factor) VALUES
    ('RECOVERY', 'Recovery', 'Half the sets at 80% of the planned load',             1, 1, 5, 0.80),
    ('REDUCED',  'Reduced',  'Main lifts as written, one fewer set of accessory work', 2, 3, 3, NULL),
    ('FULL',     'Full',     'The session as written',                               3, 4, 2, NULL);
