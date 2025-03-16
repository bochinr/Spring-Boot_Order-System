package com.example.demo1.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝开放平台配置
 */
@Configuration
public class AlipayConfig {

    @Value("${alipay.appid:alipay_appid}")
    private String appId;

    @Value("${alipay.privateKey:alipay_private_key}")
    private String privateKey;

    @Value("${alipay.publicKey:alipay_public_key}")
    private String publicKey;

    @Value("${alipay.redirect-uri:http://localhost:8080/api/oauth/alipay/callback}")
    private String redirectUri;

    // 支付宝网关（固定）
    private static final String GATEWAY_URL = "https://openapi.alipay.com/gateway.do";
    // 数据格式
    private static final String FORMAT = "json";
    // 字符编码
    private static final String CHARSET = "UTF-8";
    // 签名方式
    private static final String SIGN_TYPE = "RSA2";

    /**
     * 支付宝客户端
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(GATEWAY_URL, appId, privateKey, FORMAT, CHARSET, publicKey, SIGN_TYPE);
    }

    public String getAppId() {
        return appId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}