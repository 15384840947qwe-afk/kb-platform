package com.lyq.kb.dto;

import lombok.Data;

/** 着装评估结果 */
@Data
public class AppearanceEvalVO {
    /** 正式度 1-10 */
    private Integer formalityScore;
    /** 总评 */
    private String comment;
    /** 亮点 */
    private String good;
    /** 改进建议 */
    private String improve;
}
