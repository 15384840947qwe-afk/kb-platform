package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 题库录入/编辑请求体 */
@Data
public class QuestionRequest {

    @NotBlank(message = "科目不能为空")
    @Size(max = 50, message = "科目最长50字")
    private String category;

    /** SINGLE/MULTI/FILL/SHORT */
    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    /** 选项JSON数组字符串，选择题必填 */
    private String options;

    @NotBlank(message = "答案不能为空")
    private String answer;

    private String explanation;

    private Long relatedDocId;
}
