package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 每轮作答提交 */
@Data
public class InterviewEvaluateRequest {

    @NotNull
    private Long questionId;

    /** 本轮回答 */
    private String answer;

    /** 本题已追问次数 */
    private Integer followUsed = 0;

    /** 本题最多追问次数 */
    private Integer maxFollow = 1;

    /** 本题对话线程（主问题/回答/历次追问与回答，按时间序），给AI上下文避免重复追问 */
    private List<HistoryItem> history;

    @Data
    public static class HistoryItem {
        /** interviewer考官 / user考生 */
        private String role;
        private String text;
    }
}
