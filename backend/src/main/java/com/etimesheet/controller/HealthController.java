package com.etimesheet.controller;

import com.etimesheet.util.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 健康提醒控制器
 * 代理前端请求到阿里云 DashScope（通义千问）API，避免前端暴露 API Key
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Value("${dashscope.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * AI 健康分析建议
     * 根据用户工时数据调用通义千问大模型，生成个性化健康提醒
     *
     * @param requestData 包含 totalHours, dailyAvg, maxDailyHours, workDays,
     *                    overtimeDays, userName 等工时统计数据
     */
    @PostMapping("/advice")
    public ResponseResult<Map<String, String>> getHealthAdvice(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseResult.error(500, "AI 服务未配置，请联系管理员设置 dashscope.api-key");
        }

        // 提取工时统计数据
        double totalHours = toDouble(requestData.get("totalHours"));
        double dailyAvg = toDouble(requestData.get("dailyAvg"));
        double maxDailyHours = toDouble(requestData.get("maxDailyHours"));
        int workDays = toInt(requestData.get("workDays"));
        int overtimeDays = toInt(requestData.get("overtimeDays"));
        String userName = requestData.getOrDefault("userName", "用户").toString();
        String yearMonth = requestData.getOrDefault("yearMonth", "").toString();

        // 构建提示词
        String systemPrompt = """
                你是一位专业的健康顾问，同时也是用户的贴心助手。你需要根据用户的工时数据分析其工作强度，并给出个性化的健康建议。
                
                要求：
                1. 称呼用户时使用"亲爱的%s"开头
                2. 语言温暖亲切，像朋友一样关心用户
                3. 根据以下情况给出针对性建议：
                   - 如果日均工时 > 10小时，重点提醒注意身体，过度劳累的恢复方法
                   - 如果日均工时 > 9小时，提醒注意劳逸结合
                   - 如果加班天数较多（超过工作天数的一半），关注心理健康
                   - 如果最大单日工时 > 12小时，提醒避免长时间高强度工作
                4. 建议涵盖以下方面（每个方面1-2条具体可行的建议）：
                   - 饮食调理（具体的饮食建议）
                   - 运动恢复（适合上班族的具体运动）
                   - 睡眠质量（改善睡眠的具体方法）
                   - 心理调节（缓解压力的技巧）
                5. 如果工时正常（日均<8小时），给出保持良好习惯的鼓励
                6. 控制在300字以内，分段清晰
                7. 不要使用 markdown 格式，使用纯文本
                """.formatted(userName);

        String userPrompt = """
                以下是我 %s 月份的工时数据：
                - 总工时: %.1f 小时
                - 工作天数: %d 天
                - 日均工时: %.1f 小时
                - 最长单日工时: %.1f 小时
                - 加班天数（超过8小时）: %d 天
                
                请根据这些数据给我一些健康建议。
                """.formatted(yearMonth, totalHours, workDays, dailyAvg, maxDailyHours, overtimeDays);

        try {
            // 构建 DashScope API 请求
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "qwen-plus");
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            body.put("temperature", 0.8);
            body.put("max_tokens", 800);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return ResponseResult.success(Map.of("advice", content.trim()));
                }
            }

            return ResponseResult.error(500, "AI 服务返回异常");
        } catch (Exception e) {
            log.error("调用 DashScope API 失败", e);
            return ResponseResult.error(500, "AI 健康建议获取失败，请稍后再试");
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
