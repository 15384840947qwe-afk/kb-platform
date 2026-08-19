package com.lyq.kb.dto;

import lombok.Data;

/** 导入简历的返回：aiParsed=false表示AI没提取成功，前端提示用户手填表单 */
@Data
public class ResumeImportVO {
    private Long id;
    private String title;
    private String targetJob;
    private String fileName;
    /** 提取出的原文（前端可展开对照核对） */
    private String rawText;
    private String contentJson;
    private boolean aiParsed;
}
