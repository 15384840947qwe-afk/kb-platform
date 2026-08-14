package com.lyq.kb.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.entity.User;
import com.lyq.kb.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner：Spring启动完成后自动执行一次。
 * 用途：首次部署时用户表是空的，谁也登录不进来（鸡生蛋问题），
 * 所以检测到空表就自动创建初始管理员。只有第一次启动会真正插入
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(new QueryWrapper<User>());
        if (count == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("用户表为空，已自动创建初始管理员：admin / admin123");
        }
    }
}