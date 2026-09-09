package com.risk.prediction.controller;

import com.risk.prediction.entity.SystemConfig;
import com.risk.prediction.repository.PredictionResultRepository;
import com.risk.prediction.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private PredictionResultRepository predictionResultRepository;

    @GetMapping
    public Map<String, String> getSettings() {
        List<SystemConfig> configs = configRepository.findAll();
        // 如果没有初始配置，返回默认值
        Map<String, String> result = new HashMap<>();
        result.put("threshold_high", "0.7");
        result.put("threshold_medium", "0.3");
        
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @PostMapping
    public Map<String, String> updateSettings(@RequestBody Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemConfig config = configRepository.findById(entry.getKey())
                    .orElse(new SystemConfig(entry.getKey(), entry.getValue(), "System threshold setting"));
            config.setConfigValue(entry.getValue());
            configRepository.save(config);
        }
        return getSettings();
    }

    @DeleteMapping("/history/clear")
    public Map<String, String> clearHistory() {
        predictionResultRepository.deleteAll();
        return Map.of("message", "历史预测记录已清空");
    }
}
