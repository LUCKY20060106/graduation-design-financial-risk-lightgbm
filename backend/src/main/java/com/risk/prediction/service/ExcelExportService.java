package com.risk.prediction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.prediction.entity.PredictionResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExcelExportService {

    public byte[] exportPredictionResultToExcel(PredictionResult result) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("风险预测报告");

        // 设置字体和样式
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 创建标题行
        String[] headers = {"指标名称", "指标值", "SHAP贡献值", "SHAP解释"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // 填充数据
        ObjectMapper objectMapper = new ObjectMapper();
        
        Map<String, Object> shapValues = new HashMap<>();
        if (result.getShapValues() != null && !result.getShapValues().isEmpty()) {
            try {
                shapValues = objectMapper.readValue(result.getShapValues(), new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                System.err.println("解析 SHAP 值 JSON 失败: " + e.getMessage());
            }
        }

        Map<String, Object> featureValues = new HashMap<>();
        if (result.getFeatureValuesJson() != null && !result.getFeatureValuesJson().isEmpty()) {
            try {
                featureValues = objectMapper.readValue(result.getFeatureValuesJson(), new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                System.err.println("解析特征值 JSON 失败: " + e.getMessage());
            }
        }

        int rowNum = 1;
        System.out.println("开始导出 Excel，SHAP 数量: " + shapValues.size() + ", 特征值数量: " + featureValues.size());
        for (Map.Entry<String, Object> entry : shapValues.entrySet()) {
            String featureName = entry.getKey();
            Object shapValueObj = entry.getValue();
            double shapValue = 0.0;
            if (shapValueObj instanceof Number) {
                shapValue = ((Number) shapValueObj).doubleValue();
            }

            Object featureValueObj = featureValues.get(featureName);
            if (featureValueObj == null) {
                System.out.println("特征值缺失: " + featureName + ", 可用键: " + featureValues.keySet());
            }
            
            String featureValueStr = "N/A";
            if (featureValueObj instanceof Number) {
                featureValueStr = String.format("%.4f", ((Number) featureValueObj).doubleValue());
            } else if (featureValueObj != null) {
                featureValueStr = featureValueObj.toString();
            }

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(featureName);
            row.createCell(1).setCellValue(featureValueStr);
            row.createCell(2).setCellValue(shapValue);
            row.createCell(3).setCellValue(shapValue > 0 ? "风险增加" : (shapValue < 0 ? "风险降低" : "无影响"));
        }

        // 添加预测结果总结
        Row summaryRow = sheet.createRow(rowNum + 1);
        summaryRow.createCell(0).setCellValue("预测概率");
        summaryRow.createCell(1).setCellValue(result.getRiskScore().doubleValue());

        summaryRow = sheet.createRow(rowNum + 2);
        summaryRow.createCell(0).setCellValue("是否风险");
        summaryRow.createCell(1).setCellValue(result.getIsRisk() ? "是" : "否");

        summaryRow = sheet.createRow(rowNum + 3);
        summaryRow.createCell(0).setCellValue("风险等级");
        String riskLevel = result.getRiskLevel();
        if (riskLevel == null || riskLevel.isEmpty()) {
            riskLevel = "未分类";
            // 尝试从分数反推
            double score = result.getRiskScore().doubleValue();
            if (score > 0.7) riskLevel = "高风险";
            else if (score > 0.3) riskLevel = "中风险";
            else riskLevel = "低风险";
        }
        summaryRow.createCell(1).setCellValue(riskLevel);

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    public List<Map<String, Object>> parseExcelForPrediction(InputStream is) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        
        // 假设第一行是标题头
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return dataList;

        // 查找列索引
        int stkcdIdx = -1;
        int shortNameIdx = -1;
        Map<String, Integer> featureIdxMap = new HashMap<>();
        
        String[] featureNames = {
            "流动比率", "速动比率", "现金比率", "营运资金", "利息保障倍数", 
            "经营净现金流/流动负债", "现金流利息保障倍数", "资产负债率", "有形资产负债率", "权益乘数",
            "应收账款周转率", "存货周转率", "流动资产周转率", "总资产周转率",
            "总资产增长率", "净利润增长率", "营业收入增长率"
        };

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String title = headerRow.getCell(i).getStringCellValue().trim();
            if (title.contains("股票代码")) stkcdIdx = i;
            else if (title.contains("股票简称")) shortNameIdx = i;
            else {
                for (String f : featureNames) {
                    if (title.equals(f)) {
                        featureIdxMap.put(f, i);
                        break;
                    }
                }
            }
        }

        // 遍历数据行
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Object> data = new HashMap<>();
            String stkcd = stkcdIdx != -1 ? getCellValueAsString(row.getCell(stkcdIdx)) : "Unknown";
            String shortName = shortNameIdx != -1 ? getCellValueAsString(row.getCell(shortNameIdx)) : "Unknown";
            
            List<Double> features = new ArrayList<>();
            boolean validRow = true;
            for (String f : featureNames) {
                Integer idx = featureIdxMap.get(f);
                if (idx != null) {
                    Cell cell = row.getCell(idx);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        features.add(cell.getNumericCellValue());
                    } else if (cell != null && cell.getCellType() == CellType.STRING) {
                        try {
                            features.add(Double.parseDouble(cell.getStringCellValue()));
                        } catch (Exception e) {
                            features.add(0.0);
                        }
                    } else {
                        features.add(0.0);
                    }
                } else {
                    features.add(0.0); // 缺失列补0
                }
            }
            
            if (features.size() == 17) {
                data.put("stkcd", stkcd);
                data.put("short_name", shortName);
                data.put("features", features);
                dataList.add(data);
            }
        }
        workbook.close();
        return dataList;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long)cell.getNumericCellValue());
        return "";
    }
}
