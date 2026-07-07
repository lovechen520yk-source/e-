-- ============================================
-- 月度总结功能 - 建表 + 模拟数据
-- 在 IDEA 中右键此文件 → Run 即可执行
-- ============================================

-- 1. 建表（如果不存在）
CREATE TABLE IF NOT EXISTS monthly_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    year INT NOT NULL COMMENT '年份',
    total_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总工时',
    total_salary DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总薪资',
    record_count INT NOT NULL DEFAULT 0 COMMENT '记录条数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_month (user_id, `year_month`),
    INDEX idx_user_year (user_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度薪资总结';

-- 2. 插入模拟数据（2026年6月）
-- 假设用户ID为1，薪资模式为 HOURLY（30元/小时）
INSERT INTO monthly_summary (user_id, `year_month`, year, total_hours, total_salary, record_count)
SELECT * FROM (
    SELECT
        1 AS user_id,
        '2026-06' AS `year_month`,
        2026 AS year,
        168.00 AS total_hours,
        5040.00 AS total_salary,
        22 AS record_count
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_summary WHERE user_id = 1 AND `year_month` = '2026-06'
);

-- 3. （可选）再插入几条历史数据，让年份 Tab 切换效果更明显
INSERT INTO monthly_summary (user_id, `year_month`, year, total_hours, total_salary, record_count)
SELECT * FROM (
    SELECT 1, '2026-05', 2026, 176.00, 5280.00, 23 FROM dual
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_summary WHERE user_id = 1 AND `year_month` = '2026-05'
);

INSERT INTO monthly_summary (user_id, `year_month`, year, total_hours, total_salary, record_count)
SELECT * FROM (
    SELECT 1, '2026-04', 2026, 160.00, 4800.00, 20 FROM dual
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_summary WHERE user_id = 1 AND `year_month` = '2026-04'
);

INSERT INTO monthly_summary (user_id, `year_month`, year, total_hours, total_salary, record_count)
SELECT * FROM (
    SELECT 1, '2026-03', 2026, 172.00, 5160.00, 21 FROM dual
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_summary WHERE user_id = 1 AND `year_month` = '2026-03'
);

-- 4. 验证数据
SELECT * FROM monthly_summary WHERE user_id = 1 ORDER BY `year_month` DESC;
