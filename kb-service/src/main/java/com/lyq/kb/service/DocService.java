package com.lyq.kb.service;

import com.lyq.kb.dto.CreateDocRequest;
import com.lyq.kb.dto.DocAskRequest;
import com.lyq.kb.dto.SaveDocRequest;
import com.lyq.kb.entity.Doc;

import java.util.List;
import java.util.function.Consumer;

public interface DocService {

    Doc create(CreateDocRequest req);

    Doc detail(Long id);

    Doc save(Long id, SaveDocRequest req);

    /** 草稿/被驳回的文档提交审核 */
    void submit(Long id);

    void delete(Long id);
    /** 标题模糊搜索，只返回id/title/kbId三列 */
    List<Doc> search(String keyword);

    /**
     * 文档问答（RAG）：关键词检索文档片段后交给AI；onDelta回调答案增量，
     * onDone回调完整答案；AI不可用时流式给出固定提示，不报错
     */
    void ask(Long id, DocAskRequest req, Consumer<String> onDelta, Consumer<String> onDone);

    /** 管理员一键重建全部已审核文档的向量索引（存量回填用），后台异步执行，仅管理员 */
    String reindexAll();
}