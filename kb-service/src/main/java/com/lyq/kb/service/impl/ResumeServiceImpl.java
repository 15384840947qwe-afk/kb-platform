package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.ForbiddenException;
import com.lyq.kb.common.ResumePrompts;
import com.lyq.kb.common.Sse;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.ResumeGenerateRequest;
import com.lyq.kb.dto.ResumeImportVO;
import com.lyq.kb.dto.ResumeJdRequest;
import com.lyq.kb.dto.ResumeSaveRequest;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.entity.Practice;
import com.lyq.kb.entity.Resume;
import com.lyq.kb.entity.Question;
import com.lyq.kb.mapper.InterviewMapper;
import com.lyq.kb.mapper.PracticeMapper;
import com.lyq.kb.mapper.QuestionMapper;
import com.lyq.kb.mapper.ResumeMapper;
import com.lyq.kb.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    /** 空模板：和ResumePrompts.SCHEMA字段一致，前端兜底也用它 */
    private static final String EMPTY_TEMPLATE =
            "{\"basics\":{\"name\":\"\",\"phone\":\"\",\"email\":\"\",\"city\":\"\",\"github\":\"\",\"blog\":\"\"}," +
            "\"work\":[],\"projects\":[],\"education\":[],\"skills\":[],\"awards\":[]}";

    /** 原文进prompt的上限，防超长简历把上下文撑爆 */
    private static final int MAX_TEXT = 30000;

    private final ResumeMapper resumeMapper;
    private final PracticeMapper practiceMapper;
    private final InterviewMapper interviewMapper;
    private final QuestionMapper questionMapper;
    private final AiGrader aiGrader;
    private final ObjectMapper objectMapper;

    // ===== CRUD =====

    @Override
    public ResumeImportVO importFile(MultipartFile file) {
        AuthUtil.requireWritable();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!Set.of("pdf", "txt", "md").contains(ext)) {
            throw new IllegalArgumentException("仅支持导入 pdf/txt/md 格式");
        }
        String text;
        try {
            byte[] bytes = file.getBytes();
            if ("pdf".equals(ext)) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    text = new PDFTextStripper().getText(doc);
                }
            } else {
                text = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("文件读取失败，请确认文件未损坏");
        }
        text = text.replace("\r", "").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("无法从文件提取文本：空文件或扫描版PDF不支持");
        }
        if (text.length() > MAX_TEXT) {
            text = text.substring(0, MAX_TEXT);
        }

        // AI提取结构化字段；不可用或格式异常则退回空模板让用户手填
        String ai = aiGrader.chat(ResumePrompts.EXTRACT_SYSTEM, text, 60);
        JsonNode node = ai == null ? null : Sse.parseJson(ai, objectMapper);
        boolean aiParsed = node != null && (node.has("basics") || node.has("projects"));

        Resume row = new Resume();
        row.setUserId(UserContext.get().getId());
        String title = name.replaceFirst("\\.[^.]+$", "");
        row.setTitle(title.isBlank() ? "导入的简历" : title);
        row.setRawText(text);
        row.setContentJson(aiParsed ? node.toString() : EMPTY_TEMPLATE);
        row.setFileName(name);
        resumeMapper.insert(row);

        ResumeImportVO vo = new ResumeImportVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setFileName(name);
        vo.setRawText(text);
        vo.setContentJson(row.getContentJson());
        vo.setAiParsed(aiParsed);
        return vo;
    }

    @Override
    public Resume create(ResumeSaveRequest req) {
        AuthUtil.requireWritable();
        Resume row = new Resume();
        row.setUserId(UserContext.get().getId());
        row.setTitle(req == null || req.getTitle() == null || req.getTitle().isBlank()
                ? "新简历" : req.getTitle());
        row.setTargetJob(req == null ? null : req.getTargetJob());
        row.setContentJson(req != null && req.getContentJson() != null && !req.getContentJson().isBlank()
                ? req.getContentJson() : EMPTY_TEMPLATE);
        resumeMapper.insert(row);
        return row;
    }

    @Override
    public List<Resume> list() {
        return resumeMapper.selectList(new QueryWrapper<Resume>()
                .select(Resume.class, info -> {
                    String col = info.getColumn();
                    return !"raw_text".equals(col) && !"content_json".equals(col);
                })
                .eq("user_id", UserContext.get().getId())
                .orderByDesc("update_time"));
    }

    @Override
    public Resume detail(Long id) {
        return owned(id);
    }

    @Override
    public void update(Long id, ResumeSaveRequest req) {
        AuthUtil.requireWritable();
        Resume row = owned(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            row.setTitle(req.getTitle());
        }
        row.setTargetJob(req.getTargetJob());
        if (req.getContentJson() != null && !req.getContentJson().isBlank()) {
            row.setContentJson(req.getContentJson());
        }
        resumeMapper.updateById(row);
    }

    @Override
    public void delete(Long id) {
        AuthUtil.requireWritable();
        owned(id);
        resumeMapper.deleteById(id);
    }

    /** 取本人简历，不存在/不是自己的都拦掉 */
    private Resume owned(Long id) {
        Resume row = resumeMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        if (!row.getUserId().equals(UserContext.get().getId())) {
            throw new ForbiddenException("只能操作自己的简历");
        }
        return row;
    }

    // ===== 分析 =====

    @Override
    public void analyzeStream(Long id, Consumer<String> onDelta,
                              Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback) {
        Resume r = owned(id);
        String md = renderMarkdown(r.getContentJson());
        String targetJob = r.getTargetJob() == null || r.getTargetJob().isBlank()
                ? "未指定" : r.getTargetJob();

        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(ResumePrompts.ANALYZE_STREAM_SYSTEM,
                "目标岗位：" + targetJob + "\n简历内容：\n" + md, 120,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        Map<String, Object> analysis = parseAnalysis(Sse.parseJson(sp.json(), objectMapper), sp.text());
        boolean fallback = analysis == null;
        if (fallback) {
            // AI不可用或结果异常：本地规则兜底，有点评正文就带上
            analysis = localCheck(r.getContentJson());
            if (!sp.text().isBlank()) {
                analysis.put("summary", sp.text());
            }
        }
        saveAnalysis(r, analysis);
        analysis.put("id", r.getId());
        if (fallback) {
            onFallback.accept(analysis);
        } else {
            onDone.accept(analysis);
        }
    }

    private void saveAnalysis(Resume r, Map<String, Object> analysis) {
        try {
            r.setAnalysisJson(objectMapper.writeValueAsString(analysis));
            resumeMapper.updateById(r);
        } catch (Exception ignored) {
            // 落库失败不影响用户看结果
        }
    }

    // ===== JD匹配 =====

    @Override
    public void jdStream(Long id, ResumeJdRequest req, Consumer<String> onDelta,
                         Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback) {
        if (req == null || req.getJd() == null || req.getJd().isBlank()) {
            throw new IllegalArgumentException("请粘贴目标岗位的JD内容");
        }
        // JD太长截断，防把上下文撞爆
        String jd = req.getJd().trim();
        if (jd.length() > 4000) {
            jd = jd.substring(0, 4000);
        }
        Resume r = owned(id);
        String md = renderMarkdown(r.getContentJson());

        Sse.Splitter sp = new Sse.Splitter();
        aiGrader.chatStream(ResumePrompts.JD_MATCH_SYSTEM,
                "岗位JD：\n" + jd + "\n\n我的简历：\n" + md, 120,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        Map<String, Object> result = parseJdMatch(Sse.parseJson(sp.json(), objectMapper), sp.text());
        if (result == null) {
            // AI不可用或输出没按格式：有啥文本给啥，前端只展示点评
            Map<String, Object> fb = new LinkedHashMap<>();
            fb.put("summary", sp.text().isBlank() ? "AI暂时不可用，请稍后重试" : sp.text());
            onFallback.accept(fb);
            return;
        }
        result.put("id", r.getId());
        onDone.accept(result);
    }

    /** 解析JD匹配JSON；score缺失视为无效返回null */
    private Map<String, Object> parseJdMatch(JsonNode node, String text) {
        if (node == null || !node.hasNonNull("score")) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", node.path("score").asInt(0));
        out.put("matched", textList(node.path("matched")));
        out.put("missing", textList(node.path("missing")));
        out.put("suggestions", textList(node.path("suggestions")));
        out.put("summary", text);
        return out;
    }

    /** 解析AI评审JSON；score缺失视为无效返回null */
    private Map<String, Object> parseAnalysis(JsonNode node, String text) {
        if (node == null || !node.hasNonNull("score")) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", node.path("score").asInt(0));
        Map<String, Object> scores = new LinkedHashMap<>();
        node.path("scores").fields().forEachRemaining(e -> scores.put(e.getKey(), e.getValue().asInt(0)));
        out.put("scores", scores);
        out.put("strengths", textList(node.path("strengths")));
        List<Map<String, Object>> issues = new ArrayList<>();
        for (JsonNode i : node.path("issues")) {
            issues.add(Map.of(
                    "section", i.path("section").asText(""),
                    "severity", i.path("severity").asText("mid"),
                    "advice", i.path("advice").asText("")));
        }
        out.put("issues", issues);
        out.put("missing", textList(node.path("missing")));
        out.put("suggestKeywords", textList(node.path("suggestKeywords")));
        out.put("summary", text);
        return out;
    }

    /**
     * 本地规则兜底检查：不依赖AI也能查出最常见的问题。
     * 扣分权重：high -10 / mid -5 / low -2，下限30分
     */
    private Map<String, Object> localCheck(String contentJson) {
        List<Map<String, Object>> issues = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        JsonNode c = null;
        try {
            c = objectMapper.readTree(contentJson == null ? "{}" : contentJson);
        } catch (Exception ignored) {
        }
        JsonNode root = c == null ? objectMapper.createObjectNode() : c;
        JsonNode b = root.path("basics");
        checkBlank(b.path("name"), issues, missing, "basics", "high", "基本信息缺少姓名");
        checkBlank(b.path("phone"), issues, missing, "basics", "high", "基本信息缺少联系电话");
        checkBlank(b.path("email"), issues, missing, "basics", "mid", "基本信息缺少邮箱");
        if (root.path("education").isEmpty()) {
            issues.add(issue("education", "high", "缺少教育经历"));
            missing.add("教育经历");
        }
        if (root.path("work").isEmpty() && root.path("projects").isEmpty()) {
            issues.add(issue("projects", "high", "工作经历与项目/实践经历都为空，至少补充一类"));
            missing.add("工作或项目经历");
        }
        if (root.path("skills").isEmpty()) {
            issues.add(issue("skills", "mid", "缺少技能/专长清单"));
            missing.add("技能清单");
        }
        // 量化检查：经历描述里一个数字都没有，多半是空话
        if (!contentJson.matches("(?s).*\\d{2,}.*")) {
            issues.add(issue("projects", "mid",
                    "经历描述缺少量化数据（规模、百分比、名次等），加数字更有说服力"));
        }
        if (contentJson != null && contentJson.length() > 6000) {
            issues.add(issue("awards", "low", "内容偏长，建议精简到一页纸篇幅"));
        }
        int penalty = issues.stream().mapToInt(i -> switch ((String) i.get("severity")) {
            case "high" -> 10;
            case "mid" -> 5;
            default -> 2;
        }).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", Math.max(30, 100 - penalty));
        out.put("scores", Map.of());
        out.put("strengths", List.of());
        out.put("issues", issues);
        out.put("missing", missing);
        out.put("suggestKeywords", List.of());
        out.put("summary", "AI暂不可用，以下为本地规则检查清单。");
        out.put("fallback", true);
        return out;
    }

    /** JsonNode数组转字符串List，非数组返回空表 */
    private List<String> textList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> {
                if (!n.asText("").isBlank()) {
                    out.add(n.asText());
                }
            });
        }
        return out;
    }

    private void checkBlank(JsonNode field, List<Map<String, Object>> issues, List<String> missing,
                            String section, String severity, String advice) {
        if (field == null || field.asText("").isBlank()) {
            issues.add(issue(section, severity, advice));
            missing.add(advice.replaceFirst("基本信息缺少", "").replaceFirst("缺少", ""));
        }
    }

    private Map<String, Object> issue(String section, String severity, String advice) {
        return Map.of("section", section, "severity", severity, "advice", advice);
    }

    // ===== 生成 =====

    @Override
    public void generateStream(ResumeGenerateRequest req, Consumer<String> onDelta,
                               Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback) {
        AuthUtil.requireWritable();
        Resume existing = null;
        if (req.getId() != null) {
            existing = owned(req.getId());
        }
        StringBuilder user = new StringBuilder(ResumePrompts.GENERATE_USER_HEAD);
        user.append("\n目标岗位：")
                .append(req.getTargetJob() == null || req.getTargetJob().isBlank() ? "未指定" : req.getTargetJob());
        if (req.getContentJson() != null && !req.getContentJson().isBlank()) {
            String existMd = renderMarkdown(req.getContentJson());
            if (!existMd.isBlank()) {
                user.append("\n已有内容（在其基础上补全润色，保留已有事实，不要删除）：\n").append(existMd);
            }
        }
        String material = materialText(req);
        if (!material.isBlank()) {
            user.append('\n').append(material);
        }
        if (req.getNote() != null && !req.getNote().isBlank()) {
            user.append("\n补充说明：").append(req.getNote());
        }

        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(ResumePrompts.GENERATE_STREAM_SYSTEM, user.toString(), 90,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        String md = sp.text();
        if (!ok && md.isBlank()) {
            onFallback.accept(Map.of("message", "AI暂不可用，请在编辑页手动填写后保存"));
            return;
        }
        // 结构化回填：优先AI给的contentJson，其次用户已有表单，最后空模板
        JsonNode node = Sse.parseJson(sp.json(), objectMapper);
        String contentJson = null;
        if (node != null && node.hasNonNull("contentJson")) {
            contentJson = node.get("contentJson").toString();
        }
        if (contentJson == null && req.getContentJson() != null && !req.getContentJson().isBlank()) {
            contentJson = req.getContentJson();
        }
        if (contentJson == null) {
            contentJson = EMPTY_TEMPLATE;
        }

        Resume row = existing != null ? existing : new Resume();
        if (existing == null) {
            row.setUserId(UserContext.get().getId());
        }
        row.setTargetJob(req.getTargetJob());
        row.setRawText(md);
        row.setContentJson(contentJson);
        if (existing == null) {
            row.setTitle(deriveTitle(contentJson, req.getTargetJob()));
            resumeMapper.insert(row);
        } else {
            resumeMapper.updateById(row);
        }
        onDone.accept(Map.of("id", row.getId(), "markdown", md, "contentJson", contentJson,
                "title", row.getTitle()));
    }

    /** 新简历标题：姓名-目标岗位，缺谁省谁 */
    private String deriveTitle(String contentJson, String targetJob) {
        String name = "";
        try {
            name = objectMapper.readTree(contentJson).path("basics").path("name").asText("");
        } catch (Exception ignored) {
        }
        String job = targetJob == null ? "" : targetJob.trim();
        if (!name.isBlank() && !job.isBlank()) {
            return name + "-" + job;
        }
        if (!name.isBlank()) {
            return name + "的简历";
        }
        return job.isBlank() ? "生成的简历" : job + "简历";
    }

    // ===== 站内素材 =====

    @Override
    public Map<String, Object> materials() {
        Long me = UserContext.get().getId();
        Map<String, Object> out = new LinkedHashMap<>();
        // 刷题科目分布：和看板同一套内存聚合
        List<Practice> all = practiceMapper.selectList(
                new QueryWrapper<Practice>().eq("user_id", me));
        List<Map<String, Object>> drillTech = new ArrayList<>();
        if (!all.isEmpty()) {
            List<Long> qids = all.stream().map(Practice::getQuestionId).distinct().toList();
            Map<Long, String> categoryOf = questionMapper.selectBatchIds(qids).stream()
                    .collect(java.util.stream.Collectors.toMap(Question::getId,
                            q -> q.getCategory() == null ? "其他" : q.getCategory(), (a, b) -> a));
            Map<String, long[]> agg = new LinkedHashMap<>();
            for (Practice p : all) {
                long[] a = agg.computeIfAbsent(categoryOf.getOrDefault(p.getQuestionId(), "其他"),
                        k -> new long[2]);
                a[0]++;
                if (p.getResult() != null && p.getResult() == 1) {
                    a[1]++;
                }
            }
            agg.forEach((cat, a) -> drillTech.add(Map.of(
                    "category", cat, "total", a[0], "correct", a[1])));
        }
        out.put("drillTech", drillTech);
        // 模拟面试记录：按类别计数+均分
        List<Interview> interviews = interviewMapper.selectList(new QueryWrapper<Interview>()
                .select("id", "category", "score").eq("user_id", me));
        Map<String, long[]> byCat = new LinkedHashMap<>();
        for (Interview i : interviews) {
            String cat = i.getCategory() == null ? "其他" : i.getCategory();
            long[] a = byCat.computeIfAbsent(cat, k -> new long[2]);
            a[0]++;
            a[1] += i.getScore() == null ? 0 : i.getScore();
        }
        List<Map<String, Object>> interviewStats = new ArrayList<>();
        byCat.forEach((cat, a) -> interviewStats.add(Map.of(
                "category", cat, "count", a[0],
                "avgScore", a[0] == 0 ? 0 : Math.round(a[1] * 1.0 / a[0]))));
        out.put("interviews", interviewStats);
        return out;
    }

    /** 把勾选的站内素材拼成prompt素材段；只作线索注入，要求AI不得编造 */
    private String materialText(ResumeGenerateRequest req) {
        if (!req.isUseDrill() && !req.isUseInterview()) {
            return "";
        }
        Map<String, Object> m = materials();
        StringBuilder sb = new StringBuilder("站内学习线索（仅作参考，不得据此编造不存在的经历）：\n");
        if (req.isUseDrill()) {
            for (Object o : (List<?>) m.get("drillTech")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> d = (Map<String, Object>) o;
                sb.append("- 刷题《").append(d.get("category")).append("》共")
                        .append(d.get("total")).append("题，答对").append(d.get("correct")).append("题\n");
            }
        }
        if (req.isUseInterview()) {
            for (Object o : (List<?>) m.get("interviews")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> d = (Map<String, Object>) o;
                sb.append("- 模拟面试《").append(d.get("category")).append("》")
                        .append(d.get("count")).append("场，平均分").append(d.get("avgScore")).append("\n");
            }
        }
        return sb.toString();
    }

    // ===== 导出 =====

    @Override
    public String exportMarkdown(Long id) {
        Resume r = owned(id);
        return renderMarkdown(r.getContentJson());
    }

    /**
     * contentJson → Markdown（单栏一页纸风格，billryan/resume式排版）：
     * 分析时当AI输入、导出时当成品，一个渲染器两用
     */
    private String renderMarkdown(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return "";
        }
        JsonNode c;
        try {
            c = objectMapper.readTree(contentJson);
        } catch (Exception e) {
            return contentJson;
        }
        StringBuilder sb = new StringBuilder();
        JsonNode b = c.path("basics");
        sb.append("# ").append(b.path("name").asText("")).append('\n');
        List<String> contacts = new ArrayList<>();
        for (String k : new String[]{"phone", "email", "city", "github", "blog"}) {
            String v = b.path(k).asText("");
            if (!v.isBlank()) {
                contacts.add(v);
            }
        }
        if (!contacts.isEmpty()) {
            sb.append(String.join(" | ", contacts)).append("\n\n");
        }
        if (!c.path("work").isEmpty()) {
            sb.append("## 工作经历\n\n");
            for (JsonNode w : c.path("work")) {
                sb.append("**").append(w.path("company").asText()).append("** ")
                        .append(w.path("position").asText()).append("　")
                        .append(w.path("start").asText()).append("~").append(w.path("end").asText()).append('\n');
                for (JsonNode h : w.path("highlights")) {
                    if (!h.asText("").isBlank()) {
                        sb.append("- ").append(h.asText()).append('\n');
                    }
                }
                sb.append('\n');
            }
        }
        if (!c.path("projects").isEmpty()) {
            sb.append("## 项目/实践经历\n\n");
            for (JsonNode p : c.path("projects")) {
                sb.append("**").append(p.path("name").asText()).append("**")
                        .append(p.path("role").asText("").isBlank() ? "" : "（" + p.path("role").asText() + "）")
                        .append("　").append(p.path("start").asText()).append("~").append(p.path("end").asText()).append('\n');
                List<String> tech = new ArrayList<>();
                p.path("techStack").forEach(t -> {
                    if (!t.asText("").isBlank()) {
                        tech.add(t.asText());
                    }
                });
                if (!tech.isEmpty()) {
                    sb.append("关键词：").append(String.join("、", tech)).append('\n');
                }
                for (JsonNode h : p.path("highlights")) {
                    if (!h.asText("").isBlank()) {
                        sb.append("- ").append(h.asText()).append('\n');
                    }
                }
                sb.append('\n');
            }
        }
        if (!c.path("education").isEmpty()) {
            sb.append("## 教育经历\n\n");
            for (JsonNode e : c.path("education")) {
                sb.append("**").append(e.path("school").asText()).append("** ")
                        .append(e.path("degree").asText()).append(" ").append(e.path("major").asText())
                        .append("　").append(e.path("start").asText()).append("~").append(e.path("end").asText())
                        .append("\n\n");
            }
        }
        if (!c.path("skills").isEmpty()) {
            sb.append("## 专业技能\n\n");
            for (JsonNode s : c.path("skills")) {
                List<String> items = new ArrayList<>();
                s.path("items").forEach(i -> {
                    if (!i.asText("").isBlank()) {
                        items.add(i.asText());
                    }
                });
                sb.append("- ").append(s.path("category").asText("其他")).append("：")
                        .append(String.join("、", items)).append('\n');
            }
            sb.append('\n');
        }
        if (!c.path("awards").isEmpty()) {
            sb.append("## 荣誉奖项\n\n");
            for (JsonNode a : c.path("awards")) {
                if (!a.asText("").isBlank()) {
                    sb.append("- ").append(a.asText()).append('\n');
                }
            }
        }
        return sb.toString().trim();
    }
}
