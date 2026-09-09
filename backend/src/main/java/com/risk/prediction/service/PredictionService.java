package com.risk.prediction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    private final String pythonPath = "d:\\桌面\\计算机科学与技术毕业论文\\venv\\Scripts\\python.exe";
    private final String scriptPath = "d:\\桌面\\计算机科学与技术毕业论文\\src\\predict.py";

    public Map<String, Object> predictRisk(List<Double> features, double thresholdHigh, double thresholdMedium) {
        try {
            // 将特征列表和阈值包装成一个对象
            Map<String, Object> payload = Map.of(
                "features", features,
                "threshold_high", thresholdHigh,
                "threshold_medium", thresholdMedium
            );
            
            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);

            // 构建命令行指令
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.redirectErrorStream(true);

            // 执行命令
            Process process = pb.start();

            // 向 Python 脚本写入输入数据 (STDIN)
            try (java.io.OutputStream os = process.getOutputStream();
                 java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(os, "UTF-8"))) {
                writer.write(payloadJson);
                writer.flush();
            }

            // 读取 Python 脚本的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String lastLine = "";
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line); // 打印到控制台方便调试
                if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
                    lastLine = line;
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && !lastLine.isEmpty()) {
                // 解析 Python 返回的 JSON 结果
                return mapper.readValue(lastLine, Map.class);
            } else {
                return Map.of("error", "Python 脚本执行失败或未返回有效结果", "exitCode", exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "系统内部错误: " + e.getMessage());
        }
    }
}
