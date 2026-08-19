package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 岗位：爬虫抓取（BOSS）+ 管理员手动录入（MANUAL），统一走审核流 */
@Data
@TableName("t_job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 数据来源：BOSS=Boss直聘爬取 MANUAL=管理员手动录入 */
    private String source;
    /** 来源站唯一标识，配合source去重（uk_source） */
    private String sourceId;
    private String title;
    private String company;
    private String city;
    /** 薪资原文，如 15-25K·14薪 */
    private String salary;
    /** 经验要求原文，如 3-5年 */
    private String experience;
    /** 学历要求原文，如 本科 */
    private String education;
    /** 技能标签JSON数组：来源站标签或AI解析结果 */
    private String skillsJson;
    /** JD职责描述原文 */
    private String jdText;
    /** AI结构化需求：{skills:[],minExpYears,education,keywords:[]} */
    private String requireJson;
    private String jobUrl;
    /** 0待审核 1已上架 2已驳回/下架 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
