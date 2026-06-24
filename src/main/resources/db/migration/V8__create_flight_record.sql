-- ======================================================
-- V8__create_flight_record.sql
-- Журнал БпАК
-- ======================================================

CREATE TABLE IF NOT EXISTS flight_record (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             record_number INT,
                                             flight_date DATE,
                                             crew VARCHAR(255),
    event VARCHAR(255),
    takeoff_time TIME,
    loss_time TIME,
    coordinates VARCHAR(255),
    azimuth INT,
    distance INT,
    flight_altitude INT,
    loss_reason VARCHAR(255),
    target_type VARCHAR(255),
    identification VARCHAR(255),
    weapon VARCHAR(255),
    explosive VARCHAR(255),
    detonator VARCHAR(255),
    altitude VARCHAR(100),
    target_altitude INT,
    target VARCHAR(255),
    target_speed INT,
    note VARCHAR(2000),
    flight_month VARCHAR(30)
    );

CREATE INDEX IF NOT EXISTS idx_flight_record_date ON flight_record(flight_date);
CREATE INDEX IF NOT EXISTS idx_flight_record_month ON flight_record(flight_month);