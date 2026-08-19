package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据。类名叫FileRecord不叫File，
 * 避免和java.io.File撞名引起import混乱
 */
@Data
@TableName("t_file")
public class FileRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private Long docId;
    private String originalName;
    private String objectKey;
    private Long size;
    private String contentType;
    private Long uploaderId;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}