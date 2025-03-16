package com.example.demo1.Controller;

import com.example.demo1.Entity.user;
import com.example.demo1.auth.JwtTokenProvider;
import com.example.demo1.dto.OAuth2UserInfo;
import com.example.demo1.exception.OAuth2Exception;
import com.example.demo1.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 第三方OAuth2授权控制器
 */
@RestController
@RequestMapping("/api/oauth")
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    @Autowired
    private OAuth2Service oauth2Service;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.frontend-success-url:http://localhost:3000/oauth/success}")
    private String frontendSuccessUrl;

    @Value("${oauth2.frontend-error-url:http://localhost:3000/oauth/error}")
    private String frontendErrorUrl;

    /**
     * 生成授权URL并进行重定向
     * 
     * @param platform 平台类型（wechat/alipay）
     * @return 重定向视图
     */
    @GetMapping("/{platform}/authorize")
    public RedirectView authorize(@PathVariable String platform) {
        try {
            String state = UUID.randomUUID().toString();
            String authUrl = oauth2Service.generateAuthUrl(platform, state);
            return new RedirectView(authUrl);
        } catch (Exception e) {
            log.error("生成授权URL失败", e);
            return new RedirectView(frontendErrorUrl + "?error=authorization_failed&message=" + e.getMessage());
        }
    }

    /**
     * 获取授权URL（不进行重定向，返回URL供前端使用）
     * 
     * @param platform 平台类型（wechat/alipay）
     * @return 授权URL
     */
    @GetMapping("/{platform}/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable String platform) {
        try {
            String state = UUID.randomUUID().toString();
            String authUrl = oauth2Service.generateAuthUrl(platform, state);

            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            response.put("state", state);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成授权URL失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "authorization_failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 处理第三方平台的授权回调
     * 
     * @param platform 平台类型（wechat/alipay）
     * @param code     授权码
     * @param state    状态
     * @return 重定向到前端页面
     */
    @GetMapping("/{platform}/callback")
    public RedirectView callback(
            @PathVariable String platform,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request) {

        try {
            // 如果是支付宝，参数名可能是auth_code而不是code
            if (code == null && platform.equals("alipay") && request.getParameter("auth_code") != null) {
                code = request.getParameter("auth_code");
            }

            // 如果没有code，可能用户拒绝授权
            if (code == null) {
                return new RedirectView(frontendErrorUrl + "?error=access_denied&message=用户拒绝授权");
            }

            // 获取用户信息
            OAuth2UserInfo userInfo = oauth2Service.getUserInfo(platform, code, state);

            // 创建或关联用户
            user localUser = oauth2Service.createOrLinkUser(userInfo);

            // 生成JWT
            String token = jwtTokenProvider.generateToken(localUser);

            // 重定向到前端成功页面，并带上token和用户基本信息
            return new RedirectView(frontendSuccessUrl +
                    "?token=" + token +
                    "&userId=" + localUser.getId() +
                    "&username=" + localUser.getName());

        } catch (OAuth2Exception e) {
            log.error("OAuth2授权回调处理失败", e);
            return new RedirectView(frontendErrorUrl + "?error=" + e.getCode() + "&message=" + e.getMessage());
        } catch (Exception e) {
            log.error("OAuth2授权回调处理失败", e);
            return new RedirectView(frontendErrorUrl + "?error=server_error&message=" + e.getMessage());
        }
    }

    /**
     * OAuth2用户信息接口（需要传入code直接获取信息，不进行账号关联）
     * 
     * @param platform 平台类型
     * @param code     授权码
     * @return 用户信息
     */
    @GetMapping("/{platform}/user-info")
    public ResponseEntity<?> getUserInfo(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        try {
            OAuth2UserInfo userInfo = oauth2Service.getUserInfo(platform, code, state);
            return ResponseEntity.ok(userInfo);
        } catch (OAuth2Exception e) {
            log.error("获取OAuth2用户信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getCode());
            error.put("message", e.getMessage());
            return ResponseEntity.status(e.getCode()).body(error);
        } catch (Exception e) {
            log.error("获取OAuth2用户信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "server_error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}