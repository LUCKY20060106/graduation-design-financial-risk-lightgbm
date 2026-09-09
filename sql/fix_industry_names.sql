SET NAMES utf8mb4;
USE risk_db;

TRUNCATE TABLE industry_average;

INSERT INTO industry_average (industry_name, avg_solvency, avg_operation, avg_growth, avg_profit, avg_cash_flow) VALUES 
('金融业', 0.85, 0.70, 0.65, 0.90, 0.80),
('制造业', 0.60, 0.85, 0.75, 0.55, 0.65),
('房地产业', 0.40, 0.50, 0.80, 0.70, 0.45),
('信息技术业', 0.75, 0.80, 0.90, 0.85, 0.70),
('批发和零售业', 0.65, 0.95, 0.60, 0.50, 0.75);
