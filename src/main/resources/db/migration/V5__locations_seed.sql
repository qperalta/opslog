/* V5: Kaimana gyms seed */
INSERT INTO locations (name) VALUES
    ('Kaimana Downtown'), ('Kaimana Kailua'), ('Kaimana Kapolei');

-- Downtown has everything
INSERT INTO location_equipment (location_id, equipment_id)
SELECT l.id, eq.id
FROM (VALUES ('Kaimana Downtown')) AS v (location)
LEFT JOIN locations l ON l.name = v.location AND l.owner_id IS NULL
CROSS JOIN equipment eq;

INSERT INTO location_equipment (location_id, equipment_id)
SELECT l.id, eq.id
FROM (VALUES
    -- Kailua: no leg/chest machines
    ('Kaimana Kailua', 'Barbell'), ('Kaimana Kailua', 'Dumbbell'), ('Kaimana Kailua', 'Kettlebell'),
    ('Kaimana Kailua', 'EZ Curl Bar'), ('Kaimana Kailua', 'Trap Bar'),
    ('Kaimana Kailua', 'Cable Stack'), ('Kaimana Kailua', 'Lat Pulldown Machine'),
    ('Kaimana Kailua', 'Pull-Up Bar'), ('Kaimana Kailua', 'Dip Station'),
    ('Kaimana Kailua', 'Flat Bench'), ('Kaimana Kailua', 'Adjustable Bench'), ('Kaimana Kailua', 'Squat Rack'),
    ('Kaimana Kailua', 'Landmine'), ('Kaimana Kailua', 'Roman Chair'), ('Kaimana Kailua', 'Plyo Box'),
    ('Kaimana Kailua', 'Resistance Band'), ('Kaimana Kailua', 'Ab Wheel'), ('Kaimana Kailua', 'Jump Rope'),
    ('Kaimana Kailua', 'Assault Bike'), ('Kaimana Kailua', 'Rowing Machine'),
    -- Kapolei: no barbell or machines
    ('Kaimana Kapolei', 'Dumbbell'), ('Kaimana Kapolei', 'Kettlebell'),
    ('Kaimana Kapolei', 'Pull-Up Bar'), ('Kaimana Kapolei', 'Adjustable Bench'),
    ('Kaimana Kapolei', 'Plyo Box'), ('Kaimana Kapolei', 'Resistance Band'),
    ('Kaimana Kapolei', 'Ab Wheel'), ('Kaimana Kapolei', 'Jump Rope'), ('Kaimana Kapolei', 'Assault Bike')
) AS v (location, equipment)
LEFT JOIN locations l ON l.name = v.location AND l.owner_id IS NULL
LEFT JOIN equipment eq ON eq.name = v.equipment;
