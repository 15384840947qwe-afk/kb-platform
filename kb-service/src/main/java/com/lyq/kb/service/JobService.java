package com.lyq.kb.service;

import com.lyq.kb.dto.JobCreateRequest;
import com.lyq.kb.entity.Job;

import java.util.List;
import java.util.Map;

public interface JobService {

    /** 分页列表：status为空=全部，keyword模糊匹配岗位名/公司名 */
    Map<String, Object> page(Integer status, String keyword, long page, long size);

    /** 已上架岗位列表（公开，仅基础字段）：用户提交简历选意向岗位用 */
    List<Job> openList();

    Job detail(Long id);

    /** 手动录入：source=MANUAL，source_id用UUID兜底唯一键 */
    Job create(JobCreateRequest req);

    /** 编辑岗位信息（审核前修正爬虫抓错的字段） */
    Job update(Long id, JobCreateRequest req);

    void remove(Long id);

    /** AI解析JD → require_json 落库并返回；AI不可用抛业务异常 */
    Map<String, Object> parse(Long id);

    /** 审核：通过上架/驳回下架 */
    void audit(Long id, boolean ok);

    /** 一键上架所有爬虫抓取的待审岗位，返回上架数量 */
    int approveCrawled();

    /** AI按岗位需求出面试简答题；save=true时管理员出题顺便入题库（同题干去重） */
    List<String> recommend(Long id, boolean save);
}
