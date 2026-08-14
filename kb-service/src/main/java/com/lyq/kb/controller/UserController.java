package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.common.Role;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.entity.User;
import com.lyq.kb.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /** "我是谁"：身份从UserContext取（也就是从你的token解出来的） */
    @GetMapping("/me")
    public Result<User> me() {
        User user = userMapper.selectById(UserContext.get().getId());
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }
        return Result.ok(user);
    }

    /**
     * 管理员专属演示接口：显式角色检查。
     * 以后"删知识库""禁用用户"这类管理操作都照这个模子：
     * 取当前用户→验角色→不够就403
     */
    @GetMapping("/admin-ping")
    public Result<String> adminPing() {
        if (!Role.ADMIN.name().equals(UserContext.get().getRole())) {
            return Result.fail(403, "需要管理员权限");
        }
        return Result.ok("你好管理员：" + UserContext.get().getUsername());
    }
}