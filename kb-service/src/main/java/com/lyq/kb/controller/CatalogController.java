package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.dto.CatalogNodeVO;
import com.lyq.kb.dto.CreateCatalogRequest;
import com.lyq.kb.dto.MoveCatalogRequest;
import com.lyq.kb.dto.RenameCatalogRequest;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /** 整棵树：前端侧边栏直接递归渲染 */
    @GetMapping("/tree/{kbId}")
    public Result<List<CatalogNodeVO>> tree(@PathVariable Long kbId) {
        return Result.ok(catalogService.tree(kbId));
    }

    @PostMapping
    public Result<Catalog> create(@Valid @RequestBody CreateCatalogRequest req) {
        return Result.ok(catalogService.create(req));
    }

    @PutMapping("/{id}/rename")
    public Result<Void> rename(@PathVariable Long id, @Valid @RequestBody RenameCatalogRequest req) {
        catalogService.rename(id, req.getTitle());
        return Result.ok();
    }

    @PutMapping("/move")
    public Result<Void> move(@Valid @RequestBody MoveCatalogRequest req) {
        catalogService.move(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        catalogService.deleteFolder(id);
        return Result.ok();
    }
}