package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 答题提交体：answer为字符串，多选传"AC"这种连写字母 */
@Data
public class DrillCheckRequest {

    @NotNull(message = "题目id不能为空")
    private Long questionId;

    private String answer;
}
