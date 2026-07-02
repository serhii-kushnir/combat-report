-- Додаємо нові колонки до таблиці combat_duty
ALTER TABLE combat_duty ADD COLUMN unit_name VARCHAR(255) DEFAULT 'СКОПА';
ALTER TABLE combat_duty ADD COLUMN commander VARCHAR(255);
ALTER TABLE combat_duty ADD COLUMN pilot VARCHAR(255);
ALTER TABLE combat_duty ADD COLUMN navigator VARCHAR(255);
ALTER TABLE combat_duty ADD COLUMN technician VARCHAR(255);

-- Колонку crew_members можна залишити або видалити (залишаємо для сумісності)