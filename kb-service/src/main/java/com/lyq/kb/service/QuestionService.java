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

    /** AI从教材文档生成3道题（单选/填空/简答），自动关联回该文档 */
    List<Question> generateFromDoc(Long docId);
}
