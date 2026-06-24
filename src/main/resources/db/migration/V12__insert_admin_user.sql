-- ======================================================
-- V12__insert_admin_user.sql
-- Додавання адміністратора за замовчуванням
-- ======================================================

INSERT INTO app_user (username, password, role, enabled)
SELECT 'admin_skopa', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN', TRUE
    WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'admin_skopa');