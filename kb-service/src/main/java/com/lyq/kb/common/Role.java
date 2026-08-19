package com.lyq.kb.common;

/**
 * 简单角色制（调研结论：小团队用角色制就够，不做文档级授权）：
 * ADMIN  = 管理员，可管理用户、删知识库
 * MEMBER = 成员，可建知识库、写文档、传文件
 * VIEWER = 访客，只读
 */
public enum Role {
    ADMIN, MEMBER, VIEWER
}