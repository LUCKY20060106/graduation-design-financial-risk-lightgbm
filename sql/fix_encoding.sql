-- 修复数据库和表的字符集为 utf8mb4
ALTER DATABASE risk_db CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 修复 company_info 表及其字段注释
ALTER TABLE company_info CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE company_info MODIFY COLUMN stkcd VARCHAR(10) COMMENT '股票代码';
ALTER TABLE company_info MODIFY COLUMN short_name VARCHAR(50) NOT NULL COMMENT '股票简称';
ALTER TABLE company_info MODIFY COLUMN industry VARCHAR(50) COMMENT '所属行业';
ALTER TABLE company_info MODIFY COLUMN is_st TINYINT(1) DEFAULT 0 COMMENT '当前是否为ST: 0-否, 1-是';
ALTER TABLE company_info MODIFY COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 修复 prediction_result 表及其字段注释
ALTER TABLE prediction_result CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE prediction_result MODIFY COLUMN stkcd VARCHAR(10) NOT NULL COMMENT '股票代码';
ALTER TABLE prediction_result MODIFY COLUMN predict_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间';
ALTER TABLE prediction_result MODIFY COLUMN is_risk TINYINT(1) COMMENT '是否有风险: 0-低风险, 1-高风险';
ALTER TABLE prediction_result MODIFY COLUMN shap_values TEXT COMMENT 'SHAP解释值(JSON格式)';
