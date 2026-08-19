package com.lyq.kb.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录/注册成功的返回：token+展示用的用户信息。
 * 前端把token存起来，之后每个请求都带上它
 */
@Data
@Builder
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String avatar;
}