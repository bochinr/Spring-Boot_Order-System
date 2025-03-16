package com.example.demo1.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunSmsConfig {

    @Value("${aliyun.sms.access-key-id:your-access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret:your-access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name:your-sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code:your-template-code}")
    private String templateCode;

    @Bean
    public Client smsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("dysmsapi.aliyuncs.com");
        return new Client(config);
    }

    public String getSignName() {
        return signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }
}