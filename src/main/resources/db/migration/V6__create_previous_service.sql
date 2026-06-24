-- ======================================================
-- V6__create_previous_service.sql
-- Попередня військова служба
-- ======================================================

CREATE TABLE IF NOT EXISTS previous_service (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                personnel_id BIGINT NOT NULL,
                                                service_type VARCHAR(100),
    drafted_by VARCHAR(200),
    start_date DATE,
    end_date DATE,
    rank VARCHAR(50),
    military_unit VARCHAR(100),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_previous_service_personnel ON previous_service(personnel_id);