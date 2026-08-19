package com.lyq.kb.dto;

import lombok.Data;

/**
 * 一键生成/补全简历的入参。
 * useDrill/useInterview：是否把站内刷题、面试数据作为素材线索注入prompt，
 * 只对技术类岗位有意义，其他岗位前端不勾选即可
 */
@Data
public class ResumeGenerateRequest {
    /** 有值=覆盖更新这份简历；无值=新建一份 */
    private Long id;
    private String targetJob;
    /** 现有表单内容（补全场景），AI在它的基礎上补齐而不是重写 */
    private String contentJson;
    private boolean useDrill;
    private boolean useInterview;
    /** 补充说明，如"突出运营数据""强调教学成果" */
    private String note;
}
