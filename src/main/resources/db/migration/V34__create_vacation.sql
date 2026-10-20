-- ======================================================
-- V34__create_vacation.sql
-- Таблиця відпусток
-- ======================================================

CREATE TABLE IF NOT EXISTS vacation (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        personnel_id BIGINT NOT NULL,
                                        start_date DATE NOT NULL,
                                        end_date DATE NOT NULL,
                                        vacation_type VARCHAR(100),
    note VARCHAR(500),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_vacation_personnel ON vacation(personnel_id);
CREATE INDEX IF NOT EXISTS idx_vacation_dates ON vacation(start_date, end_date);