/* V9: Kaimana program templates seed */
INSERT INTO program_templates (name, description) VALUES
    ('Upper/Lower 4-Day', 'Four days a week, alternating upper and lower body. Two heavy and two volume days.'),
    ('Full Body 3-Day', 'Three full-body days a week with a rest day between each. Good for busy schedules.');

INSERT INTO template_weekdays (template_id, day_of_week)
SELECT t.id, v.day_of_week
FROM (VALUES
    ('Upper/Lower 4-Day', 1), ('Upper/Lower 4-Day', 2), ('Upper/Lower 4-Day', 4), ('Upper/Lower 4-Day', 5),
    ('Full Body 3-Day', 1), ('Full Body 3-Day', 3), ('Full Body 3-Day', 5)
) AS v (template, day_of_week)
LEFT JOIN program_templates t ON t.name = v.template;

INSERT INTO template_days (template_id, position, name)
SELECT t.id, v.position, v.name
FROM (VALUES
    ('Upper/Lower 4-Day', 1, 'Upper A'), ('Upper/Lower 4-Day', 2, 'Lower A'),
    ('Upper/Lower 4-Day', 3, 'Upper B'), ('Upper/Lower 4-Day', 4, 'Lower B'),
    ('Full Body 3-Day', 1, 'Full Body A'), ('Full Body 3-Day', 2, 'Full Body B'),
    ('Full Body 3-Day', 3, 'Full Body C')
) AS v (template, position, name)
LEFT JOIN program_templates t ON t.name = v.template;

-- main lifts keep their sets on Reduced days
INSERT INTO template_exercises (template_day_id, position, exercise_id, sets, main_lift)
SELECT d.id, v.position, e.id, v.sets, v.main_lift
FROM (VALUES
    ('Upper/Lower 4-Day', 'Upper A', 1, 'Barbell Bench Press',            4, true),
    ('Upper/Lower 4-Day', 'Upper A', 2, 'Barbell Row',                    4, true),
    ('Upper/Lower 4-Day', 'Upper A', 3, 'Seated Dumbbell Shoulder Press', 3, false),
    ('Upper/Lower 4-Day', 'Upper A', 4, 'Lat Pulldown',                   3, false),
    ('Upper/Lower 4-Day', 'Upper A', 5, 'EZ Bar Curl',                    2, false),
    ('Upper/Lower 4-Day', 'Upper A', 6, 'Cable Triceps Pushdown',         2, false),
    ('Upper/Lower 4-Day', 'Lower A', 1, 'Back Squat',                     4, true),
    ('Upper/Lower 4-Day', 'Lower A', 2, 'Romanian Deadlift',              3, true),
    ('Upper/Lower 4-Day', 'Lower A', 3, 'Walking Lunge',                  3, false),
    ('Upper/Lower 4-Day', 'Lower A', 4, 'Leg Curl',                       3, false),
    ('Upper/Lower 4-Day', 'Lower A', 5, 'Standing Calf Raise',            3, false),
    ('Upper/Lower 4-Day', 'Lower A', 6, 'Plank',                          3, false),
    ('Upper/Lower 4-Day', 'Upper B', 1, 'Overhead Press',                 4, true),
    ('Upper/Lower 4-Day', 'Upper B', 2, 'Pull-Up',                        4, true),
    ('Upper/Lower 4-Day', 'Upper B', 3, 'Incline Dumbbell Press',         3, false),
    ('Upper/Lower 4-Day', 'Upper B', 4, 'Seated Cable Row',               3, false),
    ('Upper/Lower 4-Day', 'Upper B', 5, 'Dumbbell Lateral Raise',         3, false),
    ('Upper/Lower 4-Day', 'Upper B', 6, 'Dumbbell Hammer Curl',           2, false),
    ('Upper/Lower 4-Day', 'Lower B', 1, 'Conventional Deadlift',          3, true),
    ('Upper/Lower 4-Day', 'Lower B', 2, 'Front Squat',                    3, true),
    ('Upper/Lower 4-Day', 'Lower B', 3, 'Bulgarian Split Squat',          3, false),
    ('Upper/Lower 4-Day', 'Lower B', 4, 'Hip Thrust',                     3, false),
    ('Upper/Lower 4-Day', 'Lower B', 5, 'Seated Calf Raise',              3, false),
    ('Upper/Lower 4-Day', 'Lower B', 6, 'Hanging Knee Raise',             3, false),
    ('Full Body 3-Day',   'Full Body A', 1, 'Back Squat',                 3, true),
    ('Full Body 3-Day',   'Full Body A', 2, 'Barbell Bench Press',        3, true),
    ('Full Body 3-Day',   'Full Body A', 3, 'Barbell Row',                3, false),
    ('Full Body 3-Day',   'Full Body A', 4, 'Dumbbell Romanian Deadlift', 3, false),
    ('Full Body 3-Day',   'Full Body A', 5, 'Plank',                      2, false),
    ('Full Body 3-Day',   'Full Body B', 1, 'Trap Bar Deadlift',          3, true),
    ('Full Body 3-Day',   'Full Body B', 2, 'Overhead Press',             3, true),
    ('Full Body 3-Day',   'Full Body B', 3, 'Lat Pulldown',               3, false),
    ('Full Body 3-Day',   'Full Body B', 4, 'Reverse Lunge',              3, false),
    ('Full Body 3-Day',   'Full Body B', 5, 'Face Pull',                  2, false),
    ('Full Body 3-Day',   'Full Body C', 1, 'Front Squat',                3, true),
    ('Full Body 3-Day',   'Full Body C', 2, 'Incline Dumbbell Press',     3, true),
    ('Full Body 3-Day',   'Full Body C', 3, 'One-Arm Dumbbell Row',       3, false),
    ('Full Body 3-Day',   'Full Body C', 4, 'Hip Thrust',                 3, false),
    ('Full Body 3-Day',   'Full Body C', 5, 'Farmer''s Carry',            2, false),
    ('Full Body 3-Day',   'Full Body C', 6, 'Assault Bike Intervals',     1, false)
) AS v (template, day, position, exercise, sets, main_lift)
LEFT JOIN program_templates t ON t.name = v.template
LEFT JOIN template_days d ON d.template_id = t.id AND d.name = v.day
LEFT JOIN exercises e ON e.name = v.exercise AND e.owner_id IS NULL;
