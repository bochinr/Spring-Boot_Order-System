package com.example.demo1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 微信开放平台配置
 */
@Configuration
public class WechatConfig {

    @Value("${wechat.appid:wx_appid}")
    private String appId;

    @Value("${wechat.secret:wx_secret}")
    private String appSecret;

    @Value("${wechat.redirect-uri:http://localhost:8080/api/oauth/wechat/callback}")
    private String redirectUri;

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}