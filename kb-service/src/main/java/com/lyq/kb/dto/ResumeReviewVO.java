package com.lyq.kb.dto;

import lombok.Data;
import java.util.List;

/** 简历审核结果 */
@Data
public class ResumeReviewVO {
    /** 亮点 */
    private List<String> highlights;
    /** 疑虑/风险点 */
    private List<String> concerns;
    /** 面试考察方向建议 */
    private List<String> suggestions;
}
