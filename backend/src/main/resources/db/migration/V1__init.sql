CREATE TABLE couriers (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    phone        VARCHAR(30)  NOT NULL,
    vehicle_type VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE shifts (
    id          BIGSERIAL PRIMARY KEY,
    zone        VARCHAR(30) NOT NULL,
    start_time  TIMESTAMPTZ NOT NULL,
    end_time    TIMESTAMPTZ NOT NULL,
    capacity    INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_shift_time CHECK (end_time > start_time),
    CONSTRAINT chk_shift_capacity CHECK (capacity > 0)
);

CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    courier_id   BIGINT      NOT NULL REFERENCES couriers (id),
    shift_id     BIGINT      NOT NULL REFERENCES shifts (id),
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ
);

CREATE INDEX idx_shifts_start ON shifts (start_time);
CREATE INDEX idx_bookings_courier ON bookings (courier_id);
CREATE INDEX idx_bookings_shift ON bookings (shift_id);
