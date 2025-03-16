package com.example.demo1.auth;

import com.example.demo1.Entity.user;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌生成器
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mysecretkey}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private int jwtExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 为用户生成JWT令牌
     * 
     * @param user 用户信息
     * @return JWT令牌
     */
    public String generateToken(user user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getName());

        // 添加用户角色 - 实际应用中应从用户权限表获取
        claims.put("role", "USER");

        if (user.getEmail() != null) {
            claims.put("email", user.getEmail());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从token中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Integer.class);
    }

    /**
     * 验证token是否有效
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取token的过期时间
     *
     * @param token JWT令牌
     * @return 过期日期
     */
    public Date getExpirationDate(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取token的唯一标识
     *
     * @param token JWT令牌
     * @return token的唯一标识
     */
    public String getTokenId(String token) {
        Claims claims = getAllClaimsFromToken(token);
        // 优先使用jti字段
        String jti = claims.getId();
        if (jti != null && !jti.isEmpty()) {
            return jti;
        }

        // 如果没有jti，使用subject+发行时间+哈希值组合
        String subject = claims.getSubject();
        Date issuedAt = claims.getIssuedAt();
        String timeStr = issuedAt != null ? String.valueOf(issuedAt.getTime()) : "";

        return subject + ":" + timeStr;
    }

    /**
     * 从token中获取所有的Claims
     *
     * @param token JWT令牌
     * @return Claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}