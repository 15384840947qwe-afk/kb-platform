package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.common.Sse;
import com.lyq.kb.dto.CreateDocRequest;
import com.lyq.kb.dto.DocAskRequest;
import com.lyq.kb.dto.SaveDocRequest;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.service.DocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
public class DocController {

    private final DocService docService;

    @PostMapping
    public Result<Doc> create(@Valid @RequestBody CreateDocRequest req) {
        return Result.ok(docService.create(req));
    }

    @GetMapping("/{id}")
    public Result<Doc> detail(@PathVariable Long id) {
        return Result.ok(docService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<Doc> save(@PathVariable Long id, @Valid @RequestBody SaveDocRequest req) {
        return Result.ok(docService.save(id, req));
    }

    /** 成员写完草稿后点"提交审核" */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        docService.submit(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        docService.delete(id);
        return Result.ok();
    }
    /** 顶栏搜索：标题模糊匹配 */
    @GetMapping("/search")
    public Result<List<Doc>> search(@RequestParam String keyword) {
        return Result.ok(docService.search(keyword));
    }

    /** 文档问答（SSE）：delta=答案增量 → done=完整答案；AI不可用时流式给固定提示 */
    @RateLimit(timeWindow = 60, maxCount = 10, message = "提问太快了，稍等一下再问吧")
    @PostMapping("/{id}/ask")
    public SseEmitter ask(@PathVariable Long id, @Valid @RequestBody DocAskRequest req) {
        return Sse.run(emitter -> docService.ask(id, req,
                d -> Sse.send(emitter, "delta", d),
                answer -> Sse.send(emitter, "done", java.util.Map.of("answer", answer))));
    }

    /** 一键重建全部已审核文档的向量索引（存量回填），后台异步执行，仅管理员 */
    @RateLimit(timeWindow = 60, maxCount = 1, message = "索引重建中，请勿重复触发")
    @PostMapping("/reindex")
    public Result<String> reindex() {
        return Result.ok(docService.reindexAll());
    }
}