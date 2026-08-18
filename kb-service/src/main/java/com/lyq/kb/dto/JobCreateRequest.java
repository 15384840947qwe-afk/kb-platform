package com.lyq.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员手动录入/编辑岗位 */
@Data
public class JobCreateRequest {

    @NotBlank(message = "岗位名不能为空")
    @Size(max = 100, message = "岗位名不能超过100字")
    private String title;

    @Size(max = 100, message = "公司名不能超过100字")
    private String company;

    private String city;

    /** 薪资原文，如 15-25K·14薪 */
    private String salary;

    /** 经验要求原文，如 3-5年 */
    private String experience;

    /** 学历要求原文，如 本科 */
    private String education;

    /** 技能标签JSON数组，可选 */
    private String skillsJson;

    /** JD职责描述原文 */
    private String jdText;

    @Size(max = 300, message = "链接不能超过300字")
    private String jobUrl;
}
