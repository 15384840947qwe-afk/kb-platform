package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 简历：每人若干份，contentJson按jsonresume风格分段存储 */
@Data
@TableName("t_resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String targetJob;
    /** 导入原文或生成的Markdown全文 */
    private String rawText;
    private String contentJson;
    private String analysisJson;
    private String fileName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
