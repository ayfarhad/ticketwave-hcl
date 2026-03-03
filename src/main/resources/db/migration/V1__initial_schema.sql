-- Users and security
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Passengers
CREATE TABLE passengers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    dob DATE,
    passport_number VARCHAR(20) UNIQUE
);

-- Routes and schedules
CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    origin VARCHAR(50) NOT NULL,
    destination VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    UNIQUE(origin,destination,type)
);

CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id),
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    base_price NUMERIC(10,2) NOT NULL
);

-- Seats
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL REFERENCES schedules(id),
    seat_number VARCHAR(10) NOT NULL,
    class VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    UNIQUE(schedule_id,seat_number)
);

-- Bookings
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    pnr VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    seat_id BIGINT NOT NULL REFERENCES seats(id),
    price NUMERIC(10,2) NOT NULL
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Refunds
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    amount NUMERIC(10,2) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Indexes
CREATE INDEX idx_schedule_route ON schedules(route_id);
CREATE INDEX idx_seat_schedule ON seats(schedule_id);
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_payment_booking ON payments(booking_id);
