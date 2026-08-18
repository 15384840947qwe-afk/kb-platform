package com.lyq.kb.service;

import com.lyq.kb.dto.ResumeGenerateRequest;
import com.lyq.kb.dto.ResumeImportVO;
import com.lyq.kb.dto.ResumeJdRequest;
import com.lyq.kb.dto.ResumeSaveRequest;
import com.lyq.kb.entity.Job;
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

    // ===== 提交给管理员 / 管理员审阅 =====

    /** 提交给管理员审阅：重算公共字段，可附意向岗位 */
    void submit(Long id, Long jobId);

    /** 撤回提交：仅待审阅状态可撤 */
    void withdraw(Long id);

    /** 管理员：简历分页，可按提交状态/学历/姓名目标岗位关键词筛选 */
    Map<String, Object> adminPage(Integer submitStatus, String education, String keyword, long page, long size);

    /** 管理员：简历详情（含contentJson全文） */
    Resume adminDetail(Long id);

    /** 管理员：给简历推荐岗位（可多次追加，重复推同一个自动忽略） */
    void assign(Long id, Long jobId);

    /** 管理员：撤销某个已推荐的岗位；全部撤完回到待审阅 */
    void unassign(Long id, Long jobId);

    /** 管理员：查某简历已推荐的岗位完整列表 */
    List<Job> adminRecommendedJobs(Long id);

    /** 用户：查自己简历被推荐的岗位完整列表 */
    List<Job> recommendedJobs(Long id);

    /** 管理员：退回简历并附理由 */
    void sendBack(Long id, String remark);

    /** 管理员：统计总览（提交/推荐数、学历城市分布、AI均分） */
    Map<String, Object> adminStats();
}
