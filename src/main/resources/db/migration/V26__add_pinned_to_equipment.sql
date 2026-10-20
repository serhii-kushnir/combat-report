-- ======================================================
-- V26__add_pinned_to_equipment.sql
-- Додає колонку pinned до таблиці equipment
-- ======================================================

ALTER TABLE equipment ADD COLUMN pinned BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_equipment_pinned ON equipment(pinned);