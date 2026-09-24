package com.risk.prediction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiCommentaryService {

    @Value("${zhipu.api.key}")
    private String apiKey;

    @Value("${zhipu.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateCommentary(String level, double probability, Map<String, Double> shapAnalysis) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构造 Prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一位资深的金融风险审计专家。请针对以下上市公司的财务风险预测结果提供专业点评：\n");
            prompt.append("- 风险等级：").append(level).append("\n");
            prompt.append("- 风险概率：").append(String.format("%.2f%%", probability * 100)).append("\n");
            prompt.append("- 核心财务指标贡献分析 (SHAP值，正值增加风险，负值降低风险)：\n");
            
            shapAnalysis.forEach((k, v) -> {
                prompt.append("  * ").append(k).append(": ").append(String.format("%.4f", v)).append("\n");
            });

            prompt.append("\n请从以下三个维度给出简洁专业的点评（总字数控制在200字以内）：\n");
            prompt.append("1. 风险成因深度剖析\n");
            prompt.append("2. 潜在连锁反应预警\n");
            prompt.append("3. 针对性的财务治理策略");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "glm-4");
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的金融AI助手。");
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
            
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "AI 专家暂时无法提供点评，请检查网络或 API 配置。";
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 点评生成失败: " + e.getMessage();
        }
    }
}
