package com.lyq.kb.dto;

import lombok.Data;

/** 判分返回体 */
@Data
public class DrillCheckVO {
    /** true对 false错 null=简答题AI不可用、需前端自评 */
    private Boolean correct;
    /** 正确答案（简答为参考答案），判分后展示 */
    private String correctAnswer;
    private String explanation;
    /** AI点评，仅简答题且AI可用时有 */
    private String aiComment;
    private Long relatedDocId;
}
