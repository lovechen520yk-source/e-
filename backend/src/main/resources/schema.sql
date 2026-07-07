-- 创建数据库
CREATE DATABASE IF NOT EXISTS worktime_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE worktime_db;

-- 工时记录表
CREATE TABLE IF NOT EXISTS work_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    company VARCHAR(100) NOT NULL COMMENT '公司名称',
    department VARCHAR(100) NOT NULL COMMENT '部门',
    position VARCHAR(100) NOT NULL COMMENT '岗位',
    content VARCHAR(500) NOT NULL COMMENT '工作内容',
    hours DECIMAL(3,1) NOT NULL COMMENT '工时（小时）',
    project VARCHAR(100) NOT NULL COMMENT '所属项目',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    is_rest TINYINT(1) DEFAULT 0 COMMENT '是否休息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_date (work_date),
    INDEX idx_company (company),
    INDEX idx_department (department),
    INDEX idx_project (project),
    INDEX idx_work_date_month (work_date, project, hours),
    INDEX idx_work_date_company (work_date, company),
    INDEX idx_work_date_department (work_date, department),
    INDEX idx_work_date_position (work_date, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工时记录表';

-- 用户表
CREATE TABLE IF NOT EXISTS user(
    id BIGINT PRIMARY KEY auto_increment COMMENT '主键ID',
    username VARCHAR(50) NOT NULL DEFAULT '' COMMENT '用户名',
    password VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密码',
    name VARCHAR(100) DEFAULT '' COMMENT '姓名',
    avatar MEDIUMTEXT COMMENT '头像（base64）',
    company VARCHAR(100) DEFAULT '' COMMENT '公司名称',
    department VARCHAR(100) DEFAULT '' COMMENT '部门',
    position VARCHAR(100) DEFAULT '' COMMENT '岗位',
    project VARCHAR(100) DEFAULT '' COMMENT '常用项目',
    content VARCHAR(500) DEFAULT '' COMMENT '常用工作内容',
    salary_mode VARCHAR(20) DEFAULT 'FIXED' COMMENT '薪资模式（HOURLY/FIXED）',
    hourly_rate DECIMAL(10,2) DEFAULT NULL COMMENT '小时单价',
    fixed_monthly_salary DECIMAL(10,2) DEFAULT NULL COMMENT '固定月薪',
    overtime_hourly_rate DECIMAL(10,2) DEFAULT NULL COMMENT '加班小时单价',
    allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '月度津贴',
    performance_bonus DECIMAL(10,2) DEFAULT 0.00 COMMENT '月度绩效',
    deduction DECIMAL(10,2) DEFAULT 0.00 COMMENT '月度扣除',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
