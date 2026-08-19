package com.lyq.kb.service;

import com.lyq.kb.dto.InterviewEvaluateRequest;
import com.lyq.kb.dto.InterviewEvaluateVO;
import com.lyq.kb.dto.InterviewQuestionVO;
import com.lyq.kb.dto.InterviewReportRequest;
import com.lyq.kb.dto.InterviewStartRequest;
import com.lyq.kb.entity.Interview;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface InterviewService {

    /** 抽题开考：优先简答题 */
    List<InterviewQuestionVO> start(InterviewStartRequest req);

    /** 每轮判分+决定追问 */
    InterviewEvaluateVO evaluate(InterviewEvaluateRequest req);

    /**
     * 流式判分：onDelta回调点评增量；AI完全不可用时onFallback（带参考答案），
     * 否则onDone（pass为null表示格式异常，前端同样走自评）
     */
    void evaluateStream(InterviewEvaluateRequest req, Consumer<String> onDelta,
                        Consumer<InterviewEvaluateVO> onDone, Consumer<InterviewEvaluateVO> onFallback);

    /** 终场总评+落库，返回报告Map(含id) */
    Map<String, Object> report(InterviewReportRequest req);

    /** 流式总评：onDelta回调summary增量，onDone拿到完整报告（已落库，降级时本地兑底） */
    void reportStream(InterviewReportRequest req, Consumer<String> onDelta, Consumer<Map<String, Object>> onDone);

    /** 我的面试记录（不含长文本字段） */
    List<Interview> list();

    /** 回看一场（本人） */
    Interview detail(Long id);
}
