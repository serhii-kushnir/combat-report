-- ======================================================
-- V7__create_schedule_entry.sql
-- Графік чергувань
-- ======================================================

CREATE TABLE IF NOT EXISTS schedule_entry (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              personnel_id BIGINT NOT NULL,
                                              entry_date DATE NOT NULL,
                                              status VARCHAR(20),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE,
    CONSTRAINT uk_schedule_personnel_date UNIQUE (personnel_id, entry_date)
    );

CREATE INDEX IF NOT EXISTS idx_schedule_entry_personnel ON schedule_entry(personnel_id);
CREATE INDEX IF NOT EXISTS idx_schedule_entry_date ON schedule_entry(entry_date);