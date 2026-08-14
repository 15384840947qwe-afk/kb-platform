package com.lyq.kb.service;

import com.lyq.kb.dto.HistoryVO;

import java.util.List;
import java.util.Map;

public interface HistoryService {

    /** 记录当前用户查看某文档 */
    void recordView(Long docId, Long kbId);

    /** 分页取当前用户历史，返回{total, list}；已删的文档不返回 */
    Map<String, Object> recent(int page, int size);

    /** 删一条自己的历史 */
    void deleteOne(Long id);

    /** 清空自己的全部历史 */
    void clear();
}
