package com.lyq.kb.dto;

import lombok.Data;

/** 错题本条目：最近一次仍答错的题；答对一次即毕业 */
@Data
public class WrongVO {
    private Long id;
    private String type;
    private String stem;
    private String options;
    private String answer;
    private String explanation;
    private Long relatedDocId;
    /** 累计答错次数 */
    private Long wrongCount;
}
