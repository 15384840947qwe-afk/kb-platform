package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.common.JwtUtil;
import com.lyq.kb.common.Role;
import com.lyq.kb.dto.LoginRequest;
import com.lyq.kb.dto.LoginResponse;
import com.lyq.kb.dto.RegisterRequest;
import com.lyq.kb.entity.User;
import com.lyq.kb.mapper.UserMapper;
import com.lyq.kb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse register(RegisterRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null && !req.getNickname().isBlank()
                ? req.getNickname() : req.getUsername());
        // 新用户默认MEMBER；ADMIN只有种子账号，防止注册接口被薅出管理员
        user.setRole(Role.MEMBER.name());
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // uk_username唯一索引兜底：并发下两个人同时注册同名，
            // 靠数据库挡住而不是靠"先查后插"（那个有竞态窗口）
            throw new IllegalArgumentException("用户名已存在");
        }
        return toLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", req.getUsername()));
        // 用户不存在和密码错误给同一句话：不让试探者区分"这个用户名存在"，
        // 防用户名枚举，安全课标准操作
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        return toLoginResponse(user);
    }

    /** 组装返回体：签发token + 用户展示信息 */
    private LoginResponse toLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }
}