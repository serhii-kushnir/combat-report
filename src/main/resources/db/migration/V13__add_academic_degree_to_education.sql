-- ======================================================
-- V13__add_academic_degree_to_education.sql
-- Додає колонку academic_degree до таблиці освіти
-- ======================================================

ALTER TABLE personnel_education ADD COLUMN academic_degree VARCHAR(255);