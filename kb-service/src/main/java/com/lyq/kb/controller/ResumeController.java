package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.common.Sse;
import com.lyq.kb.dto.ResumeGenerateRequest;
import com.lyq.kb.dto.ResumeImportVO;
import com.lyq.kb.dto.ResumeJdRequest;
import com.lyq.kb.dto.ResumeSaveRequest;
import com.lyq.kb.entity.Job;
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

    // ===== 提交给管理员 / 管理员审阅 =====

    /** 提交给管理员审阅，body可带意向岗位 {jobId} */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody(required = false) Map<String, Long> body) {
        resumeService.submit(id, body == null ? null : body.get("jobId"));
        return Result.ok();
    }

    /** 撤回提交：仅待审阅状态可撤 */
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        resumeService.withdraw(id);
        return Result.ok();
    }

    /** 管理员：简历分页，可按提交状态/学历/姓名目标岗位关键词筛 */
    @GetMapping("/admin/page")
    public Result<Map<String, Object>> adminPage(@RequestParam(required = false) Integer submitStatus,
                                                 @RequestParam(required = false) String education,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(resumeService.adminPage(submitStatus, education, keyword, page, size));
    }

    /** 管理员：统计总览（提交/推荐数、学历城市分布、AI均分） */
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> adminStats() {
        return Result.ok(resumeService.adminStats());
    }

    /** 管理员：简历详情（含contentJson全文） */
    @GetMapping("/admin/{id}")
    public Result<Resume> adminDetail(@PathVariable Long id) {
        return Result.ok(resumeService.adminDetail(id));
    }

    /** 管理员：推荐岗位 {jobId}，可多次追加 */
    @PostMapping("/admin/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        resumeService.assign(id, body == null ? null : body.get("jobId"));
        return Result.ok();
    }

    /** 管理员：撤销某个已推荐岗位 {jobId}，全部撤完回待审阅 */
    @PostMapping("/admin/{id}/unassign")
    public Result<Void> unassign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        resumeService.unassign(id, body == null ? null : body.get("jobId"));
        return Result.ok();
    }

    /** 管理员：某简历已推荐的岗位完整列表 */
    @GetMapping("/admin/{id}/jobs")
    public Result<List<Job>> adminRecommendedJobs(@PathVariable Long id) {
        return Result.ok(resumeService.adminRecommendedJobs(id));
    }

    /** 用户：我的简历被推荐的岗位完整列表（仅本人） */
    @GetMapping("/{id}/recommended")
    public Result<List<Job>> recommendedJobs(@PathVariable Long id) {
        return Result.ok(resumeService.recommendedJobs(id));
    }

    /** 管理员：退回并附理由 {remark} */
    @PostMapping("/admin/{id}/return")
    public Result<Void> sendBack(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        resumeService.sendBack(id, body == null ? null : body.get("remark"));
        return Result.ok();
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
