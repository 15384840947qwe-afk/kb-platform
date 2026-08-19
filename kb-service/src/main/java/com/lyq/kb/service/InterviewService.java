package com.lyq.kb.service;

import com.lyq.kb.dto.AppearanceEvalRequest;
import com.lyq.kb.dto.AppearanceEvalVO;
import com.lyq.kb.dto.InterviewEvaluateRequest;
import com.lyq.kb.dto.InterviewEvaluateVO;
import com.lyq.kb.dto.InterviewQuestionVO;
import com.lyq.kb.dto.InterviewReportRequest;
import com.lyq.kb.dto.InterviewStartRequest;
import com.lyq.kb.dto.ResumeReviewVO;
import com.lyq.kb.entity.Interview;
import com.lyq.kb.entity.Resume;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface InterviewService {

    /** 抽题开考：有简历时按简历出题，否则从题库抽 */
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

    /** 流式简历审核：onDelta回调审核文本增量，onDone拿到结构化结果 */
    void reviewResumeStream(Long resumeId, Consumer<String> onDelta, Consumer<ResumeReviewVO> onDone);

    /** 着装评估 */
    AppearanceEvalVO evaluateAppearance(AppearanceEvalRequest req);

    /** 当前用户的简历列表（轻量字段） */
    List<Resume> myResumes();

    /** 我的面试记录（不含长文本字段） */
    List<Interview> list();

    /** 回看一场（本人） */
    Interview detail(Long id);
}
