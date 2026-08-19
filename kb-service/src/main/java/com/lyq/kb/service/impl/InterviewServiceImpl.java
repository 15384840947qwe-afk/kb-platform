package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.ForbiddenException;
import com.lyq.kb.common.Sse;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.AppearanceEvalRequest;
import com.lyq.kb.dto.AppearanceEvalVO;
import com.lyq.kb.dto.InterviewEvaluateRequest;
import com.lyq.kb.dto.InterviewEvaluateVO;
import com.lyq.kb.dto.InterviewQuestionVO;
import com.lyq.kb.dto.InterviewReportRequest;
import com.lyq.kb.dto.InterviewStartRequest;
import com.lyq.kb.dto.ResumeReviewVO;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.entity.Question;
import com.lyq.kb.entity.Resume;
import com.lyq.kb.mapper.InterviewMapper;
import com.lyq.kb.mapper.QuestionMapper;
import com.lyq.kb.mapper.ResumeMapper;
import com.lyq.kb.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    /** 流式判分提示词：先出点评文本（逐字上屏），再出<<<RESULT>>>+结构化判定 */
    private static final String EVALUATE_STREAM_SYSTEM =
            "你是资深技术面试官。对照参考答案评估考生答案，给出百分制得分。" +
            "评分标准：完全答出核心要点90-100；答出大部分要点70-89；" +
            "答出部分要点但有明显遗漏50-69；答偏或只沾边30-49；完全不会0-29。" +
            "点评一两句话，先肯定后指缺；然后决定追问：若还有追问额度且答案有值得深挖的点" +
            "（答得薄或漏了关键处）就提一个追问，追问不得与对话记录中已有的追问重复；否则不追问。" +
            "输出格式：先直接输出点评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"score\":0到100整数,\"pass\":true或false,\"followUp\":\"...\"或null}";

    /** 流式总评提示词：summary先流式上屏，结构化部分在标记后 */
    private static final String REPORT_STREAM_SYSTEM =
            "你是面试总考官。根据逐题得分和点评生成总评，总分应接近各题得分的平均值。" +
            "输出格式：先直接输出两三句总评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"score\":0-100整数,\"strengths\":[强项],\"weaknesses\":[弱项],\"suggestions\":[备考建议]}";

    /** 简历审核提示词 */
    private static final String RESUME_REVIEW_SYSTEM =
            "你是资深HR面试官。审核候选人简历，给出亮点、疑虑和面试考察方向。" +
            "输出格式：先直接输出两三句简历总体评价，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"highlights\":[亮点],\"concerns\":[疑虑],\"suggestions\":[面试考察方向]}";

    private final QuestionMapper questionMapper;
    private final InterviewMapper interviewMapper;
    private final ResumeMapper resumeMapper;
    private final AiGrader aiGrader;
    private final ObjectMapper objectMapper;

    /* ==================== 开考 ==================== */

    @Override
    public List<InterviewQuestionVO> start(InterviewStartRequest req) {
        if (req.getResumeId() != null) {
            return startFromResume(req);
        }
        return startFromQuestionBank(req);
    }

    /** 题库出题（原有逻辑） */
    private List<InterviewQuestionVO> startFromQuestionBank(InterviewStartRequest req) {
        QueryWrapper<Question> qw = new QueryWrapper<Question>().eq("type", "SHORT");
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            qw.eq("category", req.getCategory());
        }
        List<Question> pool = questionMapper.selectList(qw);
        Collections.shuffle(pool);
        List<Question> picked = pool.stream().limit(req.getCount()).collect(Collectors.toList());
        if (picked.size() < req.getCount()) {
            List<Long> got = picked.stream().map(Question::getId).collect(Collectors.toList());
            QueryWrapper<Question> more = new QueryWrapper<Question>().notIn("id", got.isEmpty() ? List.of(-1L) : got);
            if (req.getCategory() != null && !req.getCategory().isBlank()) {
                more.eq("category", req.getCategory());
            }
            List<Question> extra = questionMapper.selectList(more);
            Collections.shuffle(extra);
            for (Question q : extra) {
                if (picked.size() >= req.getCount()) break;
                picked.add(q);
            }
        }
        return picked.stream().map(q -> {
            InterviewQuestionVO vo = new InterviewQuestionVO();
            vo.setId(q.getId());
            vo.setStem(q.getStem());
            vo.setRelatedDocId(q.getRelatedDocId());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 简历出题：AI根据简历内容生成针对性面试题+参考答案 */
    private List<InterviewQuestionVO> startFromResume(InterviewStartRequest req) {
        Resume resume = resumeMapper.selectById(req.getResumeId());
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        if (!resume.getUserId().equals(UserContext.get().getId())) {
            throw new ForbiddenException("只能用自己的简历");
        }

        String resumeText = buildResumeText(resume);
        String categoryHint = (req.getCategory() != null && !req.getCategory().isBlank())
                ? "，侧重" + req.getCategory() + "方向" : "";

        String system = "你是资深技术面试官。根据候选人简历出" + req.getCount() + "道针对性面试题。" +
                "题目要围绕简历中的具体项目、技术栈、工作经历来问，像真实面试一样自然口语化。" +
                "每题附参考答案要点（2-4句），用于后续评估。" +
                "只输出JSON：{\"questions\":[{\"stem\":\"题目\",\"answer\":\"参考答案要点\"}]}";
        String user = "候选人简历：\n" + resumeText + "\n请出" + req.getCount() + "道题" + categoryHint + "。";

        String content = aiGrader.chat(system, user, 60);
        JsonNode node = parseJson(content);

        List<InterviewQuestionVO> result = new ArrayList<>();
        if (node != null && node.path("questions").isArray()) {
            for (JsonNode q : node.path("questions")) {
                InterviewQuestionVO vo = new InterviewQuestionVO();
                vo.setId(null); // 简历出题不入题库
                vo.setStem(q.path("stem").asText("").trim());
                vo.setReferenceAnswer(q.path("answer").asText("").trim());
                if (!vo.getStem().isEmpty()) {
                    result.add(vo);
                }
            }
        }

        // AI生成失败时降级到题库
        if (result.isEmpty()) {
            return startFromQuestionBank(req);
        }
        return result;
    }

    /* ==================== 判分 ==================== */

    @Override
    public InterviewEvaluateVO evaluate(InterviewEvaluateRequest req) {
        String stem, reference;
        if (req.getQuestionId() != null) {
            Question q = questionMapper.selectById(req.getQuestionId());
            if (q == null) throw new IllegalArgumentException("题目不存在");
            stem = q.getStem();
            reference = q.getAnswer();
        } else {
            stem = req.getStem();
            reference = req.getReferenceAnswer();
        }
        if (stem == null || stem.isBlank()) {
            throw new IllegalArgumentException("题目不能为空");
        }

        String resumeCtx = resolveResumeContext(req);
        InterviewEvaluateVO vo = new InterviewEvaluateVO();
        vo.setReference(reference);

        String content = aiGrader.chat(
                buildEvaluateSystem(resumeCtx),
                buildEvaluateUser(stem, reference, req, resumeCtx),
                30);
        if (content == null) {
            vo.setPass(null);
            return vo;
        }
        try {
            int s = content.indexOf('{');
            int e = content.lastIndexOf('}');
            if (s < 0 || e <= s) {
                vo.setPass(null);
                return vo;
            }
            JsonNode node = objectMapper.readTree(content.substring(s, e + 1));
            vo.setScore(node.path("score").asInt(-1));
            if (vo.getScore() < 0) vo.setScore(null);
            vo.setPass(node.path("pass").asBoolean(false));
            vo.setComment(node.path("comment").asText(""));
            boolean canFollow = req.getFollowUsed() < req.getMaxFollow();
            JsonNode fu = node.path("followUp");
            vo.setFollowUp(canFollow && !fu.isNull() && !fu.asText("").isBlank() ? fu.asText() : null);
        } catch (Exception ex) {
            vo.setPass(null);
            vo.setScore(null);
        }
        return vo;
    }

    @Override
    public void evaluateStream(InterviewEvaluateRequest req, Consumer<String> onDelta,
                               Consumer<InterviewEvaluateVO> onDone, Consumer<InterviewEvaluateVO> onFallback) {
        String stem, reference;
        if (req.getQuestionId() != null) {
            Question q = questionMapper.selectById(req.getQuestionId());
            if (q == null) throw new IllegalArgumentException("题目不存在");
            stem = q.getStem();
            reference = q.getAnswer();
        } else {
            stem = req.getStem();
            reference = req.getReferenceAnswer();
        }
        if (stem == null || stem.isBlank()) {
            throw new IllegalArgumentException("题目不能为空");
        }

        String resumeCtx = resolveResumeContext(req);
        InterviewEvaluateVO vo = new InterviewEvaluateVO();
        vo.setReference(reference);

        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(
                buildEvaluateSystem(resumeCtx),
                buildEvaluateUser(stem, reference, req, resumeCtx),
                45,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        JsonNode node = Sse.parseJson(sp.json(), objectMapper);
        if (!ok && node == null) {
            onFallback.accept(vo);
            return;
        }
        vo.setComment(sp.text());
        if (node != null) {
            vo.setScore(node.path("score").asInt(-1));
            if (vo.getScore() < 0) vo.setScore(null);
            vo.setPass(node.path("pass").asBoolean(false));
            boolean canFollow = req.getFollowUsed() < req.getMaxFollow();
            JsonNode fu = node.path("followUp");
            vo.setFollowUp(canFollow && !fu.isNull() && !fu.asText("").isBlank() ? fu.asText() : null);
        }
        onDone.accept(vo);
    }

    /** 判分系统提示词：有简历上下文时增加"结合简历评估"指引 */
    private String buildEvaluateSystem(String resumeContext) {
        if (resumeContext != null && !resumeContext.isBlank()) {
            return "你是资深技术面试官。结合候选人简历背景评估其回答，给出百分制得分。" +
                    "评分标准：完全答出核心要点90-100；答出大部分要点70-89；" +
                    "答出部分要点但有明显遗漏50-69；答偏或只沾边30-49；完全不会0-29。" +
                    "关注候选人是否真正理解自己简历中提到的技术。" +
                    "点评一两句话，先肯定后指缺；然后决定追问：若还有追问额度且答案有值得深挖的点" +
                    "（答得薄或漏了关键处）就提一个追问，追问不得与对话记录中已有的追问重复；否则不追问。" +
                    "输出格式：先直接输出点评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
                    "{\"score\":0到100整数,\"pass\":true或false,\"followUp\":\"...\"或null}";
        }
        return EVALUATE_STREAM_SYSTEM;
    }

    /** 判分用户提示词：同步/流式共用 */
    private String buildEvaluateUser(String stem, String reference, InterviewEvaluateRequest req, String resumeContext) {
        StringBuilder sb = new StringBuilder()
                .append("题目：").append(stem);
        if (reference != null && !reference.isBlank()) {
            sb.append("\n参考答案：").append(reference);
        }
        sb.append("\n考生答案：").append(req.getAnswer() == null ? "" : req.getAnswer())
                .append("\n已追问次数：").append(req.getFollowUsed()).append("/").append(req.getMaxFollow());
        if (resumeContext != null && !resumeContext.isBlank()) {
            sb.append("\n候选人简历摘要：").append(resumeContext);
        }
        if (req.getHistory() != null && !req.getHistory().isEmpty()) {
            sb.append("\n本场本题对话记录：\n");
            for (InterviewEvaluateRequest.HistoryItem h : req.getHistory()) {
                if (h.getText() == null || h.getText().isBlank()) continue;
                sb.append("interviewer".equals(h.getRole()) ? "面试官：" : "考生：")
                        .append(h.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    /* ==================== 简历审核 ==================== */

    @Override
    public void reviewResumeStream(Long resumeId, Consumer<String> onDelta, Consumer<ResumeReviewVO> onDone) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) throw new IllegalArgumentException("简历不存在");
        if (!resume.getUserId().equals(UserContext.get().getId())) {
            throw new ForbiddenException("只能审核自己的简历");
        }

        String resumeText = buildResumeText(resume);
        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(RESUME_REVIEW_SYSTEM, "候选人简历：\n" + resumeText, 60,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);

        ResumeReviewVO vo = new ResumeReviewVO();
        JsonNode node = ok ? Sse.parseJson(sp.json(), objectMapper) : null;
        if (node != null) {
            vo.setHighlights(jsonToStringList(node.path("highlights")));
            vo.setConcerns(jsonToStringList(node.path("concerns")));
            vo.setSuggestions(jsonToStringList(node.path("suggestions")));
        } else {
            // AI不可用或格式异常：降级给空结果
            vo.setHighlights(List.of("简历审核暂不可用"));
            vo.setConcerns(List.of());
            vo.setSuggestions(List.of("建议稍后重试"));
        }
        onDone.accept(vo);
    }

    /* ==================== 着装评估 ==================== */

    @Override
    public AppearanceEvalVO evaluateAppearance(AppearanceEvalRequest req) {
        AppearanceEvalVO vo = new AppearanceEvalVO();
        String desc = req.getOutfitDescription();
        if (desc == null || desc.isBlank()) {
            vo.setFormalityScore(5);
            vo.setComment("请描述你的穿着，我来帮你评估");
            vo.setGood("");
            vo.setImprove("建议描述上装、下装、鞋子和配饰等");
            return vo;
        }

        String content = aiGrader.chat(
                "你是面试形象顾问。根据求职者的着装描述评估面试着装是否得体。" +
                "只输出JSON：{\"formalityScore\":1到10的正式度整数,\"comment\":\"一两句话总评\",\"good\":\"得体的地方\",\"improve\":\"改进建议\"}",
                "面试者着装描述：" + desc, 20);
        if (content == null) {
            vo.setFormalityScore(5);
            vo.setComment("AI暂不可用，建议穿着正装或商务休闲参加面试");
            vo.setGood("");
            vo.setImprove("衬衫/西装外套/西裤/皮鞋是安全选择");
            return vo;
        }
        try {
            JsonNode node = parseJson(content);
            if (node != null) {
                vo.setFormalityScore(node.path("formalityScore").asInt(5));
                vo.setComment(node.path("comment").asText(""));
                vo.setGood(node.path("good").asText(""));
                vo.setImprove(node.path("improve").asText(""));
            } else {
                vo.setFormalityScore(5);
                vo.setComment("着装评估结果解析异常");
            }
        } catch (Exception e) {
            vo.setFormalityScore(5);
            vo.setComment("着装评估异常");
        }
        return vo;
    }

    /* ==================== 总评 ==================== */

    @Override
    public Map<String, Object> report(InterviewReportRequest req) {
        List<InterviewReportRequest.Item> items = req.getItems() == null ? List.of() : req.getItems();
        int fallbackScore = calcAvgScore(items);

        Map<String, Object> report = new HashMap<>();
        String ai = aiGrader.chat(
                "你是面试总考官。根据逐题得分和点评生成总评。总分应接近各题得分的平均值。只输出JSON：" +
                "{\"score\":0-100整数,\"summary\":两三句总评,\"strengths\":[强项],\"weaknesses\":[弱项],\"suggestions\":[备考建议]}",
                buildReportUser(req, items),
                40);
        if (ai != null) {
            try {
                int s = ai.indexOf('{');
                int e = ai.lastIndexOf('}');
                if (s >= 0 && e > s) {
                    JsonNode node = objectMapper.readTree(ai.substring(s, e + 1));
                    report.put("score", node.path("score").asInt(fallbackScore));
                    report.put("summary", node.path("summary").asText(""));
                    report.put("strengths", jsonToStringList(node.path("strengths")));
                    report.put("weaknesses", jsonToStringList(node.path("weaknesses")));
                    report.put("suggestions", jsonToStringList(node.path("suggestions")));
                } else {
                    ai = null;
                }
            } catch (Exception ex) {
                ai = null;
            }
        }
        if (ai == null) {
            report = fallbackReport(items, fallbackScore);
        }

        saveReport(req, report);
        return report;
    }

    @Override
    public void reportStream(InterviewReportRequest req, Consumer<String> onDelta,
                             Consumer<Map<String, Object>> onDone) {
        List<InterviewReportRequest.Item> items = req.getItems() == null ? List.of() : req.getItems();
        int fallbackScore = calcAvgScore(items);

        Map<String, Object> report = null;
        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(REPORT_STREAM_SYSTEM, buildReportUser(req, items), 60,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        JsonNode node = ok ? Sse.parseJson(sp.json(), objectMapper) : null;
        if (node != null) {
            report = new HashMap<>();
            report.put("score", node.path("score").asInt(fallbackScore));
            String summary = sp.text();
            report.put("summary", summary.isEmpty() ? node.path("summary").asText("") : summary);
            report.put("strengths", jsonToStringList(node.path("strengths")));
            report.put("weaknesses", jsonToStringList(node.path("weaknesses")));
            report.put("suggestions", jsonToStringList(node.path("suggestions")));
        }
        if (report == null) {
            report = fallbackReport(items, fallbackScore);
        }
        saveReport(req, report);
        onDone.accept(report);
    }

    /** 总评用户提示词：含每题得分、简历审核和着装评估上下文 */
    private String buildReportUser(InterviewReportRequest req, List<InterviewReportRequest.Item> items) {
        StringBuilder sb = new StringBuilder()
                .append("科目：").append(req.getCategory())
                .append("\n逐题：\n")
                .append(items.stream()
                        .map(i -> "- " + i.getStem() + " => "
                                + (i.getScore() != null ? i.getScore() + "分" : (Boolean.TRUE.equals(i.getPass()) ? "通过" : "未通过"))
                                + (i.getComment() == null ? "" : "（" + i.getComment() + "）"))
                        .collect(Collectors.joining("\n")));
        if (req.getResumeReview() != null && !req.getResumeReview().isBlank()) {
            sb.append("\n简历审核：").append(req.getResumeReview());
        }
        if (req.getAppearanceEval() != null && !req.getAppearanceEval().isBlank()) {
            sb.append("\n着装评估：").append(req.getAppearanceEval());
        }
        return sb.toString();
    }

    /** AI不可用时的本地兑底报告 */
    private Map<String, Object> fallbackReport(List<InterviewReportRequest.Item> items, int fallbackScore) {
        Map<String, Object> report = new HashMap<>();
        report.put("score", fallbackScore);
        report.put("summary", "AI暂不可用，本场按通过率自动计分。");
        report.put("strengths", items.stream().filter(i -> Boolean.TRUE.equals(i.getPass()))
                .map(InterviewReportRequest.Item::getStem).collect(Collectors.toList()));
        report.put("weaknesses", items.stream().filter(i -> !Boolean.TRUE.equals(i.getPass()))
                .map(InterviewReportRequest.Item::getStem).collect(Collectors.toList()));
        report.put("suggestions", List.of("把未通过的题加入错题本，回教材补齐后重刷"));
        return report;
    }

    /** 报告落库并把主键回填到返回体 */
    private void saveReport(InterviewReportRequest req, Map<String, Object> report) {
        Interview row = new Interview();
        row.setUserId(UserContext.get().getId());
        row.setCategory(req.getCategory());
        row.setScore((Integer) report.get("score"));
        row.setTranscript(req.getTranscript());
        try {
            row.setReport(objectMapper.writeValueAsString(report));
        } catch (Exception ex) {
            row.setReport("{}");
        }
        // 简历审核和着装评估存档（如果前端传了）
        if (req.getResumeReview() != null) {
            row.setResumeReview(req.getResumeReview());
        }
        if (req.getAppearanceEval() != null) {
            row.setAppearanceReview(req.getAppearanceEval());
        }
        interviewMapper.insert(row);
        report.put("id", row.getId());
    }

    /* ==================== 简历列表 & 面试记录 ==================== */

    @Override
    public List<Resume> myResumes() {
        return resumeMapper.selectList(new QueryWrapper<Resume>()
                .select("id", "title", "target_job", "name", "education", "work_years", "skills", "submit_status")
                .eq("user_id", UserContext.get().getId())
                .orderByDesc("update_time")
                .last("limit 20"));
    }

    @Override
    public List<Interview> list() {
        return interviewMapper.selectList(new QueryWrapper<Interview>()
                .select(Interview.class, info -> {
                    String col = info.getColumn();
                    return !"transcript".equals(col) && !"report".equals(col);
                })
                .eq("user_id", UserContext.get().getId())
                .orderByDesc("create_time")
                .last("limit 20"));
    }

    @Override
    public Interview detail(Long id) {
        Interview row = interviewMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        if (!row.getUserId().equals(UserContext.get().getId())) {
            throw new ForbiddenException("只能看自己的面试记录");
        }
        return row;
    }

    /* ==================== 工具方法 ==================== */

    /** 把简历各字段拼成AI可读的纯文本 */
    private String buildResumeText(Resume resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getName() != null) sb.append("姓名：").append(resume.getName()).append('\n');
        if (resume.getTargetJob() != null) sb.append("目标岗位：").append(resume.getTargetJob()).append('\n');
        if (resume.getEducation() != null) sb.append("学历：").append(resume.getEducation()).append('\n');
        if (resume.getWorkYears() != null) sb.append("工作年限：").append(resume.getWorkYears()).append("年\n");
        if (resume.getSkills() != null) sb.append("技能：").append(resume.getSkills()).append('\n');
        if (resume.getRawText() != null && !resume.getRawText().isBlank()) {
            String raw = resume.getRawText();
            sb.append("\n简历全文：\n").append(raw.length() > 3000 ? raw.substring(0, 3000) + "..." : raw);
        } else if (resume.getContentJson() != null && !resume.getContentJson().isBlank()) {
            String json = resume.getContentJson();
            sb.append("\n简历结构化数据：\n").append(json.length() > 3000 ? json.substring(0, 3000) + "..." : json);
        }
        return sb.toString();
    }

    /** 从resumeId自动取简历文本，优先resumeId，其次req.resumeContext，都没有返回null */
    private String resolveResumeContext(InterviewEvaluateRequest req) {
        if (req.getResumeId() != null) {
            Resume resume = resumeMapper.selectById(req.getResumeId());
            if (resume != null) {
                return buildResumeText(resume);
            }
        }
        return req.getResumeContext();
    }

    /** 计算各题平均分作为总评兜底分；无得分时按通过率折算 */
    private int calcAvgScore(List<InterviewReportRequest.Item> items) {
        if (items.isEmpty()) return 0;
        long scoredCount = items.stream().filter(i -> i.getScore() != null).count();
        if (scoredCount > 0) {
            int sum = items.stream().mapToInt(i -> i.getScore() != null ? i.getScore() : 0).sum();
            return Math.round((float) sum / items.size());
        }
        // 都没有score时降级为通过率
        long passCount = items.stream().filter(i -> Boolean.TRUE.equals(i.getPass())).count();
        return Math.round(passCount * 100f / items.size());
    }

    /** JSON数组节点 → String列表 */
    private List<String> jsonToStringList(JsonNode arr) {
        List<String> list = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    /** 截取第一对花括号之间的JSON解析；解析失败返回null */
    private JsonNode parseJson(String content) {
        if (content == null) return null;
        try {
            int s = content.indexOf('{');
            int e = content.lastIndexOf('}');
            if (s < 0 || e <= s) return null;
            return objectMapper.readTree(content.substring(s, e + 1));
        } catch (Exception ex) {
            return null;
        }
    }
}
