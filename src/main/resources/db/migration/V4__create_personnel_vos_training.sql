-- ======================================================
-- V4__create_personnel_vos_training.sql
-- ВОС навчання
-- ======================================================

CREATE TABLE IF NOT EXISTS personnel_vos_training (
                                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                      personnel_id BIGINT NOT NULL,
                                                      name VARCHAR(255),
    speciality VARCHAR(255),
    vos_number VARCHAR(255),
    start_date DATE,
    end_date DATE,
    order_number VARCHAR(255),
    order_date DATE,
    military_unit VARCHAR(255),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_personnel_vos_training_personnel ON personnel_vos_training(personnel_id);