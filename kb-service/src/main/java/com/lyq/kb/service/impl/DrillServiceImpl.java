package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.Sse;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.DrillCheckVO;
import com.lyq.kb.dto.DrillPickVO;
import com.lyq.kb.dto.DrillStatsVO;
import com.lyq.kb.dto.WrongVO;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.entity.Practice;
import com.lyq.kb.entity.Question;
import com.lyq.kb.mapper.InterviewMapper;
import com.lyq.kb.mapper.PracticeMapper;
import com.lyq.kb.mapper.QuestionMapper;
import com.lyq.kb.service.DrillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrillServiceImpl implements DrillService {

    private final QuestionMapper questionMapper;
    private final PracticeMapper practiceMapper;
    private final InterviewMapper interviewMapper;
    private final AiGrader aiGrader;
    private final ObjectMapper objectMapper;

    @Override
    public List<DrillPickVO> pick(String category, String mode, int n) {
        int limit = Math.min(n, 50);
        List<Question> pool;
        if ("wrong".equals(mode)) {
            // 错题本：从自己最近答错的记录里圈题，@TableLogic自动滤掉已删题
            Long me = UserContext.get().getId();
            List<Long> wrongIds = practiceMapper.selectList(
                            new QueryWrapper<Practice>().eq("user_id", me).eq("result", 0)
                                    .orderByDesc("create_time").last("limit 300"))
                    .stream().map(Practice::getQuestionId).distinct().collect(Collectors.toList());
            pool = wrongIds.isEmpty() ? List.of() : questionMapper.selectBatchIds(wrongIds);
        } else if ("smart".equals(mode)) {
            // AI针对练：按科目正确率加权抽样，越薄弱抽到概率越高
            pool = smartPick(category);
        } else {
            QueryWrapper<Question> qw = new QueryWrapper<Question>();
            if (category != null && !category.isBlank()) {
                qw.eq("category", category);
            }
            pool = questionMapper.selectList(qw);
        }
        List<Question> shuffled = new ArrayList<>(pool);
        if (!"smart".equals(mode)) {
            // smart 模式已按权重排序，不能再洗牌，否则薄弱点优先级就丢了
            Collections.shuffle(shuffled);
        }
        return shuffled.stream().limit(limit).map(q -> {
            DrillPickVO vo = new DrillPickVO();
            vo.setId(q.getId());
            vo.setType(q.getType());
            vo.setStem(q.getStem());
            vo.setOptions(q.getOptions());
            // 注意：不带answer/explanation，判分权在check接口手里
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public DrillCheckVO check(Long questionId, String answer) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        DrillCheckVO vo = new DrillCheckVO();
        vo.setCorrectAnswer(q.getAnswer());
        vo.setExplanation(q.getExplanation());
        vo.setRelatedDocId(q.getRelatedDocId());
        String user = answer == null ? "" : answer;
        switch (q.getType()) {
            case "SINGLE" -> vo.setCorrect(user.trim().equalsIgnoreCase(q.getAnswer().trim()));
            case "MULTI" -> vo.setCorrect(normMulti(user).equals(normMulti(q.getAnswer())));
            case "FILL" -> vo.setCorrect(user.trim().toLowerCase().equals(q.getAnswer().trim().toLowerCase()));
            case "SHORT" -> {
                // 简答走AI；AI不可用返回null，前端降级自评
                Map<String, Object> grade = aiGrader.grade(q.getStem(), q.getAnswer(), user);
                if (grade != null) {
                    vo.setCorrect((Boolean) grade.get("pass"));
                    vo.setAiComment((String) grade.get("comment"));
                } else {
                    vo.setCorrect(null);
                }
            }
            default -> throw new IllegalArgumentException("未知题型");
        }
        return vo;
    }

    @Override
    public void checkStream(Long questionId, String answer, Consumer<String> onDelta,
                            Consumer<DrillCheckVO> onDone, Consumer<DrillCheckVO> onFallback) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        if (!"SHORT".equals(q.getType())) {
            throw new IllegalArgumentException("仅简答题支持流式批改");
        }
        DrillCheckVO vo = new DrillCheckVO();
        vo.setCorrectAnswer(q.getAnswer());
        vo.setExplanation(q.getExplanation());
        vo.setRelatedDocId(q.getRelatedDocId());
        // 点评逐段上屏，<<<RESULT>>>后只跟{"pass":...}判定
        Sse.Splitter sp = new Sse.Splitter();
        boolean ok = aiGrader.chatStream(
                "你是技术面试官。对比参考答案与考生答案，考生答出核心要点即算对，不要求措辞一致。" +
                "点评要先肯定答对的部分再指出缺失，像老师不像判官。" +
                "输出格式：先直接输出点评文本，然后换行输出<<<RESULT>>>再跟JSON：{\"pass\":true或false}",
                "题目：" + q.getStem() + "\n参考答案：" + q.getAnswer() +
                "\n考生答案：" + (answer == null ? "" : answer),
                30,
                d -> sp.accept(d, onDelta));
        sp.flush(onDelta);
        JsonNode node = Sse.parseJson(sp.json(), objectMapper);
        if (!ok && node == null) {
            // AI不可用：correct=null，前端降级自评
            onFallback.accept(vo);
            return;
        }
        vo.setAiComment(sp.text());
        vo.setCorrect(node != null ? node.path("pass").asBoolean(false) : null);
        onDone.accept(vo);
    }

    @Override
    public void record(Long questionId, int result) {
        Practice p = new Practice();
        p.setUserId(UserContext.get().getId());
        p.setQuestionId(questionId);
        p.setResult(result);
        practiceMapper.insert(p);
    }

    @Override
    public DrillStatsVO stats() {
        Long me = UserContext.get().getId();
        List<Practice> list = practiceMapper.selectList(
                new QueryWrapper<Practice>().eq("user_id", me));
        long total = list.size();
        long known = list.stream().filter(p -> p.getResult() == 1).count();
        DrillStatsVO vo = new DrillStatsVO();
        vo.setTotal(total);
        vo.setKnown(known);
        vo.setRate(total == 0 ? 0 : Math.round(known * 100.0 / total));
        return vo;
    }

    @Override
    public Map<String, Object> dashboard() {
        Long me = UserContext.get().getId();
        List<Practice> all = practiceMapper.selectList(
                new QueryWrapper<Practice>().eq("user_id", me));
        Map<String, Object> out = new HashMap<>();

        // 分科目正确率：刷题记录关联题目拿科目，内存聚合
        List<Map<String, Object>> perCategory = new ArrayList<>();
        if (!all.isEmpty()) {
            List<Long> qids = all.stream().map(Practice::getQuestionId).distinct().collect(Collectors.toList());
            Map<Long, String> categoryOf = questionMapper.selectBatchIds(qids).stream()
                    .collect(Collectors.toMap(Question::getId,
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
            agg.forEach((cat, a) -> perCategory.add(Map.of(
                    "category", cat, "total", a[0], "correct", a[1],
                    "rate", a[0] == 0 ? 0 : Math.round(a[1] * 100.0 / a[0]))));
        }
        out.put("perCategory", perCategory);

        // 近七天：每日刷题量与正确数，没刷的天也占位为0，图表才连续
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> recent7 = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long total = 0, correct = 0;
            for (Practice p : all) {
                if (p.getCreateTime() != null && d.equals(p.getCreateTime().toLocalDate())) {
                    total++;
                    if (p.getResult() != null && p.getResult() == 1) {
                        correct++;
                    }
                }
            }
            recent7.add(Map.of("date", d.toString(), "total", total, "correct", correct));
        }
        out.put("recent7", recent7);

        // 面试分数曲线：最近20场，按时间正序给图表画线
        List<Interview> scores = interviewMapper.selectList(new QueryWrapper<Interview>()
                .select("id", "category", "score", "create_time")
                .eq("user_id", me)
                .orderByDesc("create_time")
                .last("limit 20"));
        Collections.reverse(scores);
        out.put("scores", scores);
        return out;
    }

    @Override
    public List<WrongVO> wrongBook() {
        Long me = UserContext.get().getId();
        List<Practice> all = practiceMapper.selectList(
                new QueryWrapper<Practice>().eq("user_id", me).orderByAsc("create_time"));
        // 按题分组；最近一次仍答错才留在本子里，答对一次即毕业
        Map<Long, List<Practice>> byQ = all.stream()
                .collect(Collectors.groupingBy(Practice::getQuestionId));
        List<WrongVO> out = new ArrayList<>();
        for (Map.Entry<Long, List<Practice>> e : byQ.entrySet()) {
            List<Practice> recs = e.getValue();
            if (recs.get(recs.size() - 1).getResult() == 1) {
                continue;
            }
            Question q = questionMapper.selectById(e.getKey());
            if (q == null) {
                continue;
            }
            WrongVO vo = new WrongVO();
            vo.setId(q.getId());
            vo.setType(q.getType());
            vo.setStem(q.getStem());
            vo.setOptions(q.getOptions());
            vo.setAnswer(q.getAnswer());
            vo.setExplanation(q.getExplanation());
            vo.setRelatedDocId(q.getRelatedDocId());
            vo.setWrongCount(recs.stream().filter(p -> p.getResult() == 0).count());
            out.add(vo);
        }
        return out;
    }

    /** 多选归一：抽字母、大写、排序，"CA"和"AC"算一样 */
    private String normMulti(String s) {
        char[] cs = s.toUpperCase().replaceAll("[^A-Z]", "").toCharArray();
        java.util.Arrays.sort(cs);
        return new String(cs);
    }

    /**
     * 薄弱点抽题：统计本人分科目正确率，科目权重 = 100-正确率（越低越优先，没刷过的给中等权重），
     * 再用 Efraimidis-Spirakis 加权随机抽样，一轮里弱科目题目占多数但不独占，保留少量强科目防劝退
     */
    private List<Question> smartPick(String category) {
        QueryWrapper<Question> qw = new QueryWrapper<Question>();
        if (category != null && !category.isBlank()) {
            qw.eq("category", category);
        }
        List<Question> pool = questionMapper.selectList(qw);
        if (pool.isEmpty()) {
            return pool;
        }
        Long me = UserContext.get().getId();
        List<Practice> all = practiceMapper.selectList(
                new QueryWrapper<Practice>().eq("user_id", me));
        // 科目 → [总次数, 正确次数]
        Map<String, long[]> agg = new LinkedHashMap<>();
        if (!all.isEmpty()) {
            Map<Long, String> catOf = questionMapper.selectBatchIds(
                            all.stream().map(Practice::getQuestionId).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(Question::getId,
                            q -> q.getCategory() == null ? "其他" : q.getCategory(), (a, b) -> a));
            for (Practice p : all) {
                long[] a = agg.computeIfAbsent(catOf.getOrDefault(p.getQuestionId(), "其他"), k -> new long[2]);
                a[0]++;
                if (p.getResult() != null && p.getResult() == 1) {
                    a[1]++;
                }
            }
        }
        List<Double> keys = new ArrayList<>(pool.size());
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Question q : pool) {
            long[] a = agg.get(q.getCategory() == null ? "其他" : q.getCategory());
            // 没刷过→中等权重；刷过→正确率越低权重越高，下限5防全对科目永远抽不到
            double weight = a == null ? 60 : Math.max(5, 100 - a[1] * 100.0 / a[0]);
            keys.add(Math.pow(rnd.nextDouble(), 1.0 / weight));
        }
        // key 大的优先，取整体权重分布下的前 limit 个
        Integer[] idx = new Integer[pool.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        java.util.Arrays.sort(idx, (x, y) -> Double.compare(keys.get(y), keys.get(x)));
        List<Question> out = new ArrayList<>();
        for (int i = 0; i < Math.min(idx.length, 50); i++) {
            out.add(pool.get(idx[i]));
        }
        return out;
    }
}
