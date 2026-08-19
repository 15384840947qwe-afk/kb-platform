package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 模拟面试记录 */
@Data
@TableName("t_interview")
public class Interview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String category;
    private Long resumeId;
    private Integer score;
    /** JSON简历审核结果 */
    private String resumeReview;
    /** 着装评分0-100 */
    private Integer appearanceScore;
    /** JSON着装评估结果 */
    private String appearanceReview;
    /** JSON全文对话 */
    private String transcript;
    /** JSON总评 */
    private String report;
    private LocalDateTime createTime;
}
