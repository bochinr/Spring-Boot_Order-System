package com.example.demo1.auth.strategy;

import com.example.demo1.Entity.SocialAccount;
import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.dto.LoginRequest;
import com.example.demo1.exception.AuthenticationException;
import com.example.demo1.repository.SocialAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信登录策略
 */
@Component
public class WechatLoginStrategy implements LoginStrategy {

    private static final Logger log = LoggerFactory.getLogger(WechatLoginStrategy.class);

    @Autowired
    private userMapper userMp;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wechat.appid:wx_appid}")
    private String appId;

    @Value("${wechat.secret:wx_secret}")
    private String appSecret;

    @Override
    public user login(LoginRequest loginRequest) {
        String code = loginRequest.getPrincipal();

        if (!StringUtils.hasText(code)) {
            throw new AuthenticationException("微信授权码不能为空");
        }

        try {
            // 模拟调用微信获取OpenID的过程
            // 实际应调用微信API: https://api.weixin.qq.com/sns/oauth2/access_token
            String openId = getWxOpenId(code);
            String unionId = getWxUnionId(code);

            // 通过OpenID查找关联的用户
            SocialAccount socialAccount = socialAccountRepository.findByPlatformTypeAndPlatformUserId(
                    SocialAccount.PlatformType.WECHAT.name(), openId);

            if (socialAccount != null) {
                // 已存在关联账号，直接返回关联的用户
                return socialAccount.getUser();
            } else {
                // 查找是否有用户的微信OpenID直接绑定在user表
                user existingUser = userMp.findByWechatOpenId(openId);
                if (existingUser != null) {
                    return existingUser;
                }

                // 如果未找到关联用户，可以选择自动创建新用户或返回错误
                throw new AuthenticationException("微信账号未关联，请先注册");
            }
        } catch (Exception e) {
            log.error("微信登录失败: {}", e.getMessage(), e);
            throw new AuthenticationException("微信登录失败: " + e.getMessage());
        }
    }

    /**
     * 模拟获取微信OpenID
     * 实际项目中应调用微信API
     */
    private String getWxOpenId(String code) {
        log.info("模拟获取微信OpenID，code: {}", code);
        // 实际应调用:
        // String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" +
        // appId +
        // "&secret=" + appSecret + "&code=" + code + "&grant_type=authorization_code";
        // Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        // return (String) response.get("openid");

        // 这里模拟返回
        return "wx_" + code + "_openid";
    }

    /**
     * 模拟获取微信UnionID
     */
    private String getWxUnionId(String code) {
        log.info("模拟获取微信UnionID，code: {}", code);
        return "wx_" + code + "_unionid";
    }

    @Override
    public String getType() {
        return LOGIN_TYPE_WECHAT;
    }
}