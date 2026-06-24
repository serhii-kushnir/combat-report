-- ======================================================
-- V11__create_app_user.sql
-- Користувачі (безпека)
-- ======================================================

CREATE TABLE IF NOT EXISTS app_user (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE
    );