package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.dto.CreateBaseRequest;
import com.lyq.kb.entity.KnowledgeBase;
import com.lyq.kb.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public Result<KnowledgeBase> create(@Valid @RequestBody CreateBaseRequest req) {
        return Result.ok(knowledgeBaseService.create(req));
    }

    @GetMapping("/list")
    public Result<List<KnowledgeBase>> list() {
        return Result.ok(knowledgeBaseService.list());
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBase> detail(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.ok();
    }
}