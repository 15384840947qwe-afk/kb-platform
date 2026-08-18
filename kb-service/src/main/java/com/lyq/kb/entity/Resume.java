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
    /** 以下为content_json拍平的公共字段，供管理员SQL筛选/统计 */
    private String name;
    private String phone;
    private String city;
    /** 最高学历：博士/硕士/本科/大专/其他 */
    private String education;
    /** 工作年限，按工作经历最早开始年份估算 */
    private Integer workYears;
    /** 技能摘要，分类:项目 顿号拼接 */
    private String skills;
    /** 最近一次AI分析得分 */
    private Integer aiScore;
    /** 0未提交 1已提交待审阅 2已退回 */
    private Integer submitStatus;
    private LocalDateTime submitTime;
    /** 提交时意向岗位(t_job.id) */
    private Long appliedJobId;
    /** 管理员推荐岗位(t_job.id) */
    private Long assignedJobId;
    /** 管理员退回理由等备注 */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
