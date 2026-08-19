package com.lyq.kb.dto;

import lombok.Data;

/** 面试题目返回体：不带参考答案，防提前看 */
@Data
public class InterviewQuestionVO {
    private Long id;
    private String stem;
    private Long relatedDocId;
    /** AI根据简历生成的参考答案，仅简历出题模式有值，评估时使用 */
    private String referenceAnswer;
}
