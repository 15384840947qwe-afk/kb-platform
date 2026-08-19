package com.lyq.kb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lyq.kb.mapper")
public class KbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbServiceApplication.class, args);
    }
}