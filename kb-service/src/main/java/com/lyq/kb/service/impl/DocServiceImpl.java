package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lyq.kb.common.AiGrader;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.DocText;
import com.lyq.kb.common.ForbiddenException;
import com.lyq.kb.common.Role;
import com.lyq.kb.common.TreeCache;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.dto.CreateDocRequest;
import com.lyq.kb.dto.DocAskRequest;
import com.lyq.kb.dto.SaveDocRequest;
import com.lyq.kb.entity.Catalog;
import com.lyq.kb.entity.Doc;
import com.lyq.kb.entity.FileRecord;
import com.lyq.kb.mapper.CatalogMapper;
import com.lyq.kb.mapper.DocMapper;
import com.lyq.kb.mapper.FileMapper;
import com.lyq.kb.mapper.KnowledgeBaseMapper;
import com.lyq.kb.service.DocService;
import com.lyq.kb.service.HistoryService;
import com.lyq.kb.service.RagService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    /** Editor.js合法的空文档结构，前端拿到可直接load */
    private static final String EMPTY_CONTENT = "{\"blocks\":[]}";

    private final DocMapper docMapper;
    private final CatalogMapper catalogMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final HistoryService historyService;
    private final TreeCache treeCache;
    private final FileMapper fileMapper;
    private final MinioClient minioClient;
    private final AiGrader aiGrader;
    private final RagService ragService;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    @Transactional
    public Doc create(CreateDocRequest req) {
        AuthUtil.requireWritable();
        if (knowledgeBaseMapper.selectById(req.getKbId()) == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        // 父节点校验：非根时必须是同库、已通过审核的文件夹
        if (req.getParentId() != 0) {
            Catalog parent = catalogMapper.selectById(req.getParentId());
            if (parent == null || !parent.getKbId().equals(req.getKbId())
                    || !"FOLDER".equals(parent.getNodeType())
                    || parent.getStatus() != 1) {
                throw new IllegalArgumentException("父文件夹不存在或未审核通过");
            }
        }

        // 第一张表：文档实体
        Doc doc = new Doc();
        doc.setKbId(req.getKbId());
        doc.setTitle(req.getTitle());
        // 带初始内容就用它（导入场景），没带就用空文档（前端新建场景）
        doc.setContent(req.getContent() != null && !req.getContent().isBlank()
                ? req.getContent() : EMPTY_CONTENT);
        doc.setVersion(0);
        doc.setCreatorId(UserContext.get().getId());
        doc.setUpdaterId(UserContext.get().getId());
        // 管理员建文档直接通过；成员先进草稿(3)，写完内容自己点提交审核
        boolean admin = Role.ADMIN.name().equals(UserContext.get().getRole());
        doc.setStatus(admin ? 1 : 3);
        // 意向挂载位置先记下，审核通过时才真正挂到目录树
        doc.setParentId(req.getParentId());
        docMapper.insert(doc);

        if (admin) {
            // 目录树里挂一个DOC指针节点，和阶段3的文件夹创建同款排序逻辑
            Catalog node = new Catalog();
            node.setKbId(req.getKbId());
            node.setParentId(req.getParentId());
            node.setTitle(req.getTitle());
            node.setNodeType("DOC");
            node.setDocId(doc.getId());
            long siblings = catalogMapper.selectCount(new QueryWrapper<Catalog>()
                    .eq("kb_id", req.getKbId()).eq("parent_id", req.getParentId()));
            node.setSortOrder((int) siblings);
            catalogMapper.insert(node);
            treeCache.evict(req.getKbId());
        }
        // 成员提交时不挂树：别人在目录里看不到半成品，审核通过由AuditService补挂
        return doc;
    }

    @Override
    public Doc detail(Long id) {
        Doc doc = mustGet(id);
        // 浏览历史：打开即记录，每人每文档唯一一条
        historyService.recordView(id, doc.getKbId());
        return doc;
    }

    @Override
    public Doc save(Long id, SaveDocRequest req) {
        AuthUtil.requireWritable();
        mustGet(id);
        // 乐观锁核心：UPDATE时带上WHERE version=你带来的旧值。
        // 你读完后如果没人改过，旧值匹配，更新成功且version+1；
        // 如果有人抢先保存了，旧值对不上，影响行数=0，我们就知道撞车了
        String newTitle = req.getTitle() != null ? req.getTitle() : null;
        UpdateWrapper<Doc> uw = new UpdateWrapper<Doc>()
                .eq("id", id)
                .eq("version", req.getVersion())
                .set("content", req.getContent())
                .set("version", req.getVersion() + 1)
                .set("updater_id", UserContext.get().getId());
        if (newTitle != null) {
            uw.set("title", newTitle);
        }
        int rows = docMapper.update(null, uw);
        if (rows == 0) {
            throw new IllegalArgumentException("文档已被他人修改，请刷新获取最新版本后再保存");
        }
        // 标题改了的话，目录节点同步改，侧边栏不分裂
        if (newTitle != null) {
            catalogMapper.update(null,
                    new UpdateWrapper<Catalog>().eq("doc_id", id).set("title", newTitle));
            treeCache.evict(docMapper.selectById(id).getKbId());
        }
        // 内容变了就重建向量索引：embedding要调外部接口，放事务提交后异步跑，不阻塞保存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                new Thread(() -> ragService.rebuild(id), "rag-rebuild-" + id).start();
            }
        });
        return docMapper.selectById(id);
        // 备注：MyBatis-Plus自带@Version注解+乐观锁插件能自动干这件事，
        // 这里手写UpdateWrapper是为了让你看清它底层就是这条带version条件的UPDATE——
        // 面试问"乐观锁怎么实现"，答的就是这条SQL
    }

    @Override
    public void submit(Long id) {
        AuthUtil.requireWritable();
        Doc doc = mustGet(id);
        // 只有草稿(3)和被驳回(2)能提交；待审核/已通过不能重复提交
        if (doc.getStatus() != 3 && doc.getStatus() != 2) {
            throw new IllegalArgumentException("当前状态不能提交审核");
        }
        doc.setStatus(0);
        docMapper.updateById(doc);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Doc doc = mustGet(id);
        boolean admin = Role.ADMIN.name().equals(UserContext.get().getRole());
        // 本人可撤回自己的待审核/草稿；已过的内容只有管理员能删
        boolean withdrawable = doc.getCreatorId().equals(UserContext.get().getId())
                && (doc.getStatus() == 0 || doc.getStatus() == 3);
        if (!admin && !withdrawable) {
            throw new ForbiddenException("仅管理员可删除，或本人在待审核/草稿时可撤回");
        }
        docMapper.deleteById(id);
        // 目录里的指针节点一起删，侧边栏不留死链
        catalogMapper.delete(new QueryWrapper<Catalog>().eq("doc_id", id));
        // 级联删附件：MinIO对象+DB记录
        for (FileRecord f : fileMapper.selectList(new QueryWrapper<FileRecord>().eq("doc_id", id))) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket).object(f.getObjectKey()).build());
            } catch (Exception e) {
                // 对象已不存在也不阻塞主流程
            }
            fileMapper.deleteById(f.getId());
        }
        treeCache.evict(doc.getKbId());
    }

    private Doc mustGet(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        return doc;
    }
    @Override
    public List<Doc> search(String keyword) {
        // .select只取三列：不把LONGTEXT的content捞出来，省内存省带宽；
        // like模糊匹配标题；limit 20防返回海量结果
        return docMapper.selectList(new QueryWrapper<Doc>()
                .select("id", "title", "kb_id")
                .like("title", keyword)
                .orderByDesc("update_time")
                .last("limit 20"));
    }

    // ===== 文档问答（RAG，混合检索版）：向量+关键词打分取Top3块→拼上下文交AI =====

    @Override
    public void ask(Long id, DocAskRequest req, Consumer<String> onDelta, Consumer<String> onDone) {
        Doc doc = mustGet(id);
        String text = DocText.toPlainText(doc.getContent());
        if (text.isBlank()) {
            String msg = "这篇文档还没有正文内容，暂时没法回答。";
            onDelta.accept(msg);
            onDone.accept(msg);
            return;
        }
        // 混合检索取Top3块（块表没建过会懒建）；拼成约1200字上下文控token
        List<String> picked = ragService.retrieve(id, req.getQuestion(), 3);
        String joined = String.join("\n", picked);
        String context = joined.length() > 1200 ? joined.substring(0, 1200) : joined;
        StringBuilder user = new StringBuilder();
        // 多轮上下文：最多带最近2轮，控制token
        if (req.getHistory() != null && !req.getHistory().isEmpty()) {
            List<DocAskRequest.Item> recent = req.getHistory();
            recent = recent.subList(Math.max(0, recent.size() - 4), recent.size());
            user.append("此前的问答：\n");
            for (DocAskRequest.Item h : recent) {
                if (h.getText() == null || h.getText().isBlank()) {
                    continue;
                }
                user.append("assistant".equals(h.getRole()) ? "A：" : "Q：").append(h.getText()).append('\n');
            }
            user.append('\n');
        }
        user.append("本次问题：").append(req.getQuestion());

        StringBuilder full = new StringBuilder();
        boolean ok = aiGrader.chatStream(
                "你是教材问答助手。只依据下面的『教材摘录』回答问题，摘录没提到的内容就明说教材里没涉及，" +
                "不要自行编造。回答简洁有条理，可以引用原文。\n教材摘录：\n" + context,
                user.toString(),
                40,
                d -> {
                    full.append(d);
                    onDelta.accept(d);
                });
        if (!ok && full.length() == 0) {
            String msg = "AI助手暂时不可用，请稍后再试。";
            onDelta.accept(msg);
            full.append(msg);
        }
        onDone.accept(full.toString());
    }

    @Override
    public String reindexAll() {
        AuthUtil.requireAdmin();
        // 只索引审核通过的文档：草稿/被驳回的半成品不进知识库
        List<Doc> docs = docMapper.selectList(
                new QueryWrapper<Doc>().select("id").eq("status", 1));
        if (docs.isEmpty()) {
            return "没有已审核通过的文档，无需重建";
        }
        // embedding一篇篇调外部接口，放后台线程跑，接口立刻返回不阻塞
        new Thread(() -> {
            for (Doc d : docs) {
                ragService.rebuild(d.getId());
            }
        }, "rag-reindex").start();
        return "已开始重建" + docs.size() + "篇文档的向量索引，后台进行中";
    }
}