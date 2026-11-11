/* V13: An exercise the user skipped today; it takes no sets until re-activated */
ALTER TABLE session_exercises ADD COLUMN skipped BOOLEAN NOT NULL DEFAULT false;
