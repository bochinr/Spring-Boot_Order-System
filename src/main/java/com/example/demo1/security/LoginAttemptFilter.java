package com.example.demo1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录尝试过滤器 - 在登录前检查账号是否已被锁定
 */
public class LoginAttemptFilter extends OncePerRequestFilter {

    private static final String LOGIN_LOCKED_KEY_PREFIX = "login:locked:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RequestMatcher loginPathMatcher;

    public LoginAttemptFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, String loginProcessingUrl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.loginPathMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 只拦截登录请求
        if (!loginPathMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取用户名或手机号
        String username = request.getParameter("username");
        if (username == null || username.isEmpty()) {
            username = request.getParameter("principal");
        }

        if (username == null || username.isEmpty()) {
            // 如果无法获取用户名，使用IP地址作为标识
            username = "ip:" + getClientIP(request);
        }

        // 检查账号是否已锁定
        String lockedKey = LOGIN_LOCKED_KEY_PREFIX + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey))) {
            // 获取剩余锁定时间
            Long remainingSeconds = redisTemplate.getExpire(lockedKey, TimeUnit.SECONDS);

            // 返回账号锁定信息
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "账号已锁定，请" + formatLockTime(remainingSeconds) + "后再试");

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return;
        }

        // 账号未锁定，继续登录流程
        filterChain.doFilter(request, response);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String realIP = request.getHeader("X-Real-IP");
        if (realIP != null && !realIP.isEmpty()) {
            return realIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * 格式化锁定时间
     */
    private String formatLockTime(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "稍后";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "分" + (remainingSeconds > 0 ? remainingSeconds + "秒" : "");
        } else {
            return seconds + "秒";
        }
    }
}