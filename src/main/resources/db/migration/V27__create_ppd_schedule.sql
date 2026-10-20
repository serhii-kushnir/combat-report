-- ======================================================
-- V27__create_ppd_schedule.sql
-- Графік ППД (окрема таблиця)
-- ======================================================

CREATE TABLE IF NOT EXISTS ppd_schedule (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            personnel_id BIGINT NOT NULL,
                                            entry_date DATE NOT NULL,
                                            status VARCHAR(20),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE,
    CONSTRAINT uk_ppd_schedule_personnel_date UNIQUE (personnel_id, entry_date)
    );

CREATE INDEX IF NOT EXISTS idx_ppd_schedule_personnel ON ppd_schedule(personnel_id);
CREATE INDEX IF NOT EXISTS idx_ppd_schedule_date ON ppd_schedule(entry_date);