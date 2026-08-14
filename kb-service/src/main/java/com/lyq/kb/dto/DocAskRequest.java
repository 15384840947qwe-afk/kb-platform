package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 文档问答请求：针对某篇文档提问，可带最近几轮对话 */
@Data
public class DocAskRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    /** 最近问答历史，后端最多取2轮拼进提示词 */
    private List<Item> history;

    @Data
    public static class Item {
        /** user提问 / assistant回答 */
        private String role;
        private String text;
    }
}
