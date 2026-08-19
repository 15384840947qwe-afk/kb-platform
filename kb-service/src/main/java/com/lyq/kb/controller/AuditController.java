package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.dto.SubmissionVO;
import com.lyq.kb.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** 管理员：待审核队列 */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending() {
        return Result.ok(auditService.pending());
    }

    /** 登录用户：自己待审核/被驳回的提交 */
    @GetMapping("/mine")
    public Result<List<SubmissionVO>> mine() {
        return Result.ok(auditService.mine());
    }

    @PostMapping("/base/{id}/approve")
    public Result<Void> approveBase(@PathVariable Long id) {
        auditService.auditBase(id, true);
        return Result.ok();
    }

    @PostMapping("/base/{id}/reject")
    public Result<Void> rejectBase(@PathVariable Long id) {
        auditService.auditBase(id, false);
        return Result.ok();
    }

    @PostMapping("/doc/{id}/approve")
    public Result<Void> approveDoc(@PathVariable Long id) {
        auditService.auditDoc(id, true);
        return Result.ok();
    }

    @PostMapping("/doc/{id}/reject")
    public Result<Void> rejectDoc(@PathVariable Long id) {
        auditService.auditDoc(id, false);
        return Result.ok();
    }

    @PostMapping("/folder/{id}/approve")
    public Result<Void> approveFolder(@PathVariable Long id) {
        auditService.auditFolder(id, true);
        return Result.ok();
    }

    @PostMapping("/folder/{id}/reject")
    public Result<Void> rejectFolder(@PathVariable Long id) {
        auditService.auditFolder(id, false);
        return Result.ok();
    }
}
