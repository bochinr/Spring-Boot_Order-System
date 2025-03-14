package com.example.demo1;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GlobalInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/index.html",
                    "/",
                    "/user/login",
                    "/user/register",
                    "/user/toRegister",
                    "/user/listAll",
                    "/user/delete",
                    "/user/search",
                    "/user/detail",
                    "/food/list",
                    "/food/detail",
                    "/img/**",
                    "/css/**",
                    "/js/**"
                );
    }
}

