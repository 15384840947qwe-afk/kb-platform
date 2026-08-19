package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 每轮作答提交 */
@Data
public class InterviewEvaluateRequest {

    /** 题库题目ID，简历出题模式可为空 */
    private Long questionId;

    /** 本轮回答 */
    private String answer;

    /** 本题已追问次数 */
    private Integer followUsed = 0;

    /** 本题最多追问次数 */
    private Integer maxFollow = 1;

    /** 本题对话线程（主问题/回答/历次追问与回答，按时间序），给AI上下文避免重复追问 */
    private List<HistoryItem> history;

    /** 题目题干（简历出题模式必传，题库模式从题库取） */
    private String stem;

    /** 参考答案（简历出题模式必传，题库模式从题库取） */
    private String referenceAnswer;

    /** 关联简历ID（简历出题模式传入，后端自动取简历内容作为评估上下文） */
    private Long resumeId;

    /** 简历上下文（备选：前端直接传简历摘要，优先级低于resumeId） */
    private String resumeContext;

    @Data
    public static class HistoryItem {
        /** interviewer考官 / user考生 */
        private String role;
        private String text;
    }
}
