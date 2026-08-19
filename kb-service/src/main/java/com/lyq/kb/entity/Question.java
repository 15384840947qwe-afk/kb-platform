package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 固定题库：结构化题目，和文库(文档)分离 */
@Data
@TableName("t_question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 科目：Java基础/MySQL/Redis... */
    private String category;
    /** SINGLE单选 MULTI多选 FILL填空 SHORT简答 */
    private String type;
    /** 题干 */
    private String stem;
    /** 选项JSON数组，选择题才有 */
    private String options;
    /** 正确答案：单选"B" 多选"AC" 填空文本 简答参考答案 */
    private String answer;
    /** 解析 */
    private String explanation;
    /** 关联教材文档id，可空 */
    private Long relatedDocId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
