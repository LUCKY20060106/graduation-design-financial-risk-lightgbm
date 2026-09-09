package com.risk.prediction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.prediction.entity.PredictionResult;
import com.risk.prediction.repository.PredictionResultRepository;
import com.risk.prediction.service.ExcelExportService;
import com.risk.prediction.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictionResultRepository predictionResultRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private com.risk.prediction.repository.SystemConfigRepository configRepository;

    @Autowired
    private com.risk.prediction.repository.CompanyInfoRepository companyInfoRepository;

    @Autowired
    private com.risk.prediction.repository.IndustryAverageRepository industryAverageRepository;

    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, Object> request) {
        List<Double> features = (List<Double>) request.get("features");
        String stkcd = (String) request.getOrDefault("stkcd", "000001");

        if (features == null || features.isEmpty()) {
            return Map.of("error", "请输入有效的财务指标数据");
        }

        // 获取动态阈值
        double thresholdHigh = 0.7;
        double thresholdMedium = 0.3;
        try {
            thresholdHigh = Double.parseDouble(configRepository.findById("threshold_high")
                    .map(c -> c.getConfigValue()).orElse("0.7"));
            thresholdMedium = Double.parseDouble(configRepository.findById("threshold_medium")
                    .map(c -> c.getConfigValue()).orElse("0.3"));
        } catch (Exception e) {
            System.err.println("读取配置失败，使用默认阈值: " + e.getMessage());
        }

        Map<String, Object> result = predictionService.predictRisk(features, thresholdHigh, thresholdMedium);

        // 注入行业对标数据
        try {
            System.out.println("查询行业对标数据，股票代码: " + stkcd);
            companyInfoRepository.findById(stkcd).ifPresentOrElse(company -> {
                String industry = company.getIndustry();
                System.out.println("公司行业: [" + industry + "]");
                industryAverageRepository.findByIndustryName(industry).ifPresentOrElse(avg -> {
                    System.out.println("找到行业均值数据: " + avg.getIndustryName());
                    result.put("industry_benchmarking", avg);
                }, () -> System.err.println("未找到行业均值数据: [" + industry + "]"));
            }, () -> System.err.println("未找到公司信息: " + stkcd));
        } catch (Exception e) {
            System.err.println("获取行业对标数据失败: " + e.getMessage());
        }

        // 如果预测成功，保存到数据库
        if (!result.containsKey("error")) {
            try {
                System.out.println("开始保存预测结果，Python 返回结果: " + result);
                PredictionResult record = new PredictionResult();
                record.setStkcd(stkcd);
                record.setRiskScore(new BigDecimal(result.get("risk_probability").toString()));
                record.setIsRisk((Boolean) result.get("is_risk"));
                record.setRiskLevel((String) result.get("level")); // 保存风险等级
                
                // 保存 SHAP 解释数据
                if (result.containsKey("shap_analysis")) {
                    String shapJson = objectMapper.writeValueAsString(result.get("shap_analysis"));
                    record.setShapValues(shapJson);
                }
                if (result.containsKey("feature_values")) {
                    String featureValuesJson = objectMapper.writeValueAsString(result.get("feature_values"));
                    record.setFeatureValuesJson(featureValuesJson);
                    System.out.println("成功保存特征值 JSON: " + featureValuesJson);
                } else {
                    System.err.println("警告: Python 返回结果中缺少 feature_values");
                }
                
                PredictionResult saved = predictionResultRepository.save(record);
                System.out.println("数据库保存成功，ID: " + saved.getId() + ", 风险等级: " + saved.getRiskLevel());
            } catch (Exception e) {
                System.err.println("保存预测结果失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return result;
    }

    @GetMapping("/history")
    public List<PredictionResult> getHistory() {
        return predictionResultRepository.findAllByOrderByPredictDateDesc();
    }

    @GetMapping("/prediction/benchmarking/{stkcd}")
    public ResponseEntity<com.risk.prediction.entity.IndustryAverage> getBenchmarking(@PathVariable String stkcd) {
        return companyInfoRepository.findById(stkcd)
                .flatMap(company -> industryAverageRepository.findByIndustryName(company.getIndustry()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prediction/export/{id}")
    public ResponseEntity<byte[]> exportPrediction(@PathVariable Long id) {
        Optional<PredictionResult> resultOptional = predictionResultRepository.findById(id);
        if (resultOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            PredictionResult result = resultOptional.get();
            byte[] excelBytes = excelExportService.exportPredictionResultToExcel(result);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "prediction_report_" + id + ".xlsx");
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/predict/batch")
    public Map<String, Object> predictBatch(@RequestParam("file") MultipartFile file) {
        try {
            List<Map<String, Object>> dataList = excelExportService.parseExcelForPrediction(file.getInputStream());
            List<Map<String, Object>> results = new ArrayList<>();
            
            // 获取动态阈值
            double thresholdHigh = 0.7;
            double thresholdMedium = 0.3;
            try {
                thresholdHigh = Double.parseDouble(configRepository.findById("threshold_high").map(c -> c.getConfigValue()).orElse("0.7"));
                thresholdMedium = Double.parseDouble(configRepository.findById("threshold_medium").map(c -> c.getConfigValue()).orElse("0.3"));
            } catch (Exception e) {}

            for (Map<String, Object> data : dataList) {
                String stkcd = (String) data.getOrDefault("stkcd", "Unknown");
                List<Double> features = (List<Double>) data.get("features");
                
                if (features != null && features.size() == 17) {
                    Map<String, Object> result = predictionService.predictRisk(features, thresholdHigh, thresholdMedium);
                    result.put("stkcd", stkcd);
                    result.put("short_name", data.get("short_name"));
                    results.add(result);
                    
                    // 异步保存记录 (此处简化为同步保存)
                    if (!result.containsKey("error")) {
                        PredictionResult record = new PredictionResult();
                        record.setStkcd(stkcd);
                        record.setRiskScore(new BigDecimal(result.get("risk_probability").toString()));
                        record.setIsRisk((Boolean) result.get("is_risk"));
                        record.setRiskLevel((String) result.get("level"));
                        if (result.containsKey("shap_analysis")) {
                            record.setShapValues(objectMapper.writeValueAsString(result.get("shap_analysis")));
                        }
                        if (result.containsKey("feature_values")) {
                            record.setFeatureValuesJson(objectMapper.writeValueAsString(result.get("feature_values")));
                        }
                        predictionResultRepository.save(record);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", dataList.size());
            response.put("processed", results.size());
            response.put("results", results);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "批量处理失败: " + e.getMessage());
        }
    }
}
