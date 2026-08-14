package com.lyq.kb.config;

import com.lyq.kb.common.JwtUtil;
import com.lyq.kb.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器：除白名单外所有请求的第一道关卡。
 * 流程：取Authorization头 → 验token → 解析出身份存UserContext → 放行；
 * 任何一步失败，401直接打回
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        // 约定格式：Authorization: Bearer <token>
        if (auth == null || !auth.startsWith("Bearer ")) {
            reject(response, 401, "未登录：请先登录获取token");
            return false;
        }
        try {
            Claims claims = jwtUtil.parseToken(auth.substring(7));
            UserContext.CurrentUser user = new UserContext.CurrentUser();
            user.setId(Long.parseLong(claims.getSubject()));
            user.setUsername(claims.get("username", String.class));
            user.setRole(claims.get("role", String.class));
            UserContext.set(user);
            return true;
        } catch (Exception e) {
            // 伪造、篡改、过期，jjwt都会抛异常，统一401
            reject(response, 401, "token无效或已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 无论成功失败都执行：清ThreadLocal防线程复用串身份
        UserContext.clear();
    }

    /** 拦截器在Controller之前，进不了全局异常处理器，所以自己手写JSON响应 */
    private void reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}