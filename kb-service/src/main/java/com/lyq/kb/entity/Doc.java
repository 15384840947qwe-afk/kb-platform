package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_doc")
public class Doc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String title;
    /** Editor.js的JSON内容，阶段4才真正读写它 */
    private String content;
    /** 乐观锁版本号 */
    private Integer version;
    private Long creatorId;
    private Long updaterId;
    /** 0待审核 1通过 2驳回；管理员创建直接1 */
    private Integer status;
    /** 提交时想挂的文件夹，审核通过时才真正挂到目录树 */
    private Long parentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}