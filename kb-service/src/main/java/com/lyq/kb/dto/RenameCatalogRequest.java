package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameCatalogRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称最长100字")
    private String title;
}