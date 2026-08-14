package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 拖拽排序/移动：把节点id挂到新父节点下 */
@Data
public class MoveCatalogRequest {

    @NotNull(message = "节点id不能为空")
    private Long id;

    @NotNull(message = "目标父节点不能为空")
    private Long parentId;

    /** 新排序值，不传就保持原值 */
    private Integer sortOrder;
}