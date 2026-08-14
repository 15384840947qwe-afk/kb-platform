package com.lyq.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveDocRequest {

    /** 改标题选填；改了会同步到目录树，侧边栏和正文不分裂 */
    private String title;

    /** Editor.js的整份JSON，前端编辑器outputData序列化后原样传来 */
    @NotNull(message = "内容不能为空")
    private String content;

    /** 打开文档时拿到的version，原样带回——乐观锁的凭证 */
    @NotNull(message = "version不能为空")
    private Integer version;
}