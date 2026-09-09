package com.risk.prediction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final String metricsPath = "d:\\桌面\\计算机科学与技术毕业论文\\metrics.json";
    private final String comparisonPath = "d:\\桌面\\计算机科学与技术毕业论文\\comparison_metrics.json";

    @GetMapping("/metrics")
    public Object getMetrics() {
        File file = new File(metricsPath);
        if (!file.exists()) {
            return Map.of("error", "评估数据尚未生成，请联系管理员运行评估脚本。");
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(file, Object.class);
        } catch (IOException e) {
            return Map.of("error", "读取评估数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/comparison")
    public Object getComparison() {
        File file = new File(comparisonPath);
        if (!file.exists()) {
            return Map.of("error", "模型对比数据尚未生成。");
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(file, Object.class);
        } catch (IOException e) {
            return Map.of("error", "读取对比数据失败: " + e.getMessage());
        }
    }
}
