-- ======================================================
-- V30__add_pilot_to_flight_record.sql
-- Додає колонку pilot до таблиці flight_record
-- ======================================================

ALTER TABLE flight_record ADD COLUMN pilot VARCHAR(255);