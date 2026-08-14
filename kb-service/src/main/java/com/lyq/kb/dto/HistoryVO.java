package com.lyq.kb.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 历史列表返回体：带标题，前端直接渲染 */
@Data
public class HistoryVO {
    private Long id;
    private Long docId;
    private Long kbId;
    private String title;
    private LocalDateTime viewTime;
}
