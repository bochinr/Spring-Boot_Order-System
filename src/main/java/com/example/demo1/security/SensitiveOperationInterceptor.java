package com.example.demo1.security;

import com.example.demo1.auth.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感操作拦截器 - 需要二次验证的操作
 */
public class SensitiveOperationInterceptor implements HandlerInterceptor {

    // 验证过期时间（分钟）
    private static final int VERIFICATION_EXPIRATION_MINUTES = 10;

    private final JwtTokenProvider jwtTokenProvider;

    public SensitiveOperationInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 只拦截带有@RequireVerification注解的方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireVerification annotation = handlerMethod.getMethodAnnotation(RequireVerification.class);

        if (annotation == null) {
            return true;
        }

        // 获取请求头中的JWT令牌
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "未授权");
            return false;
        }

        // 验证JWT令牌
        if (!jwtTokenProvider.validateToken(token)) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "令牌无效");
            return false;
        }

        // 这里简化了验证逻辑，只验证token有效性
        // 在实际实现中，可能需要更复杂的验证逻辑，如检查用户是否已通过二次验证等
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.value());
        errorResponse.put("message", message);

        response.getWriter().write(errorResponse.toString());
    }
}