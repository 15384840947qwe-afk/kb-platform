package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.dto.LoginRequest;
import com.lyq.kb.dto.LoginResponse;
import com.lyq.kb.dto.RegisterRequest;
import com.lyq.kb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** @Valid触发RegisterRequest上的校验注解，不合格直接被全局异常处理器接住 */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }
}