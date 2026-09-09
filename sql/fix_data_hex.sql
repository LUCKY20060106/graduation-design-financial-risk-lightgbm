SET NAMES utf8mb4;
USE risk_db;

-- 清空并重新插入行业均值，使用十六进制避免编码问题
TRUNCATE TABLE industry_average;
INSERT INTO industry_average (industry_name, avg_solvency, avg_operation, avg_growth, avg_profit, avg_cash_flow) VALUES 
(X'E98791E89E8DE4B89A', 0.85, 0.70, 0.65, 0.90, 0.80), -- 金融业
(X'E588B6E980A0E4B89A', 0.60, 0.85, 0.75, 0.55, 0.65), -- 制造业
(X'E688BFE59CB0E4BAA7E4B89A', 0.40, 0.50, 0.80, 0.70, 0.45), -- 房地产业
(X'E4BFA1E681AFE68A80E69CAFE4B89A', 0.75, 0.80, 0.90, 0.85, 0.70), -- 信息技术业
(X'E689B9E58F91E5928CE99BB6E594AEE4B89A', 0.65, 0.95, 0.60, 0.50, 0.75); -- 批发和零售业

-- 修复 000001 的基本信息作为测试基准
DELETE FROM company_info WHERE stkcd = '000001';
INSERT INTO company_info (stkcd, short_name, industry, is_st) VALUES 
('000001', X'E5B9B3E5AE89E993B6E8A18C', X'E98791E89E8DE4B89A', 0);
