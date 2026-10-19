USE phishingdb;

-- 1. Scan history: the 20 most recent scans
SELECT id, url, risk_score, status, scanned_at
FROM scans
ORDER BY scanned_at DESC, id DESC
LIMIT 20;

-- 2. Scans flagged as suspicious, highest risk first
SELECT id, url, risk_score, scanned_at
FROM scans
WHERE status = 'Suspicious'
ORDER BY risk_score DESC, scanned_at DESC;

-- 3. Totals by status
SELECT status,
       COUNT(*)                    AS total_scans,
       ROUND(AVG(risk_score), 1)   AS avg_risk_score
FROM scans
GROUP BY status;

-- 4. Overall summary
SELECT COUNT(*)                                          AS total_scans,
       SUM(status = 'Suspicious')                        AS suspicious_scans,
       ROUND(100 * SUM(status = 'Suspicious') / COUNT(*), 1)
                                                         AS suspicious_percent,
       ROUND(AVG(risk_score), 1)                         AS avg_risk_score,
       MAX(risk_score)                                   AS highest_risk_score
FROM scans;

-- 5. Scans per day (last 30 days)
SELECT DATE(scanned_at) AS scan_date,
       COUNT(*)         AS total_scans,
       SUM(status = 'Suspicious') AS suspicious_scans
FROM scans
WHERE scanned_at >= NOW() - INTERVAL 30 DAY
GROUP BY DATE(scanned_at)
ORDER BY scan_date DESC;

-- 6. Distribution of risk scores (each rule in the analyzer adds 20 points)
SELECT risk_score, COUNT(*) AS total_scans
FROM scans
GROUP BY risk_score
ORDER BY risk_score;

-- 7. URLs that have been scanned more than once
SELECT url, COUNT(*) AS times_scanned, MAX(risk_score) AS max_score
FROM scans
GROUP BY url
HAVING COUNT(*) > 1
ORDER BY times_scanned DESC
LIMIT 20;
