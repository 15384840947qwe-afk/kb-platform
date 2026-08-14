package com.lyq.kb.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具：生成和解析token。
 * token结构=头.载荷.签名，签名用yml里的secret算——
 * 谁拿着secret谁就能签发合法token，所以secret绝不能泄露
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    /** 构造器注入：从application.yml读jwt.secret和jwt.expire-hours */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-hours}") int expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    /** 签发token：把用户身份写进claims，相当于把身份证号印到证件上 */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析token：同时完成验签名和查过期，
     * 伪造的或过期的都会抛JwtException，由调用方处理
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}