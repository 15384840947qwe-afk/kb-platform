package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDocRequest {

    @NotNull(message = "知识库id不能为空")
    private Long kbId;

    /** 挂到哪个文件夹下，0=根目录 */
    private Long parentId = 0L;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字")
    private String title;

    /** 初始内容（Editor.js JSON）选填：导入脚本会带，前端新建文档不带则用空文档 */
    private String content;
}