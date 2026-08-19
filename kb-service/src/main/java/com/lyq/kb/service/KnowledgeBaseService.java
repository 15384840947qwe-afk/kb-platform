package com.lyq.kb.service;

import com.lyq.kb.dto.CreateBaseRequest;
import com.lyq.kb.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {
    KnowledgeBase create(CreateBaseRequest req);
    List<KnowledgeBase> list();
    KnowledgeBase detail(Long id);
    void delete(Long id);
}