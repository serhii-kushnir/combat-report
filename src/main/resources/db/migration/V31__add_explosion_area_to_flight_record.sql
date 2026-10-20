-- ======================================================
-- V31__add_explosion_area_to_flight_record.sql
-- Додає колонку explosion_area до таблиці flight_record
-- ======================================================

ALTER TABLE flight_record ADD COLUMN explosion_area VARCHAR(255);