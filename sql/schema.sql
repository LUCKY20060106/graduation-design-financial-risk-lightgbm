-- 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS risk_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE risk_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='系统用户表';

-- 2. 上市公司信息表
CREATE TABLE IF NOT EXISTS `company_info` (
    `stkcd` VARCHAR(10) PRIMARY KEY COMMENT '股票代码',
    `short_name` VARCHAR(50) NOT NULL COMMENT '股票简称',
    `industry` VARCHAR(50) COMMENT '所属行业',
    `is_st` TINYINT(1) DEFAULT 0 COMMENT '当前是否为ST: 0-否, 1-是',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='上市公司信息表';

-- 3. 财务指标表 (以偿债能力为例，后续可扩展)
CREATE TABLE IF NOT EXISTS `financial_indicators` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stkcd` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `accper` DATE NOT NULL COMMENT '统计截止日期',
    `f010101a` DECIMAL(18,4) COMMENT '流动比率',
    `f011201a` DECIMAL(18,4) COMMENT '资产负债率',
    `f011601a` DECIMAL(18,4) COMMENT '权益乘数',
    -- 可根据需要添加更多字段
    UNIQUE KEY `uk_stkcd_accper` (`stkcd`, `accper`)
) ENGINE=InnoDB COMMENT='财务指标数据表';

-- 4. 预测结果记录表
CREATE TABLE IF NOT EXISTS `prediction_result` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stkcd` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `predict_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间',
    `risk_score` DECIMAL(5,4) COMMENT '风险评分(0-1)',
    `is_risk` TINYINT(1) COMMENT '是否有风险: 0-低风险, 1-高风险',
    `shap_values` TEXT COMMENT 'SHAP解释值(JSON格式)',
    FOREIGN KEY (`stkcd`) REFERENCES `company_info`(`stkcd`)
) ENGINE=InnoDB COMMENT='风险预测结果表';

-- 插入默认管理员 (密码示例: admin123)
INSERT INTO `sys_user` (`username`, `password`, `role`) VALUES ('admin', 'admin123', 'ADMIN');
