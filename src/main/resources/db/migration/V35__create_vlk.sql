-- ======================================================
-- V35__create_vlk.sql
-- Проходження ВЛК (військово-лікарська комісія)
-- ======================================================

CREATE TABLE IF NOT EXISTS vlk (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   personnel_id BIGINT NOT NULL,
                                   start_date DATE NOT NULL,
                                   end_date DATE NOT NULL,
                                   vlk_type VARCHAR(100),
    conclusion VARCHAR(255),
    note VARCHAR(500),
    FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_vlk_personnel ON vlk(personnel_id);
CREATE INDEX IF NOT EXISTS idx_vlk_dates ON vlk(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_vlk_conclusion ON vlk(conclusion);