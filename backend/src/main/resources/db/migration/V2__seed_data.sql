-- Sample data so the app is not empty on first start.
-- Shift times are relative to the moment the DB is first initialised.

INSERT INTO couriers (name, email, phone, vehicle_type, active) VALUES
    ('Alice Johnson', 'alice@example.com', '+1-555-0101', 'BIKE',    TRUE),
    ('Bob Smith',     'bob@example.com',   '+1-555-0102', 'SCOOTER', TRUE),
    ('Carla Gomez',   'carla@example.com', '+1-555-0103', 'CAR',     TRUE),
    ('David Lee',     'david@example.com', '+1-555-0104', 'BIKE',    FALSE);

INSERT INTO shifts (zone, start_time, end_time, capacity) VALUES
    ('DOWNTOWN', date_trunc('hour', now()) + interval '1 day 9 hours',  date_trunc('hour', now()) + interval '1 day 13 hours', 3),
    ('NORTH',    date_trunc('hour', now()) + interval '1 day 14 hours', date_trunc('hour', now()) + interval '1 day 18 hours', 2),
    ('SOUTH',    date_trunc('hour', now()) + interval '2 days 9 hours', date_trunc('hour', now()) + interval '2 days 17 hours', 5),
    ('EAST',     date_trunc('hour', now()) + interval '3 days 18 hours', date_trunc('hour', now()) + interval '3 days 23 hours', 1),
    ('WEST',     date_trunc('hour', now()) - interval '2 days',          date_trunc('hour', now()) - interval '2 days' + interval '4 hours', 4);

INSERT INTO bookings (courier_id, shift_id, status) VALUES
    (1, 1, 'CONFIRMED'),
    (2, 1, 'CONFIRMED'),
    (3, 4, 'CONFIRMED'),
    (1, 5, 'CONFIRMED');
