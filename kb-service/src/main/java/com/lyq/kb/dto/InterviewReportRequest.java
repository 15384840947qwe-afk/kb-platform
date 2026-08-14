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

    @Data
    public static class Item {
        private Long questionId;
        private String stem;
        private Boolean pass;
        private String comment;
        private Long relatedDocId;
    }
}
