package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.Role;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.common.TreeCache;
import com.lyq.kb.dto.CatalogNodeVO;
import com.lyq.kb.dto.CreateCatalogRequest;
import com.lyq.kb.dto.MoveCatalogRequest;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.mapper.CatalogMapper;
import com.lyq.kb.mapper.KnowledgeBaseMapper;
import com.lyq.kb.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private static final String FOLDER = "FOLDER";

    private final CatalogMapper catalogMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TreeCache treeCache;
    private final ObjectMapper objectMapper;

    @Override
    public List<CatalogNodeVO> tree(Long kbId) {
        // 先查缓存：目录树读多写少，命中就免去查库组树
        String cached = treeCache.get(kbId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CatalogNodeVO>>() {});
            } catch (Exception e) {
                // 缓存坏了当miss处理，下面重建
            }
        }
        // 一次SQL查出该库全部节点，内存里组装成树。
        // 千万别"查一层、再递归查子层"——节点越多SQL次数越炸，
        // 这就是经典的N+1问题，面试聊树形查询的考点就在这
        List<Catalog> nodes = catalogMapper.selectList(
                new QueryWrapper<Catalog>().eq("kb_id", kbId).eq("status", 1)
                        .orderByAsc("sort_order"));

        // 第一遍：全部转VO放进map，id做key
        Map<Long, CatalogNodeVO> voMap = new HashMap<>();
        for (Catalog c : nodes) {
            voMap.put(c.getId(), toVO(c));
        }
        // 第二遍：挂父子关系。parent_id=0进根列表，其余挂到父VO的children里
        List<CatalogNodeVO> roots = new ArrayList<>();
        for (Catalog c : nodes) {
            CatalogNodeVO vo = voMap.get(c.getId());
            if (c.getParentId() == 0) {
                roots.add(vo);
            } else {
                CatalogNodeVO parent = voMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        // 组好的树进缓存，写操作会evict它
        try {
            treeCache.put(kbId, objectMapper.writeValueAsString(roots));
        } catch (Exception e) {
            // 缓存写失败不影响主流程
        }
        return roots;
    }

    @Override
    public Catalog create(CreateCatalogRequest req) {
        AuthUtil.requireWritable();
        if (knowledgeBaseMapper.selectById(req.getKbId()) == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        // 父节点校验：非根时，父必须存在、同库、是文件夹，且已审核通过——
        // 只能挂在"已有的正式目录"下，堵住往待审核目录里塞东西的越级路径
        if (req.getParentId() != 0) {
            Catalog parent = catalogMapper.selectById(req.getParentId());
            if (parent == null || !parent.getKbId().equals(req.getKbId())) {
                throw new IllegalArgumentException("父节点不存在");
            }
            if (!FOLDER.equals(parent.getNodeType())) {
                throw new IllegalArgumentException("文档节点下不能创建子节点");
            }
            if (parent.getStatus() != 1) {
                throw new IllegalArgumentException("父目录尚未审核通过");
            }
        }
        Catalog catalog = new Catalog();
        catalog.setKbId(req.getKbId());
        catalog.setParentId(req.getParentId());
        catalog.setTitle(req.getTitle());
        // 3b只建文件夹；DOC类型节点在阶段4"创建文档"时连带创建
        catalog.setNodeType(FOLDER);
        // 管理员建目录直接通过；成员建目录进待审核，树上先不显示
        catalog.setStatus(Role.ADMIN.name().equals(UserContext.get().getRole()) ? 1 : 0);
        catalog.setCreateBy(UserContext.get().getId());
        // 追加到同级末尾：排序值=当前同级数量
        long siblings = catalogMapper.selectCount(new QueryWrapper<Catalog>()
                .eq("kb_id", req.getKbId()).eq("parent_id", req.getParentId()));
        catalog.setSortOrder((int) siblings);
        catalogMapper.insert(catalog);
        treeCache.evict(catalog.getKbId());
        return catalog;
    }

    @Override
    public void rename(Long id, String title) {
        AuthUtil.requireWritable();
        Catalog catalog = mustGet(id);
        catalog.setTitle(title);
        catalogMapper.updateById(catalog);
        treeCache.evict(catalog.getKbId());
    }

    @Override
    public void move(MoveCatalogRequest req) {
        AuthUtil.requireWritable();
        Catalog catalog = mustGet(req.getId());
        if (req.getParentId() != 0) {
            Catalog parent = catalogMapper.selectById(req.getParentId());
            if (parent == null || !parent.getKbId().equals(catalog.getKbId())) {
                throw new IllegalArgumentException("目标父节点不存在");
            }
            if (!FOLDER.equals(parent.getNodeType())) {
                throw new IllegalArgumentException("不能移入文档节点");
            }
            // 防环：不能把文件夹拖进它自己的子树，否则树就出环了
            ensureNotDescendant(catalog.getId(), req.getParentId());
        }
        catalog.setParentId(req.getParentId());
        if (req.getSortOrder() != null) {
            catalog.setSortOrder(req.getSortOrder());
        }
        catalogMapper.updateById(catalog);
        treeCache.evict(catalog.getKbId());
    }

    @Override
    public void deleteFolder(Long id) {
        // 删除权收归管理员
        AuthUtil.requireAdmin();
        Catalog catalog = mustGet(id);
        if (!FOLDER.equals(catalog.getNodeType())) {
            throw new IllegalArgumentException("文档节点请走文档删除接口");
        }
        long children = catalogMapper.selectCount(
                new QueryWrapper<Catalog>().eq("parent_id", id));
        if (children > 0) {
            // 保守策略：必须先清空。防止一个手滑删掉整棵子树
            throw new IllegalArgumentException("文件夹下还有子节点，请先删除");
        }
        catalogMapper.deleteById(id);
        treeCache.evict(catalog.getKbId());
    }

    private Catalog mustGet(Long id) {
        Catalog catalog = catalogMapper.selectById(id);
        if (catalog == null) {
            throw new IllegalArgumentException("节点不存在");
        }
        return catalog;
    }

    /** 从目标父节点一路向上走，如果撞见被移动的节点自己，说明会成环 */
    private void ensureNotDescendant(Long nodeId, Long targetParentId) {
        Long cur = targetParentId;
        while (cur != null && cur != 0) {
            if (cur.equals(nodeId)) {
                throw new IllegalArgumentException("不能移入自己的子树");
            }
            Catalog p = catalogMapper.selectById(cur);
            if (p == null) {
                break;
            }
            cur = p.getParentId();
        }
    }

    private CatalogNodeVO toVO(Catalog c) {
        CatalogNodeVO vo = new CatalogNodeVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setTitle(c.getTitle());
        vo.setNodeType(c.getNodeType());
        vo.setDocId(c.getDocId());
        vo.setSortOrder(c.getSortOrder());
        return vo;
    }
}