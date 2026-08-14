package com.lyq.kb.service;

import com.lyq.kb.dto.LoginRequest;
import com.lyq.kb.dto.LoginResponse;
import com.lyq.kb.dto.RegisterRequest;

public interface UserService {

    /** 注册：成功直接返回token，注册即登录 */
    LoginResponse register(RegisterRequest req);

    /** 登录：验证密码，签发token */
    LoginResponse login(LoginRequest req);
}