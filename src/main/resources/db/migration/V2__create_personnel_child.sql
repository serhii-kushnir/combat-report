-- ======================================================
-- V2__create_personnel_child.sql
-- Діти персоналу
-- ======================================================

CREATE TABLE IF NOT EXISTS personnel_child (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               personnel_id BIGINT NOT NULL,
                                               full_name VARCHAR(255),
    birth_date DATE,
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_personnel_child_personnel ON personnel_child(personnel_id);