package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 目录树节点：FOLDER=文件夹，DOC=指向一篇文档的指针 */
@Data
@TableName("t_catalog")
public class Catalog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private Long parentId;
    private String title;
    private String nodeType;
    private Long docId;
    private Integer sortOrder;
    /** 0待审核 1通过 2驳回；管理员创建直接1，成员建文件夹进审核 */
    private Integer status;
    /** 创建人id */
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}