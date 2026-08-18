package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 简历推荐岗位关系：一份简历可被推荐多个岗位，支持追加 */
@Data
@TableName("t_resume_job")
public class ResumeJobRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Long jobId;
    private LocalDateTime createTime;
}
