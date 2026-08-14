package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "名称最长100字")
    private String name;

    @Size(max = 500, message = "简介最长500字")
    private String description;
}