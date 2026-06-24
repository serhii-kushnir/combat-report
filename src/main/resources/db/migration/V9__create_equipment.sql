-- ======================================================
-- V9__create_equipment.sql
-- Майно / склад
-- ======================================================

CREATE TABLE IF NOT EXISTS equipment (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(255),
    crew VARCHAR(255),
    location VARCHAR(255),
    category VARCHAR(255)
    );