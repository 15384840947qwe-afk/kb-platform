package com.lyq.kb.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI能力统一出口：OpenAI兼容协议，配置指向谁就用谁
 * （云端DeepSeek/硅基流动/通义，或本机Ollama的/v1端点，同一套代码）。
 * 未配置或超时一律返回null，调用方各自降级——AI是插件不是依赖
 */
@Slf4j
@Component
public class AiGrader {

    @Value("${drill.ai.base-url:}")
    private String baseUrl;
    @Value("${drill.ai.api-key:}")
    private String apiKey;
    @Value("${drill.ai.model:deepseek-ai/DeepSeek-V3}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 通用对话：返回模型原文；不可用/超时返回null。默认20秒超时 */
    public String chat(String system, String user) {
        return chat(system, user, 20);
    }

    /** 通用对话：timeoutSeconds按场景给，生题这种长输出要放宽 */
    public String chat(String system, String user, int timeoutSeconds) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "temperature", 0,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    )
            );
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("AI请求HTTP {}", res.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(res.body());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("AI不可用：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 流式对话：SSE逐段回调增量，让前端打字机式上屏。
     * 返回false=连接就没建立成功（未配置/HTTP错/中途断）；
     * 注意中途断流时可能已回调过部分增量，调用方按"有没有收到内容"决定是否降级
     */
    public boolean chatStream(String system, String user, int timeoutSeconds, Consumer<String> onDelta) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "temperature", 0,
                    "stream", true,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    )
            );
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                log.warn("AI流式请求HTTP {}", res.statusCode());
                return false;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // OpenAI兼容SSE：每行形如 data: {json}，末尾 data: [DONE]
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        continue;
                    }
                    try {
                        String delta = objectMapper.readTree(data)
                                .path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            onDelta.accept(delta);
                        }
                    } catch (Exception ignored) {
                        // 单行解析失败跳过，不断流
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("AI流式不可用：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 批改一道简答。返回 {pass:bool, comment:String}；不可用返回null
     */
    public Map<String, Object> grade(String stem, String reference, String userAnswer) {
        String content = chat(
                "你是技术面试考官。对比参考答案与考生答案，考生答出核心要点即算对，不要求措辞一致。" +
                "点评要先肯定答对的部分再指出缺失，像老师不像判官。只输出JSON：{\"pass\":true或false,\"comment\":一句话点评}",
                "题目：" + stem + "\n参考答案：" + reference + "\n考生答案：" + userAnswer);
        if (content == null) {
            return null;
        }
        try {
            // 模型可能夹带解释文字或```json围栏：截取第一对花括号之间的JSON片段
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            JsonNode grade = objectMapper.readTree(content.substring(start, end + 1));
            return Map.of(
                    "pass", grade.path("pass").asBoolean(false),
                    "comment", grade.path("comment").asText("")
            );
        } catch (Exception e) {
            return null;
        }
    }
}
