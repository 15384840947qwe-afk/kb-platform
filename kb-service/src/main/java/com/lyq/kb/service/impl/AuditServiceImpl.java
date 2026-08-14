package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.TreeCache;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.SubmissionVO;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.KnowledgeBase;
import com.lyq.kb.mapper.CatalogMapper;
import com.lyq.kb.mapper.DocMapper;
import com.lyq.kb.mapper.KnowledgeBaseMapper;
import com.lyq.kb.rabbit.AuditEventProducer;
import com.lyq.kb.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocMapper docMapper;
    private final CatalogMapper catalogMapper;
    private final TreeCache treeCache;
    private final AuditEventProducer auditEventProducer;

    @Override
    public Map<String, Object> pending() {
        AuthUtil.requireAdmin();
        Map<String, Object> out = new HashMap<>();
        out.put("bases", knowledgeBaseMapper.selectList(
                new QueryWrapper<KnowledgeBase>().eq("status", 0).orderByAsc("create_time")));
        out.put("folders", catalogMapper.selectList(
                new QueryWrapper<Catalog>().eq("status", 0).eq("node_type", "FOLDER")
                        .orderByAsc("create_time")));
        out.put("docs", docMapper.selectList(
                new QueryWrapper<Doc>().eq("status", 0).orderByAsc("create_time")));
        return out;
    }

    @Override
    public List<SubmissionVO> mine() {
        Long me = UserContext.get().getId();
        List<SubmissionVO> out = new ArrayList<>();
        for (KnowledgeBase kb : knowledgeBaseMapper.selectList(
                new QueryWrapper<KnowledgeBase>().eq("owner_id", me).ne("status", 1))) {
            SubmissionVO vo = new SubmissionVO();
            vo.setType("知识库");
            vo.setId(kb.getId());
            vo.setKbId(kb.getId());
            vo.setTitle(kb.getName());
            vo.setStatus(kb.getStatus());
            vo.setCreateTime(kb.getCreateTime());
            out.add(vo);
        }
        for (Catalog c : catalogMapper.selectList(
                new QueryWrapper<Catalog>().eq("create_by", me)
                        .eq("node_type", "FOLDER").ne("status", 1))) {
            SubmissionVO vo = new SubmissionVO();
            vo.setType("文件夹");
            vo.setId(c.getId());
            vo.setKbId(c.getKbId());
            vo.setTitle(c.getTitle());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            out.add(vo);
        }
        for (Doc d : docMapper.selectList(
                new QueryWrapper<Doc>().eq("creator_id", me).ne("status", 1))) {
            SubmissionVO vo = new SubmissionVO();
            vo.setType("文档");
            vo.setId(d.getId());
            vo.setKbId(d.getKbId());
            vo.setTitle(d.getTitle());
            vo.setStatus(d.getStatus());
            vo.setCreateTime(d.getCreateTime());
            out.add(vo);
        }
        // 两类合一起按时间倒序
        out.sort(Comparator.comparing(SubmissionVO::getCreateTime).reversed());
        return out;
    }

    @Override
    public void auditBase(Long id, boolean ok) {
        AuthUtil.requireAdmin();
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (kb.getStatus() != 0) {
            throw new IllegalArgumentException("该知识库已审核过");
        }
        kb.setStatus(ok ? 1 : 2);
        knowledgeBaseMapper.updateById(kb);
    }

    @Override
    @Transactional
    public void auditDoc(Long id, boolean ok) {
        AuthUtil.requireAdmin();
        Doc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (doc.getStatus() != 0) {
            throw new IllegalArgumentException("该文档已审核过");
        }
        doc.setStatus(ok ? 1 : 2);
        docMapper.updateById(doc);
        if (ok) {
            // 通过才挂树：补上提交时没建的目录节点，挂到当初选的文件夹下
            Long parentId = doc.getParentId() != null ? doc.getParentId() : 0L;
            Catalog node = new Catalog();
            node.setKbId(doc.getKbId());
            node.setParentId(parentId);
            node.setTitle(doc.getTitle());
            node.setNodeType("DOC");
            node.setDocId(doc.getId());
            node.setStatus(1);
            long siblings = catalogMapper.selectCount(new QueryWrapper<Catalog>()
                    .eq("kb_id", doc.getKbId()).eq("parent_id", parentId));
            node.setSortOrder((int) siblings);
            catalogMapper.insert(node);
            // 树变了同步清缓存；再发MQ事件，通知/计数等后续处理异步做
            treeCache.evict(doc.getKbId());
            auditEventProducer.sendDocApproved(doc.getKbId(), doc.getTitle());
        }
    }

    @Override
    public void auditFolder(Long id, boolean ok) {
        AuthUtil.requireAdmin();
        Catalog c = catalogMapper.selectById(id);
        if (c == null) {
            throw new IllegalArgumentException("文件夹不存在");
        }
        if (c.getStatus() != 0) {
            throw new IllegalArgumentException("该文件夹已审核过");
        }
        // 通过才在树上可见（tree查询只取status=1）
        c.setStatus(ok ? 1 : 2);
        catalogMapper.updateById(c);
        treeCache.evict(c.getKbId());
    }
}
