package com.lyq.kb.service;

import com.lyq.kb.dto.DrillCheckVO;
import com.lyq.kb.dto.DrillPickVO;
import com.lyq.kb.dto.DrillStatsVO;
import com.lyq.kb.dto.WrongVO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface DrillService {

    /** 抽题：mode=random随机 / wrong错题本 */
    List<DrillPickVO> pick(String category, String mode, int n);

    /** 判分：选择/填空规则判，简答走AI（不可用返回correct=null让前端自评） */
    DrillCheckVO check(Long questionId, String answer);

    /** 流式判分（仅简答）：onDelta回调AI点评增量；AI不可用时onFallback（correct=null） */
    void checkStream(Long questionId, String answer, Consumer<String> onDelta,
                     Consumer<DrillCheckVO> onDone, Consumer<DrillCheckVO> onFallback);

    void record(Long questionId, int result);

    /** 错题本：最近一次仍答错的题，答对即毕业 */
    List<WrongVO> wrongBook();

    DrillStatsVO stats();

    /** 成长看板：perCategory分科目正确率 / recent7近七天趋势 / scores面试分数曲线 */
    Map<String, Object> dashboard();
}
