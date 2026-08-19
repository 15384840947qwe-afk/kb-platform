package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCatalogRequest {

    @NotNull(message = "知识库id不能为空")
    private Long kbId;

    /** 父节点id，0=挂在根下 */
    private Long parentId = 0L;

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称最长100字")
    private String title;
}