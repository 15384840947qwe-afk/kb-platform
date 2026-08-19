package com.lyq.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，映射t_user表。
 * MyBatis-Plus默认驼峰转下划线：createTime -> create_time，不用额外配置
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /**
     * @JsonIgnore：这个字段永远不会出现在返回给前端的JSON里。
     * 安全铁律——密码哈希虽然不可逆，也不能往外传
     */
    @JsonIgnore
    private String password;

    private String nickname;
    private String avatar;

    /** 存ADMIN/MEMBER/VIEWER字符串，和t_user.role对应 */
    private String role;

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @TableLogic：逻辑删除开关。之后调deleteById不会真DELETE，
     * 而是UPDATE deleted=1；所有查询自动带WHERE deleted=0
     */
    @TableLogic
    private Integer deleted;
}