-- Creates the database and the table used by ScanDao / HistoryServlet.
-- Run with:  mysql -u root -p < db/schema.sql

CREATE DATABASE IF NOT EXISTS phishingdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE phishingdb;

CREATE TABLE IF NOT EXISTS scans (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    url         TEXT        NOT NULL,
    risk_score  INT         NOT NULL,
    status      VARCHAR(50) NOT NULL,
    scanned_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_scans_scanned_at (scanned_at),
    INDEX idx_scans_status (status)
);
