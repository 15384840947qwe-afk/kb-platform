package com.lyq.kb.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 开始一场面试 */
@Data
public class InterviewStartRequest {

    /** 科目，空=混合 */
    private String category;

    @Min(1) @Max(10)
    private Integer count = 3;

    /** 每题最多追问次数 0-2 */
    @Min(0) @Max(2)
    private Integer maxFollow = 1;
}
