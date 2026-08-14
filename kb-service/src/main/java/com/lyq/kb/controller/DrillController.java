package com.lyq.kb.controller;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.Result;
import com.lyq.kb.common.Sse;
import com.lyq.kb.dto.DrillCheckRequest;
import com.lyq.kb.dto.DrillCheckVO;
import com.lyq.kb.dto.DrillPickVO;
import com.lyq.kb.dto.DrillRecordRequest;
import com.lyq.kb.dto.DrillStatsVO;
import com.lyq.kb.dto.WrongVO;
import com.lyq.kb.service.DrillService;
import com.lyq.kb.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drill")
@RequiredArgsConstructor
public class DrillController {

    private final DrillService drillService;
    private final QuestionService questionService;

    /** 科目列表：setup页下拉用 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(questionService.categories());
    }

    /** 抽题：category可空(全科目)，mode=random/wrong */
    @GetMapping("/pick")
    public Result<List<DrillPickVO>> pick(@RequestParam(required = false) String category,
                                          @RequestParam(defaultValue = "random") String mode,
                                          @RequestParam(defaultValue = "10") int n) {
        return Result.ok(drillService.pick(category, mode, n));
    }

    /** 判分：返回对错+正确答案+解析(+AI点评) */
    @PostMapping("/check")
    public Result<DrillCheckVO> check(@Valid @RequestBody DrillCheckRequest req) {
        return Result.ok(drillService.check(req.getQuestionId(), req.getAnswer()));
    }

    /** 流式批改（仅简答）：delta=AI点评增量 → done=完整结果；fallback=AI不可用自评 */
    @RateLimit(timeWindow = 60, maxCount = 5, message = "AI批改太频繁，请稍后再试")
    @PostMapping("/check-stream")
    public SseEmitter checkStream(@Valid @RequestBody DrillCheckRequest req) {
        return Sse.run(emitter -> drillService.checkStream(req.getQuestionId(), req.getAnswer(),
                d -> Sse.send(emitter, "delta", d),
                vo -> Sse.send(emitter, "done", vo),
                vo -> Sse.send(emitter, "fallback", vo)));
    }

    /** 记录一次答题结果 */
    @PostMapping("/record")
    public Result<Void> record(@Valid @RequestBody DrillRecordRequest req) {
        drillService.record(req.getQuestionId(), req.getResult());
        return Result.ok();
    }

    /** 我的刷题统计 */
    @GetMapping("/stats")
    public Result<DrillStatsVO> stats() {
        return Result.ok(drillService.stats());
    }

    /** 成长看板：分科目正确率+近七天趋势+面试分数曲线 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(drillService.dashboard());
    }

    /** 错题本：最近一次仍答错的题 */
    @GetMapping("/wrong")
    public Result<List<WrongVO>> wrong() {
        return Result.ok(drillService.wrongBook());
    }
}
