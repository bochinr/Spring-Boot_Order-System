package com.example.demo1.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 应用通用配置
 */
@Configuration
public class AppConfig {

    /**
     * 创建RestTemplate Bean用于HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 创建ObjectMapper Bean用于JSON处理
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}