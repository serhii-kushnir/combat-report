-- ======================================================
-- V32__add_crew_personnel_to_flight_record.sql
-- Додає колонку crew_personnel до таблиці flight_record
-- ======================================================

ALTER TABLE flight_record ADD COLUMN crew_personnel VARCHAR(2000);