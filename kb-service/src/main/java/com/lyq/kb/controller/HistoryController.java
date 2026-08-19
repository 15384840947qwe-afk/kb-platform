package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /** 分页取当前用户浏览历史：{total, list} */
    @GetMapping
    public Result<Map<String, Object>> recent(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(historyService.recent(page, size));
    }

    /** 删一条自己的历史 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        historyService.deleteOne(id);
        return Result.ok();
    }

    /** 清空自己的全部历史 */
    @DeleteMapping
    public Result<Void> clear() {
        historyService.clear();
        return Result.ok();
    }
}
