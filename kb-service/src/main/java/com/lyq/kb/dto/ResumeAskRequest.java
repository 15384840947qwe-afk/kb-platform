package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 预留：针对某份简历继续追问（二期做对话式优化建议用） */
@Data
public class ResumeAskRequest {
    @NotNull(message = "缺少简历id")
    private Long resumeId;
    @NotBlank(message = "问题不能为空")
    private String question;
}
