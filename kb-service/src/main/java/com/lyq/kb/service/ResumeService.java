package com.lyq.kb.service;

import com.lyq.kb.dto.ResumeGenerateRequest;
import com.lyq.kb.dto.ResumeImportVO;
import com.lyq.kb.dto.ResumeJdRequest;
import com.lyq.kb.dto.ResumeSaveRequest;
import com.lyq.kb.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ResumeService {

    /** 导入pdf/txt/md：抽文本→AI提取结构化JSON，AI失败aiParsed=false让用户手填 */
    ResumeImportVO importFile(MultipartFile file);

    /** 空模板新建（contentJson也可由前端带入已有表单） */
    Resume create(ResumeSaveRequest req);

    /** 我的简历列表：不带raw_text/content_json大字段 */
    List<Resume> list();

    Resume detail(Long id);

    void update(Long id, ResumeSaveRequest req);

    void delete(Long id);

    /** 流式分析：delta=点评增量 → done=结构化评审(已落库)；AI不可用fallback带本地规则清单 */
    void analyzeStream(Long id, Consumer<String> onDelta,
                       Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback);

    /** 流式生成：delta=Markdown增量 → done={id,markdown,contentJson}(已落库)；AI不可用fallback */
    void generateStream(ResumeGenerateRequest req, Consumer<String> onDelta,
                        Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback);

    /** JD匹配流式：delta=分析增量 → done={score,matched,missing,suggestions}；不落库，即时评估 */
    void jdStream(Long id, ResumeJdRequest req, Consumer<String> onDelta,
                  Consumer<Map<String, Object>> onDone, Consumer<Map<String, Object>> onFallback);

    /** 站内素材：刷题科目分布+面试记录，供技术类岗位生成时勾选注入 */
    Map<String, Object> materials();

    /** contentJson渲染成Markdown文本，前端复制/打印成PDF */
    String exportMarkdown(Long id);
}
