package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.ForbiddenException;
import com.lyq.kb.common.Sse;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.InterviewEvaluateRequest;
import com.lyq.kb.dto.InterviewEvaluateVO;
import com.lyq.kb.dto.InterviewQuestionVO;
import com.lyq.kb.dto.InterviewReportRequest;
import com.lyq.kb.dto.InterviewStartRequest;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.entity.Question;
import com.lyq.kb.mapper.InterviewMapper;
import com.lyq.kb.mapper.QuestionMapper;
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
            "你是资深技术面试官。对照参考答案评估考生答案：答出核心要点即pass。" +
            "点评一两句话，先肯定后指缺；然后决定追问：若还有追问额度且答案有值得深挖的点" +
            "（答得薄或漏了关键处）就提一个追问，追问不得与对话记录中已有的追问重复；否则不追问。" +
            "输出格式：先直接输出点评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"pass\":true或false,\"followUp\":\"...\"或null}";

    /** 流式总评提示词：summary先流式上屏，结构化部分在标记后 */
    private static final String REPORT_STREAM_SYSTEM =
            "你是面试总考官。根据逐题判分生成总评。" +
            "输出格式：先直接输出两三句总评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"score\":0-100整数,\"strengths\":[强项],\"weaknesses\":[弱项],\"suggestions\":[备考建议]}";

    private final QuestionMapper questionMapper;
    private final InterviewMapper interviewMapper;
    private final AiGrader aiGrader;
    private final ObjectMapper objectMapper;

    @Override
    public List<InterviewQuestionVO> start(InterviewStartRequest req) {
        // 面试就该答长的：优先简答题，不够再拿其他题型补
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

    @Override
    public InterviewEvaluateVO evaluate(InterviewEvaluateRequest req) {
        Question q = questionMapper.selectById(req.getQuestionId());
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        InterviewEvaluateVO vo = new InterviewEvaluateVO();
        vo.setReference(q.getAnswer());
        // 对照题库参考答案判分（grounding），并决定追问
        String content = aiGrader.chat(
                "你是资深技术面试官。对照参考答案评估考生答案：答出核心要点即pass。" +
                "点评一句话，先肯定后指缺。若还有追问额度且答案有值得深挖的点（答得薄或漏了关键处），" +
                "就提一个追问，追问不得与对话记录中已有的追问重复；否则followUp为null。只输出JSON：" +
                "{\"pass\":true或false,\"comment\":\"...\",\"followUp\":\"...\"或null}",
                evaluateUserPrompt(q, req),
                30);
        if (content == null) {
            // AI不可用：pass=null，前端降级自评
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
            vo.setPass(node.path("pass").asBoolean(false));
            vo.setComment(node.path("comment").asText(""));
            boolean canFollow = req.getFollowUsed() < req.getMaxFollow();
            JsonNode fu = node.path("followUp");
            vo.setFollowUp(canFollow && !fu.isNull() && !fu.asText("").isBlank() ? fu.asText() : null);
        } catch (Exception ex) {
            vo.setPass(null);
        }
        return vo;
    }

    /** 判分用户提示词：同步/流式共用，带本题对话线程给AI上下文 */
    private String evaluateUserPrompt(Question q, InterviewEvaluateRequest req) {
        StringBuilder sb = new StringBuilder()
                .append("题目：").append(q.getStem())
                .append("\n参考答案：").append(q.getAnswer())
                .append("\n考生答案：").append(req.getAnswer() == null ? "" : req.getAnswer())
                .append("\n已追问次数：").append(req.getFollowUsed()).append("/").append(req.getMaxFollow());
        if (req.getHistory() != null && !req.getHistory().isEmpty()) {
            sb.append("\n本场本题对话记录：\n");
            for (InterviewEvaluateRequest.HistoryItem h : req.getHistory()) {
                if (h.getText() == null || h.getText().isBlank()) {
                    continue;
                }
                sb.append("interviewer".equals(h.getRole()) ? "面试官：" : "考生：")
                        .append(h.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public void evaluateStream(InterviewEvaluateRequest req, Consumer<String> onDelta,
                               Consumer<InterviewEvaluateVO> onDone, Consumer<InterviewEvaluateVO> onFallback) {
        Question q = questionMapper.selectById(req.getQuestionId());
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        InterviewEvaluateVO vo = new InterviewEvaluateVO();
        vo.setReference(q.getAnswer());
        // 点评随delta逐段上屏；<<<RESULT>>>后的JSON由Splitter截住不外泄
        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(EVALUATE_STREAM_SYSTEM, evaluateUserPrompt(q, req), 45,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        JsonNode node = Sse.parseJson(sp.json(), objectMapper);
        if (!ok && node == null) {
            // 一个字都没拿到：前端降级自评（带参考答案）
            onFallback.accept(vo);
            return;
        }
        vo.setComment(sp.text());
        if (node != null) {
            vo.setPass(node.path("pass").asBoolean(false));
            boolean canFollow = req.getFollowUsed() < req.getMaxFollow();
            JsonNode fu = node.path("followUp");
            vo.setFollowUp(canFollow && !fu.isNull() && !fu.asText("").isBlank() ? fu.asText() : null);
        }
        // node==null：流到了但格式异常，pass留null，前端同样走自评
        onDone.accept(vo);
    }

    @Override
    public Map<String, Object> report(InterviewReportRequest req) {
        List<InterviewReportRequest.Item> items = req.getItems() == null ? List.of() : req.getItems();
        long passCount = items.stream().filter(i -> Boolean.TRUE.equals(i.getPass())).count();
        int fallbackScore = items.isEmpty() ? 0 : Math.round(passCount * 100f / items.size());

        // 终场总评：AI综合逐题表现；不可用则本地按通过率兑底
        Map<String, Object> report = new HashMap<>();
        String ai = aiGrader.chat(
                "你是面试总考官。根据逐题判分生成总评。只输出JSON：" +
                "{\"score\":0-100整数,\"summary\":两三句总评,\"strengths\":[强项],\"weaknesses\":[弱项],\"suggestions\":[备考建议]}",
                reportUserPrompt(req, items),
                40);
        if (ai != null) {
            try {
                int s = ai.indexOf('{');
                int e = ai.lastIndexOf('}');
                if (s >= 0 && e > s) {
                    JsonNode node = objectMapper.readTree(ai.substring(s, e + 1));
                    report.put("score", node.path("score").asInt(fallbackScore));
                    report.put("summary", node.path("summary").asText(""));
                    List<String> strengths = new ArrayList<>();
                    node.path("strengths").forEach(n -> strengths.add(n.asText()));
                    List<String> weaknesses = new ArrayList<>();
                    node.path("weaknesses").forEach(n -> weaknesses.add(n.asText()));
                    List<String> suggestions = new ArrayList<>();
                    node.path("suggestions").forEach(n -> suggestions.add(n.asText()));
                    report.put("strengths", strengths);
                    report.put("weaknesses", weaknesses);
                    report.put("suggestions", suggestions);
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

        // 落库：完成一场才存一行
        saveReport(req, report);
        return report;
    }

    @Override
    public void reportStream(InterviewReportRequest req, Consumer<String> onDelta,
                             Consumer<Map<String, Object>> onDone) {
        List<InterviewReportRequest.Item> items = req.getItems() == null ? List.of() : req.getItems();
        long passCount = items.stream().filter(i -> Boolean.TRUE.equals(i.getPass())).count();
        int fallbackScore = items.isEmpty() ? 0 : Math.round(passCount * 100f / items.size());

        Map<String, Object> report = null;
        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(REPORT_STREAM_SYSTEM, reportUserPrompt(req, items), 60,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        JsonNode node = ok ? Sse.parseJson(sp.json(), objectMapper) : null;
        if (node != null) {
            report = new HashMap<>();
            report.put("score", node.path("score").asInt(fallbackScore));
            String summary = sp.text();
            report.put("summary", summary.isEmpty() ? node.path("summary").asText("") : summary);
            List<String> strengths = new ArrayList<>();
            node.path("strengths").forEach(n -> strengths.add(n.asText()));
            List<String> weaknesses = new ArrayList<>();
            node.path("weaknesses").forEach(n -> weaknesses.add(n.asText()));
            List<String> suggestions = new ArrayList<>();
            node.path("suggestions").forEach(n -> suggestions.add(n.asText()));
            report.put("strengths", strengths);
            report.put("weaknesses", weaknesses);
            report.put("suggestions", suggestions);
        }
        if (report == null) {
            // AI不可用或格式异常：本地兑底，降级文案直接done，不误导用户
            report = fallbackReport(items, fallbackScore);
        }
        saveReport(req, report);
        onDone.accept(report);
    }

    /** 总评用户提示词：同步/流式共用 */
    private String reportUserPrompt(InterviewReportRequest req, List<InterviewReportRequest.Item> items) {
        return "科目：" + req.getCategory() + "\n逐题：\n" + items.stream()
                .map(i -> "- " + i.getStem() + " => " + (Boolean.TRUE.equals(i.getPass()) ? "通过" : "未通过")
                        + (i.getComment() == null ? "" : "（" + i.getComment() + "）"))
                .collect(Collectors.joining("\n"));
    }

    /** AI不可用时的本地兑底报告：按通过率计分，强弱势=通过/未通过的题面 */
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
        interviewMapper.insert(row);
        report.put("id", row.getId());
    }

    @Override
    public List<Interview> list() {
        // 列表不取长文本字段，轻
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
}
