package com.lyq.kb.dto;

import lombok.Data;

/** 简历-JD匹配请求：jd为目标岗位的职位描述全文 */
@Data
public class ResumeJdRequest {

    private String jd;
}
