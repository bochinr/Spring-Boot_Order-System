package com.example.demo1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败处理器 - 负责记录失败次数并在超过阈值时锁定账号
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_FAILED_ATTEMPTS = 5; // 最大失败尝试次数
    private static final int LOCK_DURATION_MINUTES = 15; // 锁定时长(分钟)

    private static final String LOGIN_FAILED_KEY_PREFIX = "login:failed:";
    private static final String LOGIN_LOCKED_KEY_PREFIX = "login:locked:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        // 如果是手机号登录，获取手机号
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
            sendErrorResponse(response, "账号已锁定，请" + formatLockTime(remainingSeconds) + "后再试",
                    HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        // 增加失败计数
        String failedKey = LOGIN_FAILED_KEY_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(failedKey);

        // 设置失败记录的过期时间（比锁定时间长一些，确保在锁定期内始终能检测到）
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(failedKey, LOCK_DURATION_MINUTES + 10, TimeUnit.MINUTES);
        }

        // 检查是否达到最大失败次数
        if (attempts != null && attempts >= MAX_FAILED_ATTEMPTS) {
            // 锁定账号
            redisTemplate.opsForValue().set(lockedKey, "locked", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);

            // 返回账号锁定信息
            sendErrorResponse(response, "登录失败次数过多，账号已锁定" + LOCK_DURATION_MINUTES + "分钟",
                    HttpStatus.TOO_MANY_REQUESTS.value());
        } else {
            // 返回普通登录失败信息
            int remainingAttempts = MAX_FAILED_ATTEMPTS - (attempts != null ? attempts.intValue() : 0);
            sendErrorResponse(response, "登录失败，还剩" + remainingAttempts + "次尝试机会", HttpStatus.UNAUTHORIZED.value());
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
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