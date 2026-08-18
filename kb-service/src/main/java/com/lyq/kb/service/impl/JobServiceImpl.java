package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.dto.JobCreateRequest;
import com.lyq.kb.entity.Job;
import com.lyq.kb.entity.Question;
import com.lyq.kb.mapper.JobMapper;
import com.lyq.kb.mapper.QuestionMapper;
import com.lyq.kb.service.JobService;
import com.lyq.kb.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final QuestionMapper questionMapper;
    private final AiGrader aiGrader;
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> page(Integer status, String keyword, long page, long size) {
        AuthUtil.requireAdmin();
        QueryWrapper<Job> w = new QueryWrapper<>();
        if (status != null) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(q -> q.like("title", kw).or().like("company", kw));
        }
        w.orderByDesc("create_time");
        Page<Job> p = jobMapper.selectPage(new Page<>(page, size), w);
        Map<String, Object> out = new HashMap<>();
        out.put("records", p.getRecords());
        out.put("total", p.getTotal());
        return out;
    }

    @Override
    public Job detail(Long id) {
        AuthUtil.requireAdmin();
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        return job;
    }

    @Override
    public List<Job> openList() {
        // 公开端点不要求管理员：只给已上架岗位的基础字段，供提交简历时选意向
        return jobMapper.selectList(new QueryWrapper<Job>()
                .select("id", "title", "company", "city")
                .eq("status", 1)
                .orderByDesc("create_time"));
    }

    @Override
    public Job create(JobCreateRequest req) {
        AuthUtil.requireAdmin();
        Job job = new Job();
        copyFromRequest(job, req);
        job.setSource("MANUAL");
        // uk_source(source,source_id)去重：手动录入没有来源站ID，UUID兜底
        job.setSourceId("M" + UUID.randomUUID().toString().replace("-", ""));
        job.setStatus(0);
        jobMapper.insert(job);
        return job;
    }

    @Override
    public Job update(Long id, JobCreateRequest req) {
        AuthUtil.requireAdmin();
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        copyFromRequest(job, req);
        jobMapper.updateById(job);
        return job;
    }

    @Override
    public void remove(Long id) {
        AuthUtil.requireAdmin();
        if (jobMapper.selectById(id) == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        jobMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> parse(Long id) {
        AuthUtil.requireAdmin();
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        if (job.getJdText() == null || job.getJdText().isBlank()) {
            throw new IllegalArgumentException("该岗位没有JD内容，无法解析");
        }
        JsonNode node = aiGrader.parseJobRequirement(job.getTitle(), job.getJdText());
        if (node == null) {
            // AI是插件不是依赖：不可用时给明确提示，前端弹出即可
            throw new IllegalArgumentException("AI服务暂不可用，请稍后再试");
        }
        String json = node.toString();
        job.setRequireJson(json);
        jobMapper.updateById(job);
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    @Override
    public void audit(Long id, boolean ok) {
        AuthUtil.requireAdmin();
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        // 允许反复上下架：驳回后修正可重新通过，上架后也能下架
        job.setStatus(ok ? 1 : 2);
        jobMapper.updateById(job);
    }

    @Override
    public int approveCrawled() {
        AuthUtil.requireAdmin();
        // 爬虫入库的岗位字段齐全，批量直接上架，供推荐简历时选用
        return jobMapper.update(null, new UpdateWrapper<Job>()
                .eq("source", "BOSS")
                .eq("status", 0)
                .set("status", 1));
    }

    @Override
    public List<String> recommend(Long id, boolean save) {
        // 登录即可：用户端推荐岗位详情里也要按岗位出面试题，不限管理员
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        if (job.getRequireJson() == null || job.getRequireJson().isBlank()) {
            throw new IllegalArgumentException("请先对该岗位执行AI解析，再按解析结果出题");
        }
        // RAG接地：知识库相关块+题库相似题注入prompt，题目贴合站内知识且不撞题；
        // 检索出任何岔子都退回纯生成，不影响主链路
        String material = buildGrounding(job);
        List<String> questions;
        List<Map<String, String>> rich = null;
        if (save) {
            // save=true是管理员在岗位管理里出题：用带答案的生成，顺便落题库；
            // 用户端生成练习用不传，不写库
            AuthUtil.requireAdmin();
            rich = aiGrader.recommendQuestionsRich(job.getTitle(), job.getRequireJson(), material);
            if (rich == null) {
                throw new IllegalArgumentException("AI服务暂不可用，请稍后再试");
            }
            questions = rich.stream().map(m -> m.get("stem")).toList();
            saveToQuestionBank(job, rich);
        } else {
            questions = aiGrader.recommendQuestions(job.getTitle(), job.getRequireJson(), material);
            if (questions == null) {
                throw new IllegalArgumentException("AI服务暂不可用，请稍后再试");
            }
        }
        return questions;
    }

    /** 面试题入题库：SHORT简答、分类用岗位名、答案用AI生成的要点；同题干已有的自动跳过防重复点刷库 */
    private void saveToQuestionBank(Job job, List<Map<String, String>> rich) {
        // 分类列varchar(50)，岗位名超长截断防入库报错
        String category = job.getTitle();
        if (category != null && category.length() > 50) {
            category = category.substring(0, 50);
        }
        for (Map<String, String> item : rich) {
            String stem = item.get("stem");
            Long exists = questionMapper.selectCount(new QueryWrapper<Question>()
                    .eq("deleted", 0).eq("stem", stem));
            if (exists != null && exists > 0) {
                continue;
            }
            Question q = new Question();
            q.setCategory(category);
            q.setType("SHORT");
            q.setStem(stem);
            // answer列非空：优先用AI生成的答案要点，模型没给才放占位说明
            String answer = item.get("answer");
            q.setAnswer(answer != null && !answer.isBlank()
                    ? answer
                    : "（面试简答题：结合项目经验和技术理解作答，无标准答案）");
            q.setExplanation("根据岗位「" + job.getTitle() + "」的结构化需求AI生成的面试题");
            questionMapper.insert(q);
        }
    }

    /**
     * 拼接地材料：①知识库全局检索Top5块（带文档标题）②题库里按岗位技能词命中的已有题干。
     * 全部失败返回null，出题退回纯生成
     */
    private String buildGrounding(Job job) {
        try {
            StringBuilder sb = new StringBuilder();
            // 检索query：岗位标题+前几个技能词，向量+关键词混合打分
            String query = job.getTitle();
            List<String> skills = parseSkills(job.getSkillsJson());
            if (!skills.isEmpty()) {
                query += " " + String.join(" ", skills.subList(0, Math.min(4, skills.size())));
            }
            List<String> chunks = ragService.searchGlobal(query, 5);
            if (!chunks.isEmpty()) {
                sb.append("【知识库资料】\n");
                for (String c : chunks) {
                    // 单块截断，防止接地材料把prompt撑爆
                    sb.append(c.length() > 300 ? c.substring(0, 300) + "…" : c).append('\n');
                }
            }
            // 题库相似题：按技能词like题干，取前10条，防出新题和库里撞车
            if (!skills.isEmpty()) {
                QueryWrapper<Question> qw = new QueryWrapper<Question>()
                        .select("stem").eq("deleted", 0);
                qw.and(w -> {
                    for (String s : skills.subList(0, Math.min(3, skills.size()))) {
                        w.or().like("stem", s);
                    }
                });
                qw.last("limit 10");
                List<Question> similar = questionMapper.selectList(qw);
                if (!similar.isEmpty()) {
                    sb.append("【已有题库题目（不要重复）】\n");
                    for (Question q : similar) {
                        sb.append("- ").append(q.getStem()).append('\n');
                    }
                }
            }
            return sb.isEmpty() ? null : sb.toString();
        } catch (Exception e) {
            log.warn("出题接地检索失败，退回纯生成：{}", e.getMessage());
            return null;
        }
    }

    /** 解析skillsJson里的技能词：兼容["a","b"]和[{"name":"a"}]两种存法，坏了返回空列表 */
    private List<String> parseSkills(String skillsJson) {
        List<String> out = new ArrayList<>();
        if (skillsJson == null || skillsJson.isBlank()) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(skillsJson);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String s = n.isTextual() ? n.asText() : n.path("name").asText("");
                    if (!s.isBlank()) {
                        out.add(s.trim());
                    }
                }
            }
        } catch (Exception ignore) {
            // 解析失败当没技能词，接地退成只用标题检索
        }
        return out;
    }

    private void copyFromRequest(Job job, JobCreateRequest req) {
        job.setTitle(req.getTitle());
        job.setCompany(req.getCompany());
        job.setCity(req.getCity());
        job.setSalary(req.getSalary());
        job.setExperience(req.getExperience());
        job.setEducation(req.getEducation());
        job.setSkillsJson(req.getSkillsJson());
        job.setJdText(req.getJdText());
        job.setJobUrl(req.getJobUrl());
    }
}
