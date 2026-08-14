package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.common.Sse;
import com.lyq.kb.dto.ResumeGenerateRequest;
import com.lyq.kb.dto.ResumeImportVO;
import com.lyq.kb.dto.ResumeJdRequest;
import com.lyq.kb.dto.ResumeSaveRequest;
import com.lyq.kb.entity.Resume;
import com.lyq.kb.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {

    /** 简历流式端点的连接超时：要盖住AI完整输出耗时，分析60s/生成90s都留够余量 */
    private static final long STREAM_TIMEOUT_MS = 150_000L;

    private final ResumeService resumeService;

    /** 导入已有简历：pdf/txt/md，AI提取结构化字段 */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "简历导入太频繁，请稍后再试")
    @PostMapping("/import")
    public Result<ResumeImportVO> importFile(@RequestParam("file") MultipartFile file) {
        return Result.ok(resumeService.importFile(file));
    }

    /** 空模板新建 */
    @PostMapping
    public Result<Resume> create(@RequestBody(required = false) ResumeSaveRequest req) {
        return Result.ok(resumeService.create(req));
    }

    /** 我的简历列表（不含正文大字段） */
    @GetMapping("/list")
    public Result<List<Resume>> list() {
        return Result.ok(resumeService.list());
    }

    /** 站内素材：刷题科目分布+面试记录，供生成时勾选注入 */
    @GetMapping("/materials")
    public Result<Map<String, Object>> materials() {
        return Result.ok(resumeService.materials());
    }

    /**
     * 流式分析：delta=点评增量 → done=结构化评审（已落库）；
     * AI不可用发fallback（本地规则检查清单）
     */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "AI分析中，请勿重复点击")
    @PostMapping("/{id}/analyze-stream")
    public SseEmitter analyzeStream(@PathVariable Long id) {
        return Sse.run(emitter -> resumeService.analyzeStream(id,
                d -> Sse.send(emitter, "delta", d),
                a -> Sse.send(emitter, "done", a),
                a -> Sse.send(emitter, "fallback", a)), STREAM_TIMEOUT_MS);
    }

    /**
     * 流式生成/补全：delta=Markdown增量 → done={id,markdown,contentJson}（已落库）；
     * AI不可用发fallback提示手动填写
     */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "AI生成中，请勿重复点击")
    @PostMapping("/generate-stream")
    public SseEmitter generateStream(@RequestBody ResumeGenerateRequest req) {
        return Sse.run(emitter -> resumeService.generateStream(req,
                d -> Sse.send(emitter, "delta", d),
                r -> Sse.send(emitter, "done", r),
                r -> Sse.send(emitter, "fallback", r)), STREAM_TIMEOUT_MS);
    }

    /**
     * JD匹配流式：粘贴目标岗位JD，delta=分析增量 → done={score,matched,missing,suggestions}；
     * 即时评估不落库，AI不可用发fallback
     */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "匹配太频繁，请稍后再试")
    @PostMapping("/{id}/jd-stream")
    public SseEmitter jdStream(@PathVariable Long id, @RequestBody ResumeJdRequest req) {
        return Sse.run(emitter -> resumeService.jdStream(id, req,
                d -> Sse.send(emitter, "delta", d),
                r -> Sse.send(emitter, "done", r),
                r -> Sse.send(emitter, "fallback", r)), STREAM_TIMEOUT_MS);
    }

    /** 导出Markdown纯文本：前端复制走或浏览器打印成PDF */
    @GetMapping(value = "/{id}/export.md", produces = "text/markdown;charset=UTF-8")
    public String exportMarkdown(@PathVariable Long id) {
        return resumeService.exportMarkdown(id);
    }

    @GetMapping("/{id}")
    public Result<Resume> detail(@PathVariable Long id) {
        return Result.ok(resumeService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ResumeSaveRequest req) {
        resumeService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return Result.ok();
    }
}
