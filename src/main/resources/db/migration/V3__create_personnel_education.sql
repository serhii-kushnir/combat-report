-- ======================================================
-- V3__create_personnel_education.sql
-- Освіта персоналу
-- ======================================================

CREATE TABLE IF NOT EXISTS personnel_education (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   personnel_id BIGINT NOT NULL,
                                                   level VARCHAR(255),
    institution VARCHAR(255),
    speciality VARCHAR(255),
    start_date DATE,
    end_date DATE,
    diploma VARCHAR(255),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_personnel_education_personnel ON personnel_education(personnel_id);