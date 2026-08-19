package com.lyq.kb.dto;

import lombok.Data;

/** 保存简历：表单提交什么存什么 */
@Data
public class ResumeSaveRequest {
    private String title;
    private String targetJob;
    private String contentJson;
}
