package com.lyq.kb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 设计原则：默认拒绝。所有接口默认都要登录，
 * 不需要登录的必须显式进白名单——比"默认放行再挨个设防"安全得多
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",        // 登录注册本身不能要求先有token
                        "/actuator/**",    // 健康检查保持开放
                        "/file/proxy/**"   // 图片代理：img标签带不了token
                );
    }
}
