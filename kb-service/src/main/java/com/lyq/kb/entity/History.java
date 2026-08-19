package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 浏览历史：每人每文档一条，重看只刷新时间 */
@Data
@TableName("t_history")
public class History {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long docId;
    private Long kbId;
    private LocalDateTime viewTime;
}
