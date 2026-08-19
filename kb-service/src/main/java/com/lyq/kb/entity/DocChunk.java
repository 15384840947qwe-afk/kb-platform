package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 文档向量块：RAG检索用，保存文档时整篇重建 */
@Data
@TableName("t_doc_chunk")
public class DocChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private Integer seq;
    private String content;
    /** 向量JSON数组，如 [0.1,0.2,...]；embedding不可用时为null */
    private String embedding;
    private LocalDateTime createTime;
}
