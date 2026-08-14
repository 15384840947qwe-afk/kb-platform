package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.Role;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.CreateBaseRequest;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.FileRecord;
import com.lyq.kb.entity.KnowledgeBase;
import com.lyq.kb.mapper.CatalogMapper;
import com.lyq.kb.mapper.DocMapper;
import com.lyq.kb.mapper.FileMapper;
import com.lyq.kb.mapper.KnowledgeBaseMapper;
import com.lyq.kb.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final CatalogMapper catalogMapper;
    private final DocMapper docMapper;
    private final FileMapper fileMapper;

    @Override
    public KnowledgeBase create(CreateBaseRequest req) {
        // VIEWER只读，拦在业务门口
        AuthUtil.requireWritable();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(req.getName());
        kb.setDescription(req.getDescription());
        // 创建人从当前登录身份取，不信前端传的任何"owner"字段
        kb.setOwnerId(UserContext.get().getId());
        // 管理员建库直接通过；成员建库进待审核
        kb.setStatus(Role.ADMIN.name().equals(UserContext.get().getRole()) ? 1 : 0);
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    @Override
    public List<KnowledgeBase> list() {
        // 通过的库全员可见；自己待审核的库可见（带后缀）；
        // 被驳回的库从选择器消失，驳回记录在"我的提交"里查
        Long me = UserContext.get().getId();
        boolean admin = Role.ADMIN.name().equals(UserContext.get().getRole());
        QueryWrapper<KnowledgeBase> qw = new QueryWrapper<KnowledgeBase>().orderByDesc("create_time");
        if (!admin) {
            qw.and(w -> w.eq("status", 1)
                    .or(o -> o.eq("status", 0).eq("owner_id", me)));
        }
        return knowledgeBaseMapper.selectList(qw);
    }

    @Override
    public KnowledgeBase detail(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        return kb;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = detail(id);
        // 删库只许管理员（需求收紧：成员可建可改，删除权收归管理员）
        AuthUtil.requireAdmin();
        // 级联逻辑删除：@TableLogic让delete变成UPDATE deleted=1，数据还在库里，误删能救
        catalogMapper.delete(new QueryWrapper<Catalog>().eq("kb_id", id));
        docMapper.delete(new QueryWrapper<Doc>().eq("kb_id", id));
        fileMapper.delete(new QueryWrapper<FileRecord>().eq("kb_id", id));
        knowledgeBaseMapper.deleteById(id);
        // @Transactional保证上面五步要么全成功要么全回滚，
        // 不会出现"知识库删了、文档还悬着"的半截状态
    }
}