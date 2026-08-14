package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 刷题记录：每人每题一行，错题本和统计的原料 */
@Data
@TableName("t_practice")
public class Practice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    /** 1对/会 0错/不会 */
    private Integer result;
    private LocalDateTime createTime;
}
