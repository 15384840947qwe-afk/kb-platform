package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.dto.QuestionRequest;
import com.lyq.kb.entity.Question;
import com.lyq.kb.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 题库管理：增删改查全要管理员（Service里也兜了底） */
@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/list")
    public Result<List<Question>> list(@RequestParam(required = false) String category) {
        return Result.ok(questionService.list(category));
    }

    @PostMapping
    public Result<Question> create(@Valid @RequestBody QuestionRequest req) {
        return Result.ok(questionService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @Valid @RequestBody QuestionRequest req) {
        return Result.ok(questionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    /** AI从教材生成练习题：count可选3/6/9（默认3），自动关联回该文档，仅管理员 */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "出题中，请勿重复点击")
    @PostMapping("/generate")
    public Result<List<Question>> generate(@RequestParam Long docId,
                                           @RequestParam(defaultValue = "3") int count) {
        return Result.ok(questionService.generateFromDoc(docId, count));
    }
}
