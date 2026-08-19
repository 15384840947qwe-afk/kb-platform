package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.common.Sse;
import com.lyq.kb.dto.InterviewEvaluateRequest;
import com.lyq.kb.dto.InterviewEvaluateVO;
import com.lyq.kb.dto.InterviewQuestionVO;
import com.lyq.kb.dto.InterviewReportRequest;
import com.lyq.kb.dto.InterviewStartRequest;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /** 抽题开考 */
    @PostMapping("/start")
    public Result<List<InterviewQuestionVO>> start(@Valid @RequestBody InterviewStartRequest req) {
        return Result.ok(interviewService.start(req));
    }

    /** 每轮判分+追问 */
    @RateLimit(timeWindow = 60, maxCount = 8, message = "面试评分太频繁，请稍后再试")
    @PostMapping("/evaluate")
    public Result<InterviewEvaluateVO> evaluate(@Valid @RequestBody InterviewEvaluateRequest req) {
        return Result.ok(interviewService.evaluate(req));
    }

    /**
     * 流式判分：delta=点评增量 → done=完整结果；
     * AI不可用发fallback（带参考答案，前端降级自评）
     */
    @RateLimit(timeWindow = 60, maxCount = 8, message = "面试评分太频繁，请稍后再试")
    @PostMapping("/evaluate-stream")
    public SseEmitter evaluateStream(@Valid @RequestBody InterviewEvaluateRequest req) {
        return Sse.run(emitter -> interviewService.evaluateStream(req,
                d -> Sse.send(emitter, "delta", d),
                vo -> Sse.send(emitter, "done", vo),
                vo -> Sse.send(emitter, "fallback", vo)));
    }

    /** 终场总评+落库 */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "总评生成中，请勿重复提交")
    @PostMapping("/report")
    public Result<Map<String, Object>> report(@Valid @RequestBody InterviewReportRequest req) {
        return Result.ok(interviewService.report(req));
    }

    /** 流式总评：delta=summary增量 → done=完整报告（已落库）；降级时直接done本地兑底 */
    @RateLimit(timeWindow = 60, maxCount = 3, message = "总评生成中，请勿重复提交")
    @PostMapping("/report-stream")
    public SseEmitter reportStream(@Valid @RequestBody InterviewReportRequest req) {
        return Sse.run(emitter -> interviewService.reportStream(req,
                d -> Sse.send(emitter, "delta", d),
                report -> Sse.send(emitter, "done", report)));
    }

    /** 我的面试记录 */
    @GetMapping("/list")
    public Result<List<Interview>> list() {
        return Result.ok(interviewService.list());
    }

    /** 回看一场 */
    @GetMapping("/{id}")
    public Result<Interview> detail(@PathVariable Long id) {
        return Result.ok(interviewService.detail(id));
    }
}
