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

/**
 * 支付宝登录策略
 */
@Component
public class AlipayLoginStrategy implements LoginStrategy {

    private static final Logger log = LoggerFactory.getLogger(AlipayLoginStrategy.class);

    @Autowired
    private userMapper userMp;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Value("${alipay.appid:alipay_appid}")
    private String appId;

    @Value("${alipay.privateKey:alipay_private_key}")
    private String privateKey;

    @Override
    public user login(LoginRequest loginRequest) {
        String authCode = loginRequest.getPrincipal();

        if (!StringUtils.hasText(authCode)) {
            throw new AuthenticationException("支付宝授权码不能为空");
        }

        try {
            // 模拟调用支付宝获取UserId的过程
            // 实际应使用支付宝SDK调用接口
            String userId = getAlipayUserId(authCode);

            // 通过UserId查找关联的用户
            SocialAccount socialAccount = socialAccountRepository.findByPlatformTypeAndPlatformUserId(
                    SocialAccount.PlatformType.ALIPAY.name(), userId);

            if (socialAccount != null) {
                // 已存在关联账号，直接返回关联的用户
                return socialAccount.getUser();
            } else {
                // 查找是否有用户的支付宝ID直接绑定在user表
                user existingUser = userMp.findByAlipayUserId(userId);
                if (existingUser != null) {
                    return existingUser;
                }

                // 如果未找到关联用户，可以选择自动创建新用户或返回错误
                throw new AuthenticationException("支付宝账号未关联，请先注册");
            }
        } catch (Exception e) {
            log.error("支付宝登录失败: {}", e.getMessage(), e);
            throw new AuthenticationException("支付宝登录失败: " + e.getMessage());
        }
    }

    /**
     * 模拟获取支付宝用户ID
     * 实际项目中应调用支付宝SDK
     */
    private String getAlipayUserId(String authCode) {
        log.info("模拟获取支付宝用户ID，authCode: {}", authCode);
        // 实际应调用支付宝SDK:
        // AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        // request.setCode(authCode);
        // request.setGrantType("authorization_code");
        // AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
        // return response.getUserId();

        // 这里模拟返回
        return "alipay_" + authCode + "_userid";
    }

    @Override
    public String getType() {
        return LOGIN_TYPE_ALIPAY;
    }
}