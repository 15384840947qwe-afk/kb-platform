package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.HistoryVO;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.History;
import com.lyq.kb.mapper.DocMapper;
import com.lyq.kb.mapper.HistoryMapper;
import com.lyq.kb.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final HistoryMapper historyMapper;
    private final DocMapper docMapper;

    @Override
    public void recordView(Long docId, Long kbId) {
        historyMapper.upsertView(UserContext.get().getId(), docId, kbId);
    }

    @Override
    public Map<String, Object> recent(int page, int size) {
        Long me = UserContext.get().getId();
        long total = historyMapper.selectCount(new QueryWrapper<History>().eq("user_id", me));
        List<History> list = historyMapper.selectPage(me, (page - 1) * size, size);
        List<HistoryVO> out = new ArrayList<>();
        if (!list.isEmpty()) {
            // 一次性批量取标题，不逐条查（避免N+1）
            Map<Long, Doc> docs = docMapper
                    .selectBatchIds(list.stream().map(History::getDocId).collect(Collectors.toList()))
                    .stream()
                    .collect(Collectors.toMap(Doc::getId, d -> d));
            for (History h : list) {
                Doc d = docs.get(h.getDocId());
                if (d == null) {
                    // 文档已被删除：历史里不展示死链
                    continue;
                }
                HistoryVO vo = new HistoryVO();
                vo.setId(h.getId());
                vo.setDocId(h.getDocId());
                vo.setKbId(h.getKbId());
                vo.setTitle(d.getTitle());
                vo.setViewTime(h.getViewTime());
                out.add(vo);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", out);
        return result;
    }

    @Override
    public void deleteOne(Long id) {
        // 只删自己的：user_id条件兜底，删别人的id直接0行
        historyMapper.delete(new QueryWrapper<History>()
                .eq("id", id)
                .eq("user_id", UserContext.get().getId()));
    }

    @Override
    public void clear() {
        historyMapper.delete(new QueryWrapper<History>()
                .eq("user_id", UserContext.get().getId()));
    }
}
