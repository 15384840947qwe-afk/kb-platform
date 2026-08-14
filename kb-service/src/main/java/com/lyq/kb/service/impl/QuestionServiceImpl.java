package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.DocText;
import com.lyq.kb.dto.QuestionRequest;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.Question;
import com.lyq.kb.mapper.CatalogMapper;
import com.lyq.kb.mapper.DocMapper;
import com.lyq.kb.mapper.QuestionMapper;
import com.lyq.kb.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final DocMapper docMapper;
    private final CatalogMapper catalogMapper;
    private final AiGrader aiGrader;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> categories() {
        return questionMapper.selectCategories();
    }

    @Override
    public List<Question> list(String category) {
        QueryWrapper<Question> qw = new QueryWrapper<Question>().orderByDesc("id");
        if (category != null && !category.isBlank()) {
            qw.eq("category", category);
        }
        return questionMapper.selectList(qw);
    }

    @Override
    public Question create(QuestionRequest req) {
        // 题库是固定 curated 内容，管理权归管理员
        AuthUtil.requireAdmin();
        Question q = new Question();
        copy(req, q);
        questionMapper.insert(q);
        return q;
    }

    @Override
    public Question update(Long id, QuestionRequest req) {
        AuthUtil.requireAdmin();
        Question q = mustGet(id);
        copy(req, q);
        questionMapper.updateById(q);
        return q;
    }

    @Override
    public void delete(Long id) {
        AuthUtil.requireAdmin();
        mustGet(id);
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public List<Question> generateFromDoc(Long docId) {
        AuthUtil.requireAdmin();
        Doc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        // 教材JSON转纯文本，截断控制token成本
        String text = DocText.toPlainText(doc.getContent());
        if (text.length() < 200) {
            throw new IllegalArgumentException("文档内容太短，生成不了练习题");
        }
        if (text.length() > 3000) {
            text = text.substring(0, 3000);
        }
        String content = aiGrader.chat(
                "你是题库出题专家。基于教材出3道题：1道单选、1道填空、1道简答。" +
                "只输出JSON数组，元素形如{\"type\":\"SINGLE|FILL|SHORT\",\"stem\":\"题干\"," +
                "\"options\":[\"选项1\",\"选项2\",\"选项3\",\"选项4\"](仅单选需要)," +
                "\"answer\":\"答案\",\"explanation\":\"解析\"}",
                "教材标题：" + doc.getTitle() + "\n内容：\n" + text,
                60);
        if (content == null) {
            throw new IllegalArgumentException("AI暂不可用，稍后再试");
        }
        int s = content.indexOf('[');
        int e = content.lastIndexOf(']');
        if (s < 0 || e <= s) {
            throw new IllegalArgumentException("AI返回格式异常，再试一次");
        }
        List<Question> out = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(content.substring(s, e + 1));
            String category = rootCategory(docId);
            for (JsonNode n : arr) {
                String type = n.path("type").asText("");
                String stem = n.path("stem").asText("");
                String answer = n.path("answer").asText("");
                if (!List.of("SINGLE", "FILL", "SHORT").contains(type) || stem.isBlank() || answer.isBlank()) {
                    continue;
                }
                Question q = new Question();
                q.setCategory(category);
                q.setType(type);
                q.setStem(stem);
                q.setAnswer(answer);
                q.setExplanation(n.path("explanation").asText(""));
                // 生成的题自动挂回这篇教材：刷错能跳回去看
                q.setRelatedDocId(docId);
                if ("SINGLE".equals(type)) {
                    JsonNode ops = n.path("options");
                    if (!ops.isArray() || ops.size() < 2) {
                        continue;
                    }
                    List<String> os = new ArrayList<>();
                    ops.forEach(o -> os.add(o.asText()));
                    q.setOptions(objectMapper.writeValueAsString(os));
                }
                questionMapper.insert(q);
                out.add(q);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI结果解析失败，再试一次");
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("没生成有效题目，换篇内容充实的文档试试");
        }
        return out;
    }

    /** 沿目录节点向上找到根文件夹标题当科目 */
    private String rootCategory(Long docId) {
        Catalog node = catalogMapper.selectOne(
                new QueryWrapper<Catalog>().eq("doc_id", docId).last("limit 1"));
        if (node == null) {
            return "教材题";
        }
        Long cur = node.getParentId();
        Catalog root = null;
        while (cur != null && cur != 0) {
            root = catalogMapper.selectById(cur);
            if (root == null) {
                break;
            }
            cur = root.getParentId();
        }
        return root != null ? root.getTitle() : "教材题";
    }

    private void copy(QuestionRequest req, Question q) {
        q.setCategory(req.getCategory());
        q.setType(req.getType());
        q.setStem(req.getStem());
        q.setOptions(req.getOptions());
        q.setAnswer(req.getAnswer());
        q.setExplanation(req.getExplanation());
        q.setRelatedDocId(req.getRelatedDocId());
    }

    private Question mustGet(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        return q;
    }
}
