-- ======================================================
-- V21__add_course_direction_to_flight_record.sql
-- Додає колонку course_direction до таблиці flight_record
-- ======================================================

ALTER TABLE flight_record ADD COLUMN course_direction INT;