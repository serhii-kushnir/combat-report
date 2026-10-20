-- ======================================================
-- V22__add_equipment_stock_fields_and_history.sql
-- Додає нові поля до equipment та таблицю історії
-- ======================================================

-- Додаємо нові поля до таблиці equipment
ALTER TABLE equipment ADD COLUMN stock_quantity INT DEFAULT 0;
ALTER TABLE equipment ADD COLUMN written_off_quantity INT DEFAULT 0;
ALTER TABLE equipment ADD COLUMN last_modified TIMESTAMP;
ALTER TABLE equipment ADD COLUMN modified_by VARCHAR(255);

-- Оновлюємо існуючі записи: кількість на складі = quantity, списано = 0
UPDATE equipment SET stock_quantity = quantity WHERE stock_quantity IS NULL;
UPDATE equipment SET written_off_quantity = 0 WHERE written_off_quantity IS NULL;
UPDATE equipment SET last_modified = CURRENT_TIMESTAMP WHERE last_modified IS NULL;
UPDATE equipment SET modified_by = 'system' WHERE modified_by IS NULL;

-- Створюємо таблицю історії змін
CREATE TABLE IF NOT EXISTS equipment_history (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 equipment_id BIGINT NOT NULL,
                                                 field_name VARCHAR(255) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_equipment_history_equipment_id ON equipment_history(equipment_id);
CREATE INDEX IF NOT EXISTS idx_equipment_history_changed_at ON equipment_history(changed_at DESC);