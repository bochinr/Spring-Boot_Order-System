package com.example.demo1.security;

import com.example.demo1.auth.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT黑名单服务 - 管理已注销的token
 */
@Service
public class JwtBlacklistService {

    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 将token加入黑名单
     * 
     * @param token JWT令牌
     * @return 是否成功加入黑名单
     */
    public boolean addToBlacklist(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }

            // 验证token有效性
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            // 获取token的剩余有效期
            long expirationMs = jwtTokenProvider.getExpirationDate(token).getTime() - System.currentTimeMillis();
            if (expirationMs <= 0) {
                // token已过期，无需加入黑名单
                return true;
            }

            // 计算过期时间（秒）
            long expirationSeconds = expirationMs / 1000;

            // 获取token的唯一标识（可以是jti或整个token的hash值）
            String tokenId = jwtTokenProvider.getTokenId(token);
            if (tokenId == null || tokenId.isEmpty()) {
                // 如果没有jti，使用token本身作为key
                tokenId = token;
            }

            // 将token加入Redis黑名单，过期时间设置为JWT的过期时间
            String blacklistKey = JWT_BLACKLIST_PREFIX + tokenId;
            redisTemplate.opsForValue().set(blacklistKey, "1", expirationSeconds, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            // 出现异常，加入黑名单失败
            return false;
        }
    }

    /**
     * 检查token是否在黑名单中
     * 
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }

            // 获取token的唯一标识
            String tokenId = jwtTokenProvider.getTokenId(token);
            if (tokenId == null || tokenId.isEmpty()) {
                // 如果没有jti，使用token本身作为key
                tokenId = token;
            }

            // 检查Redis中是否存在此token
            String blacklistKey = JWT_BLACKLIST_PREFIX + tokenId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            // 出现异常，当作未拉黑处理
            return false;
        }
    }
}