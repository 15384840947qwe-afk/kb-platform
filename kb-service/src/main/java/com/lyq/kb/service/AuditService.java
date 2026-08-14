package com.lyq.kb.service;

import com.lyq.kb.dto.SubmissionVO;

import java.util.List;
import java.util.Map;

public interface AuditService {

    /** 管理员：待审核队列（知识库+文档） */
    Map<String, Object> pending();

    /** 当前用户：自己待审核和被驳回的提交 */
    List<SubmissionVO> mine();

    /** 审核知识库：ok=true通过，false驳回 */
    void auditBase(Long id, boolean ok);

    /** 审核文档：通过时补挂目录树 */
    void auditDoc(Long id, boolean ok);

    /** 审核文件夹：通过才在树上可见 */
    void auditFolder(Long id, boolean ok);
}
