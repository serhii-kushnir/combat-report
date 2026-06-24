-- ======================================================
-- V5__create_personnel_weapon.sql
-- Зброя персоналу
-- ======================================================

CREATE TABLE IF NOT EXISTS personnel_weapon (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                personnel_id BIGINT NOT NULL,
                                                weapon_type VARCHAR(255),
    serial_number VARCHAR(255),
    issued_date VARCHAR(255),
    note VARCHAR(255),
    bayonet VARCHAR(255),
    magazines VARCHAR(255),
    caliber VARCHAR(255),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_personnel_weapon_personnel ON personnel_weapon(personnel_id);