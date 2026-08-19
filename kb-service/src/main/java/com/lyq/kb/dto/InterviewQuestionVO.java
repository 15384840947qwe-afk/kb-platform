package com.lyq.kb.dto;

import lombok.Data;

/** 面试题目返回体：不带参考答案，防提前看 */
@Data
public class InterviewQuestionVO {
    private Long id;
    private String stem;
    private Long relatedDocId;
}
