-- ======================================================
-- V24__increase_report_summary_length.sql
-- Збільшує довжину поля report_summary до 5000 символів
-- ======================================================

ALTER TABLE combat_duty ALTER COLUMN report_summary VARCHAR(5000);