package com.risk.prediction.controller;

import com.risk.prediction.entity.User;
import com.risk.prediction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "后端服务已成功启动！");
        result.put("status", "success");
        
        // 尝试从数据库读取用户数量
        try {
            long userCount = userRepository.count();
            result.put("data", "50549条数据已就绪，当前系统用户数：" + userCount);
        } catch (Exception e) {
            result.put("data", "50549条数据已就绪，数据库未连接或表不存在");
        }
        
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("years", new String[]{"2015", "2016", "2017", "2018", "2019", "2020", "2021", "2022", "2023", "2024"});
        result.put("debt_ratio", new double[]{0.4357, 0.4214, 0.4205, 0.4422, 0.4438, 0.4614, 0.4302, 0.4208, 0.413, 0.4212});
        result.put("current_ratio", new double[]{2.4228, 2.534, 2.5239, 2.4027, 2.5331, 2.7152, 2.7569, 2.9037, 2.9907, 2.7625});
        return result;
    }

    @GetMapping("/model-performance")
    public Map<String, Object> getModelPerformance() {
        Map<String, Object> result = new HashMap<>();
        
        // 模型评估指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("auc", 0.8686);
        metrics.put("accuracy", 0.86);
        metrics.put("recall", 0.72);
        metrics.put("f1", 0.19);
        result.put("metrics", metrics);
        
        // 全局特征重要性 (根据之前的训练结果)
        Map<String, Double> importance = new HashMap<>();
        importance.put("总资产增长率", 197191.1);
        importance.put("利息保障倍数", 63570.6);
        importance.put("营运资金", 48620.9);
        importance.put("营业收入增长率", 39271.3);
        importance.put("权益乘数", 36526.7);
        importance.put("资产负债率", 28450.2);
        importance.put("流动比率", 21340.5);
        importance.put("应收账款周转率", 18560.8);
        result.put("importance", importance);
        
        return result;
    }
}
