-- ======================================================
-- V10__create_notes.sql
-- Нотатки
-- ======================================================

CREATE TABLE IF NOT EXISTS notes (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL,
    content VARCHAR(50000),
    color VARCHAR(255),
    archived BOOLEAN DEFAULT FALSE,
    pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_notes_archived ON notes(archived);
CREATE INDEX IF NOT EXISTS idx_notes_pinned ON notes(pinned);
CREATE INDEX IF NOT EXISTS idx_notes_updated ON notes(updated_at DESC);