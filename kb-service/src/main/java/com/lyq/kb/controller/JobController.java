package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.dto.JobCreateRequest;
import com.lyq.kb.entity.Job;
import com.lyq.kb.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 岗位管理（管理员）：爬虫入库 + 手动录入，统一走 AI解析/审核/推荐 流程 */
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /** 分页列表：status空=全部，keyword搜岗位名或公司名 */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(jobService.page(status, keyword, page, size));
    }

    /** 已上架岗位列表（公开）：用户提交简历选意向岗位用 */
    @GetMapping("/open")
    public Result<List<Job>> open() {
        return Result.ok(jobService.openList());
    }

    @GetMapping("/{id}")
    public Result<Job> detail(@PathVariable Long id) {
        return Result.ok(jobService.detail(id));
    }

    /** 手动录入 */
    @PostMapping
    public Result<Job> create(@Valid @RequestBody JobCreateRequest req) {
        return Result.ok(jobService.create(req));
    }

    /** 编辑（审核前修正爬虫抓错的字段） */
    @PutMapping("/{id}")
    public Result<Job> update(@PathVariable Long id, @Valid @RequestBody JobCreateRequest req) {
        return Result.ok(jobService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        jobService.remove(id);
        return Result.ok();
    }

    /** AI解析JD为结构化需求，结果落require_json */
    @RateLimit(timeWindow = 60, maxCount = 10, message = "解析太快了，请稍后再试")
    @PostMapping("/{id}/parse")
    public Result<Map<String, Object>> parse(@PathVariable Long id) {
        return Result.ok(jobService.parse(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        jobService.audit(id, true);
        return Result.ok();
    }

    /** 一键上架所有爬虫抓取的待审岗位，返回上架数量 */
    @PostMapping("/approve-crawled")
    public Result<Integer> approveCrawled() {
        return Result.ok(jobService.approveCrawled());
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        jobService.audit(id, false);
        return Result.ok();
    }

    /** AI按结构化需求出面试简答题；save=true时管理员出题顺便入题库 */
    @RateLimit(timeWindow = 60, maxCount = 6, message = "出题太快了，请稍后再试")
    @PostMapping("/{id}/recommend")
    public Result<List<String>> recommend(@PathVariable Long id,
                                          @RequestParam(defaultValue = "false") boolean save) {
        return Result.ok(jobService.recommend(id, save));
    }
}
