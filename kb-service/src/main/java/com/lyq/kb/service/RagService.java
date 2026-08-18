package com.lyq.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.DocText;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.DocChunk;
import com.lyq.kb.mapper.DocChunkMapper;
import com.lyq.kb.mapper.DocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG检索服务：文档切块→向量化→混合检索（余弦相似度+关键词）。
 * embedding不可用时自动降级为纯关键词检索，链路永远能用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocChunkMapper chunkMapper;
    private final DocMapper docMapper;
    private final AiGrader aiGrader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 英文单词/技术词 */
    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z0-9_+#.\\-]*");

    /** embedding批量请求一次最多带几块，防单次请求过大 */
    private static final int EMBED_BATCH = 16;

    /** 按段落切块并合并到约400字/块，避免单句切散语义 */
    public List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String para : text.split("\n")) {
            if (!cur.isEmpty() && cur.length() + para.length() > 400) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(para).append('\n');
        }
        if (!cur.isEmpty()) {
            out.add(cur.toString());
        }
        return out;
    }

    /** 重建一篇文档的向量索引：删旧块→切块→批量embedding→落库。任何一步失败都不抛出（索引是增强不是依赖） */
    public void rebuild(Long docId) {
        try {
            Doc doc = docMapper.selectById(docId);
            if (doc == null) {
                return;
            }
            chunkMapper.delete(new QueryWrapper<DocChunk>().eq("doc_id", docId));
            String text = DocText.toPlainText(doc.getContent());
            List<String> chunks = chunk(text);
            if (chunks.isEmpty()) {
                return;
            }
            // 分批向量化；embedding不可用时vecs=null，照样存纯文本块供关键词检索
            List<float[]> vecs = new ArrayList<>();
            boolean embedOk = true;
            for (int i = 0; i < chunks.size() && embedOk; i += EMBED_BATCH) {
                List<String> batch = chunks.subList(i, Math.min(i + EMBED_BATCH, chunks.size()));
                List<float[]> part = aiGrader.embed(batch, 30);
                if (part == null) {
                    embedOk = false;
                } else {
                    vecs.addAll(part);
                }
            }
            for (int i = 0; i < chunks.size(); i++) {
                DocChunk c = new DocChunk();
                c.setDocId(docId);
                c.setSeq(i);
                c.setContent(chunks.get(i));
                if (embedOk && i < vecs.size()) {
                    c.setEmbedding(objectMapper.writeValueAsString(vecs.get(i)));
                }
                chunkMapper.insert(c);
            }
        } catch (Exception e) {
            log.warn("文档{}向量索引重建失败：{}", docId, e.getMessage());
        }
    }

    /** 懒建索引：一块都没有就先建（老文档没跑过重建也能用） */
    public void ensure(Long docId) {
        Long n = chunkMapper.selectCount(new QueryWrapper<DocChunk>().eq("doc_id", docId));
        if (n == null || n == 0) {
            rebuild(docId);
        }
    }

    /**
     * 混合检索单篇文档：余弦0.6 + 关键词0.4；embedding不可用退回纯关键词；
     * 一个都没分就取开头topK块兑底
     */
    public List<String> retrieve(Long docId, String question, int topK) {
        ensure(docId);
        List<DocChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<DocChunk>().eq("doc_id", docId).orderByAsc("seq"));
        if (chunks.isEmpty()) {
            return List.of();
        }
        float[] qv = aiGrader.embedOne(question);
        Set<String> toks = tokens(question);
        List<Map.Entry<String, Double>> scored = new ArrayList<>();
        for (DocChunk c : chunks) {
            double kw = kwScore(c.getContent(), toks);
            double cos = qv == null ? 0 : cosine(qv, parseVec(c.getEmbedding()));
            double score = qv == null ? kw : 0.6 * cos + 0.4 * kw;
            scored.add(Map.entry(c.getContent(), score));
        }
        List<String> picked = scored.stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        // 全部零分（问题太短/词都没命中）就取开头块，总比空上下文强
        if (scored.stream().allMatch(e -> e.getValue() <= 0)) {
            picked = chunks.stream().limit(topK).map(DocChunk::getContent).collect(Collectors.toList());
        }
        return picked;
    }

    /**
     * 全局检索：跨所有文档找最相关的块（按岗位出题接地用）。
     * 返回「《文档标题》块内容」列表，带标题方便prompt里注明来源
     */
    public List<String> searchGlobal(String query, int topK) {
        List<DocChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<DocChunk>().select("id", "doc_id", "content", "embedding"));
        if (chunks.isEmpty()) {
            return List.of();
        }
        float[] qv = aiGrader.embedOne(query);
        Set<String> toks = tokens(query);
        List<Map.Entry<Long, Double>> scored = new ArrayList<>();
        Map<Long, DocChunk> byId = new LinkedHashMap<>();
        for (DocChunk c : chunks) {
            double kw = kwScore(c.getContent(), toks);
            double cos = qv == null ? 0 : cosine(qv, parseVec(c.getEmbedding()));
            double score = qv == null ? kw : 0.6 * cos + 0.4 * kw;
            scored.add(Map.entry(c.getId(), score));
            byId.put(c.getId(), c);
        }
        List<DocChunk> picked = scored.stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .filter(e -> e.getValue() > 0)
                .map(e -> byId.get(e.getKey()))
                .collect(Collectors.toList());
        if (picked.isEmpty()) {
            return List.of();
        }
        // 带出文档标题
        Set<Long> docIds = picked.stream().map(DocChunk::getDocId).collect(Collectors.toSet());
        Map<Long, String> titles = docMapper.selectBatchIds(docIds).stream()
                .collect(Collectors.toMap(Doc::getId, Doc::getTitle, (a, b) -> a));
        return picked.stream()
                .map(c -> "《" + titles.getOrDefault(c.getDocId(), "未知文档") + "》" + c.getContent().trim())
                .collect(Collectors.toList());
    }

    /** 关键词得分：命中词数占问题词数比例，封顶1 */
    private double kwScore(String content, Set<String> toks) {
        if (toks.isEmpty()) {
            return 0;
        }
        String lower = content.toLowerCase();
        long hits = toks.stream().filter(lower::contains).count();
        return Math.min(1.0, (double) hits / toks.size());
    }

    /** 问题分词：英文单词+中文二字滑窗 */
    private Set<String> tokens(String question) {
        Set<String> set = new LinkedHashSet<>();
        Matcher m = WORD.matcher(question);
        while (m.find()) {
            set.add(m.group().toLowerCase());
        }
        String cjk = question.replaceAll("[^\u4e00-\u9fff]", "");
        for (int i = 0; i + 2 <= cjk.length(); i++) {
            set.add(cjk.substring(i, i + 2));
        }
        return set;
    }

    /** 余弦相似度；任一向量为null或维度不匹配返回0 */
    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0;
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** 反序列化块向量；坏了当没有 */
    private float[] parseVec(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = (float) arr.get(i).asDouble();
            }
            return vec;
        } catch (Exception e) {
            return null;
        }
    }
}
