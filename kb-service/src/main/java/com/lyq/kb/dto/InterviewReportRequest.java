package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 终场报告请求：客户端把整场对话和逐题结果交上来 */
@Data
public class InterviewReportRequest {

    @NotNull
    private String category;

    /** JSON全文对话，原样存档 */
    private String transcript;

    /** 逐题结果 */
    private List<Item> items;

    /** 简历审核结果文本（有简历时传入，让总评综合考虑） */
    private String resumeReview;

    /** 着装评估文本（有评估时传入，让总评综合考虑） */
    private String appearanceEval;

    @Data
    public static class Item {
        private Long questionId;
        private String stem;
        private Boolean pass;
        /** 本题得分 0-100 */
        private Integer score;
        private String comment;
        private Long relatedDocId;
    }
}
