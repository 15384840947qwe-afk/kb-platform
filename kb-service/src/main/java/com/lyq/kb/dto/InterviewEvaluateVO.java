package com.lyq.kb.dto;

import lombok.Data;

/** 每轮判分返回 */
@Data
public class InterviewEvaluateVO {
    /** true过 false没过 null=AI不可用需前端自评 */
    private Boolean pass;
    /** 本题得分 0-100，null=AI不可用 */
    private Integer score;
    /** 面试官点评 */
    private String comment;
    /** 追问；null=不追问进下一题 */
    private String followUp;
    /** 参考答案：自评兜底时展示（错题本本就可见答案，不算泄露） */
    private String reference;
}
