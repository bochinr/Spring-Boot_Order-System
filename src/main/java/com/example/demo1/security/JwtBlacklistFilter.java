package com.example.demo1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT黑名单过滤器 - 拦截黑名单中的token
 */
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtBlacklistService jwtBlacklistService;
    private final ObjectMapper objectMapper;
    private final RequestMatcher excludedPaths;

    public JwtBlacklistFilter(JwtBlacklistService jwtBlacklistService, ObjectMapper objectMapper,
            String... excludedPathPatterns) {
        this.jwtBlacklistService = jwtBlacklistService;
        this.objectMapper = objectMapper;

        if (excludedPathPatterns != null && excludedPathPatterns.length > 0) {
            List<RequestMatcher> matchers = Arrays.stream(excludedPathPatterns)
                    .map(pattern -> new AntPathRequestMatcher(pattern))
                    .collect(Collectors.toList());
            this.excludedPaths = new OrRequestMatcher(matchers);
        } else {
            this.excludedPaths = null;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 检查是否是排除的路径
        if (excludedPaths != null && excludedPaths.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取token
        String token = getTokenFromRequest(request);

        // 如果没有token，直接放行交给下一个过滤器处理
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查token是否在黑名单中
        if (jwtBlacklistService.isBlacklisted(token)) {
            // token已被注销，返回401错误
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Token已失效，请重新登录");

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return;
        }

        // token不在黑名单中，继续处理
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}