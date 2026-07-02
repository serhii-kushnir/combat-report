ALTER TABLE combat_duty ADD COLUMN total_sorties INT DEFAULT 0;
ALTER TABLE combat_duty ADD COLUMN combat_sorties INT DEFAULT 0;
ALTER TABLE combat_duty ADD COLUMN losses INT DEFAULT 0;
ALTER TABLE combat_duty ADD COLUMN destructions INT DEFAULT 0;
ALTER TABLE combat_duty ADD COLUMN ntp INT DEFAULT 0;