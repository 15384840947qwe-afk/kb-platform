package com.lyq.kb.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 我的提交列表返回体：知识库和文档统一形状 */
@Data
public class SubmissionVO {
    /** "知识库"或"文档"或"文件夹" */
    private String type;
    private Long id;
    private Long kbId;
    private String title;
    /** 0待审核 1通过 2驳回 */
    private Integer status;
    private LocalDateTime createTime;
}
