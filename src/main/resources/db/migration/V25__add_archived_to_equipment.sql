
ALTER TABLE equipment ADD COLUMN archived BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_equipment_archived ON equipment(archived);