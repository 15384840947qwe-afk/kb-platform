package com.lyq.kb.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Editor.js的JSON块转纯文本：出题和文档问答共用的抽取逻辑 */
public class DocText {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 取每个块的text/code，text剥HTML标签；解析失败当空文本，上层各自处理 */
    public static String toPlainText(String json) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonNode root = MAPPER.readTree(json);
            for (JsonNode b : root.path("blocks")) {
                String t = b.path("data").path("text").asText("");
                if (!t.isBlank()) {
                    sb.append(t.replaceAll("<[^>]+>", "")).append('\n');
                }
                String code = b.path("data").path("code").asText("");
                if (!code.isBlank()) {
                    sb.append(code).append('\n');
                }
            }
        } catch (Exception e) {
            // 内容不是合法JSON就当空文本
        }
        return sb.toString();
    }
}
