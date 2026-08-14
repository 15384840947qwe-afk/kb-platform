package com.lyq.kb.service;

import com.lyq.kb.dto.CatalogNodeVO;
import com.lyq.kb.dto.CreateCatalogRequest;
import com.lyq.kb.dto.MoveCatalogRequest;
import com.lyq.kb.entity.Catalog;

import java.util.List;

public interface CatalogService {

    List<CatalogNodeVO> tree(Long kbId);

    Catalog create(CreateCatalogRequest req);

    void rename(Long id, String title);

    void move(MoveCatalogRequest req);

    void deleteFolder(Long id);
}