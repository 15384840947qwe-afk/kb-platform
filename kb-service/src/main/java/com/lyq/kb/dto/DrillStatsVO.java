package com.lyq.kb.dto;

import lombok.Data;

/** 刷题统计返回体 */
@Data
public class DrillStatsVO {
    private Long total;
    private Long known;
    /** 掌握率百分数 */
    private Long rate;
}
