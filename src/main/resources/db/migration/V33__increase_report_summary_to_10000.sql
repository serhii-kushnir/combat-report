-- ======================================================
-- V33__increase_report_summary_to_10000.sql
-- Збільшує довжину поля report_summary до 10000 символів
-- ======================================================

ALTER TABLE combat_duty ALTER COLUMN report_summary VARCHAR(10000);