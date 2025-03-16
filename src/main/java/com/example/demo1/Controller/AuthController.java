package com.example.demo1.Controller;

import com.example.demo1.Entity.user;
import com.example.demo1.auth.JwtTokenProvider;
import com.example.demo1.auth.strategy.LoginStrategy;
import com.example.demo1.auth.strategy.LoginStrategyFactory;
import com.example.demo1.dto.ApiResponse;
import com.example.demo1.dto.LoginRequest;
import com.example.demo1.dto.LoginResponse;
import com.example.demo1.exception.AuthenticationException;
import com.example.demo1.service.LoginLogService;
import com.example.demo1.security.JwtBlacklistService;
import com.example.demo1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 处理登录、注册和退出登录
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private LoginStrategyFactory loginStrategyFactory;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    @Value("${jwt.expiration:86400}")
    private int jwtExpiration;

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String loginType = loginRequest.get("loginType");
            String principal = loginRequest.get("principal");
            String credential = loginRequest.get("credential");

            if (loginType == null || principal == null || credential == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("缺少必要的登录参数"));
            }

            // 根据登录类型调用不同的登录方法
            user authenticatedUser = null;

            switch (loginType) {
                case "phone":
                    // 手机号登录
                    authenticatedUser = userService.loginByPhone(principal, credential);
                    break;
                case "email":
                    // 邮箱密码登录
                    authenticatedUser = userService.loginByEmail(principal, credential);
                    break;
                default:
                    return ResponseEntity.badRequest().body(createErrorResponse("不支持的登录方式"));
            }

            if (authenticatedUser == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("登录失败，用户名或密码错误"));
            }

            // 生成JWT
            String token = jwtTokenProvider.generateToken(authenticatedUser);

            // 构建返回数据
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", authenticatedUser.getId());
            userData.put("username", authenticatedUser.getName());
            userData.put("token", token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", userData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("登录失败: " + e.getMessage()));
        }
    }

    /**
     * 退出登录
     * 
     * @param request HTTP请求
     * @return 退出登录结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null && !token.isEmpty()) {
            // 将token加入黑名单
            jwtBlacklistService.addToBlacklist(token);
        }

        // 返回成功响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "已成功退出登录");
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String email = registerRequest.get("email");
            String phone = registerRequest.get("phone");
            String captcha = registerRequest.get("captcha");
            String captchaToken = registerRequest.get("captchaToken");

            if (username == null || password == null || (email == null && phone == null)) {
                return ResponseEntity.badRequest().body(createErrorResponse("缺少必要的注册参数"));
            }

            // 注册用户
            user newUser = userService.registerUser(username, password, email, phone);

            if (newUser == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("注册失败，可能用户名已存在"));
            }

            // 生成JWT
            String token = jwtTokenProvider.generateToken(newUser);

            // 构建返回数据
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", newUser.getId());
            userData.put("username", newUser.getName());
            userData.put("token", token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", userData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("注册失败: " + e.getMessage()));
        }
    }

    /**
     * 从请求中提取JWT token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}