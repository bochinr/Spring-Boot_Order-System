package com.example.demo1.config;

import com.example.demo1.auth.JwtTokenProvider;
import com.example.demo1.security.JwtBlacklistFilter;
import com.example.demo1.security.JwtBlacklistService;
import com.example.demo1.security.LoginAttemptFilter;
import com.example.demo1.security.LoginFailureHandler;
import com.example.demo1.security.SensitiveOperationInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security 配置类
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

        @Autowired
        private LoginFailureHandler loginFailureHandler;

        @Autowired
        private JwtBlacklistService jwtBlacklistService;

        @Autowired
        private StringRedisTemplate redisTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        /**
         * 密码编码器
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 配置安全过滤器链
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // 登录路径，用于登录失败锁定过滤器
                String loginProcessingUrl = "/api/auth/login";

                http
                                // 禁用CSRF保护，因为使用的是JWT
                                .csrf(csrf -> csrf.disable())

                                // 配置会话管理，使用无状态会话
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 配置请求授权规则
                                .authorizeHttpRequests(authorize -> authorize
                                                // 允许公开访问的路径，明确使用AntPathRequestMatcher
                                                .requestMatchers(
                                                                new AntPathRequestMatcher("/api/auth/login"),
                                                                new AntPathRequestMatcher("/api/auth/register"),
                                                                new AntPathRequestMatcher("/api/captcha/**"),
                                                                new AntPathRequestMatcher("/api/oauth/**"),
                                                                new AntPathRequestMatcher("/oauth-success.html"),
                                                                new AntPathRequestMatcher("/h2-console/**"))
                                                .permitAll()
                                                // 其他请求都需要认证
                                                .anyRequest().authenticated())

                                // 配置登录处理
                                .formLogin(form -> form
                                                .loginProcessingUrl(loginProcessingUrl)
                                                .failureHandler(loginFailureHandler))

                                // 添加过滤器
                                .addFilterBefore(
                                                new LoginAttemptFilter(redisTemplate, objectMapper, loginProcessingUrl),
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(new JwtBlacklistFilter(jwtBlacklistService, objectMapper,
                                                "/api/auth/login", "/api/auth/register", "/api/captcha/**",
                                                "/api/oauth/**", "/h2-console/**"),
                                                UsernamePasswordAuthenticationFilter.class)

                                // 允许H2控制台的frame加载
                                .headers(headers -> headers.frameOptions().disable());

                return http.build();
        }

        // 配置敏感操作拦截器
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SensitiveOperationInterceptor(jwtTokenProvider))
                                .addPathPatterns("/api/user/password", "/api/auth/logout");
        }
}