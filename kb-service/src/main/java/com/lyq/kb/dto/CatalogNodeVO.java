package com.lyq.kb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 目录树节点VO：前端拿到直接递归渲染侧边栏 */
@Data
public class CatalogNodeVO {
    private Long id;
    private Long parentId;
    private String title;
    private String nodeType;
    private Long docId;
    private Integer sortOrder;
    /** 子节点列表，默认空列表，前端不用判null */
    private List<CatalogNodeVO> children = new ArrayList<>();
}