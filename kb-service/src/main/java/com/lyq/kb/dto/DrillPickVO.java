package com.lyq.kb.dto;

import lombok.Data;

/** 抽题返回体：不带答案和解析，防提前泄露；判分走check接口 */
@Data
public class DrillPickVO {
    private Long id;
    private String type;
    private String stem;
    /** 选项JSON数组字符串，前端parse后渲染 */
    private String options;
}
