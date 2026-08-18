package com.lyq.kb.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import java.util.ArrayList;
import java.util.HashMap;
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
    /** 备用模型：主模型超时/报错自动切过来，云端模型宕机时功能不断摆 */
    @Value("${drill.ai.fallback-model:deepseek-ai/DeepSeek-V3}")
    private String fallbackModel;
    /** RAG向量化模型；留空或调用失败时检索自动降级为纯关键词 */
    @Value("${drill.ai.embedding-model:}")
    private String embeddingModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 启动时打印一次AI配置自检：key是否注入成功一眼可见（只回显前6位不泄露全文） */
    @PostConstruct
    public void logConfig() {
        String keyState = apiKey == null || apiKey.isBlank()
                ? "未配置！请设置AI_API_KEY环境变量后重启"
                : "已配置(" + apiKey.substring(0, Math.min(6, apiKey.length())) + "…)";
        log.info("AI配置自检：baseUrl={}, model={}, fallback={}, embedding={}, apiKey={}",
                baseUrl, model, fallbackModel, embeddingModel, keyState);
    }

    /** 通用对话：返回模型原文；不可用/超时返回null。默认20秒超时 */
    public String chat(String system, String user) {
        return chat(system, user, 20);
    }

    /** 通用对话：timeoutSeconds按场景给，生题这种长输出要放宽；主模型失败自动切备用模型再来一次 */
    public String chat(String system, String user, int timeoutSeconds) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI_API_KEY未配置，跳过AI调用（设置环境变量后重启即可）");
            return null;
        }
        String r = chatOnce(model, system, user, timeoutSeconds);
        if (r == null && fallbackModel != null && !fallbackModel.isBlank()
                && !fallbackModel.equals(model)) {
            log.warn("主模型{}不可用，自动切备用模型{}重试", model, fallbackModel);
            r = chatOnce(fallbackModel, system, user, timeoutSeconds);
        }
        return r;
    }

    /** 单次对话（指定模型）：返回模型原文；不可用/超时返回null */
    private String chatOnce(String useModel, String system, String user, int timeoutSeconds) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", useModel,
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
                log.warn("AI请求HTTP {}（模型{}）", res.statusCode(), useModel);
                return null;
            }
            JsonNode root = objectMapper.readTree(res.body());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("AI不可用（模型{}）：{}", useModel, e.getMessage());
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
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI_API_KEY未配置，跳过AI调用（设置环境变量后重启即可）");
            return false;
        }
        // 包一层记录有没有吐出过内容：一个字都没出才允许换备用模型重来，
        // 已经吐了一半再重试前端会出现重复内容
        boolean[] emitted = {false};
        Consumer<String> wrapped = d -> {
            emitted[0] = true;
            onDelta.accept(d);
        };
        boolean ok = streamOnce(model, system, user, timeoutSeconds, wrapped);
        if (!ok && !emitted[0] && fallbackModel != null && !fallbackModel.isBlank()
                && !fallbackModel.equals(model)) {
            log.warn("主模型{}流式不可用，自动切备用模型{}重试", model, fallbackModel);
            ok = streamOnce(fallbackModel, system, user, timeoutSeconds, wrapped);
        }
        return ok;
    }

    /** 单次流式对话（指定模型） */
    private boolean streamOnce(String useModel, String system, String user, int timeoutSeconds,
                               Consumer<String> onDelta) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", useModel,
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
                log.warn("AI流式请求HTTP {}（模型{}）", res.statusCode(), useModel);
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
     * 批量向量化（OpenAI兼容/embeddings）：返回与texts等长的向量列表；
     * 未配置embedding模型/接口不支持/超时一律返回null，调用方降级为关键词检索
     */
    public List<float[]> embed(List<String> texts, int timeoutSeconds) {
        if (baseUrl == null || baseUrl.isBlank() || embeddingModel == null || embeddingModel.isBlank()
                || texts == null || texts.isEmpty()) {
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = Map.of("model", embeddingModel, "input", texts);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("embedding请求HTTP {}", res.statusCode());
                return null;
            }
            // data里每项带index，按index归位防乱序
            JsonNode data = objectMapper.readTree(res.body()).path("data");
            float[][] out = new float[texts.size()][];
            for (JsonNode item : data) {
                int idx = item.path("index").asInt(-1);
                if (idx < 0 || idx >= texts.size()) {
                    continue;
                }
                JsonNode emb = item.path("embedding");
                float[] vec = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) {
                    vec[i] = (float) emb.get(i).asDouble();
                }
                out[idx] = vec;
            }
            for (float[] v : out) {
                if (v == null) {
                    return null;
                }
            }
            return List.of(out);
        } catch (Exception e) {
            log.warn("embedding不可用：{}", e.getMessage());
            return null;
        }
    }

    /** 单条向量化，失败返回null */
    public float[] embedOne(String text) {
        List<float[]> r = embed(List.of(text), 15);
        return r == null ? null : r.get(0);
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
            JsonNode grade = readJson(content);
            if (grade == null) {
                return null;
            }
            return Map.of(
                    "pass", grade.path("pass").asBoolean(false),
                    "comment", grade.path("comment").asText("")
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析岗位JD为结构化需求。返回原始JSON节点（skills/minExpYears/education/keywords），
     * 不可用/解析失败返回null，调用方自己降级
     */
    public JsonNode parseJobRequirement(String title, String jdText) {
        // JD可能很长，只取前2000字，既省token也避免超模型上下文
        String jd = jdText == null ? "" : jdText.trim();
        if (jd.length() > 2000) {
            jd = jd.substring(0, 2000);
        }
        String content = chat(
                "你是招聘需求分析师。从岗位JD中提炼结构化要求，不确定就给空数组或0。" +
                "只输出JSON：{\"skills\":[技能数组],\"minExpYears\":最低年限数字,\"education\":\"学历要求\",\"keywords\":[面试考察关键词数组]}",
                "岗位：" + title + "\nJD：" + jd, 30);
        return readJson(content);
    }

    /**
     * 按岗位需求推荐面试简答题。返回题目列表；不可用/解析失败返回null
     */
    public List<String> recommendQuestions(String title, String requireJson) {
        return recommendQuestions(title, requireJson, null);
    }

    /**
     * 带接地材料的出题：material是从知识库/题库检索来的参考资料，
     * 为null时退回纯生成；有材料时要求AI贴着站内知识出题且不与已有题重复
     */
    public List<String> recommendQuestions(String title, String requireJson, String material) {
        boolean grounded = material != null && !material.isBlank();
        String system = "你是技术面试官。根据岗位需求出" + RECOMMEND_COUNT + "道最能区分水平的面试简答题，" +
                "贴近真实面试口语，不要重复不要出偏题。" +
                (grounded ? "下面附了站内知识库资料和已有题库题目：出题要贴合资料里的知识点，不要和已有题目撞题。" : "") +
                "只输出JSON：{\"questions\":[题目1,题目2,...]}";
        String user = "岗位：" + title + "\n结构化需求：" + requireJson +
                (grounded ? "\n\n站内参考资料：\n" + material : "");
        // 出题是长输出，接地时prompt更大模型更慢，超时给宽点免得白等
        String content = chat(system, user, grounded ? 90 : 60);
        JsonNode node = readJson(content);
        if (node == null || !node.path("questions").isArray()) {
            return null;
        }
        List<String> questions = new ArrayList<>();
        for (JsonNode q : node.path("questions")) {
            String text = q.asText("").trim();
            if (!text.isEmpty()) {
                questions.add(text);
            }
        }
        return questions.isEmpty() ? null : questions;
    }

    /** 推荐出题固定道数 */
    public static final int RECOMMEND_COUNT = 5;

    /**
     * 带参考答案的出题（管理员出题入题库用）：每题多带答案要点，
     * 刷题时AI评分有参照；解析宽容——模型只回了字符串数组也能用，答案置空
     */
    public List<Map<String, String>> recommendQuestionsRich(String title, String requireJson, String material) {
        boolean grounded = material != null && !material.isBlank();
        String system = "你是技术面试官。根据岗位需求出" + RECOMMEND_COUNT + "道最能区分水平的面试简答题，" +
                "贴近真实面试口语，不要重复不要出偏题。" +
                (grounded ? "下面附了站内知识库资料和已有题库题目：出题要贴合资料里的知识点，不要和已有题目撞题。" : "") +
                "只输出JSON：{\"questions\":[{\"stem\":\"题目\",\"answer\":\"参考答案要点，2到4句\"}]}";
        String user = "岗位：" + title + "\n结构化需求：" + requireJson +
                (grounded ? "\n\n站内参考资料：\n" + material : "");
        String content = chat(system, user, grounded ? 90 : 60);
        JsonNode node = readJson(content);
        if (node == null || !node.path("questions").isArray()) {
            return null;
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode q : node.path("questions")) {
            String stem;
            String answer = "";
            if (q.isObject()) {
                stem = q.path("stem").asText("").trim();
                answer = q.path("answer").asText("").trim();
            } else {
                stem = q.asText("").trim();
            }
            if (!stem.isEmpty()) {
                Map<String, String> item = new HashMap<>();
                item.put("stem", stem);
                item.put("answer", answer);
                out.add(item);
            }
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * 从模型回复里抠出JSON：模型常夹带解释文字或```json围栏，
     * 截取首尾花括号之间的片段再解析；拿不到返回null
     */
    private JsonNode readJson(String content) {
        if (content == null) {
            return null;
        }
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            return objectMapper.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
