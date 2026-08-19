package com.lyq.kb.service;

import com.lyq.kb.dto.QuestionRequest;
import com.lyq.kb.entity.Question;

import java.util.List;

public interface QuestionService {

    List<String> categories();

    List<Question> list(String category);

    Question create(QuestionRequest req);

    Question update(Long id, QuestionRequest req);

    void delete(Long id);

    /** AI从教材文档生成练习题（count可选3/6/9），自动关联回该文档，仅管理员 */
    List<Question> generateFromDoc(Long docId, int count);
}
