package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 刷题记录提交体 */
@Data
public class DrillRecordRequest {

    @NotNull(message = "题目id不能为空")
    private Long questionId;

    /** 1对/会 0错/不会 */
    @NotNull(message = "结果不能为空")
    private Integer result;
}
