package com.example.demo1.auth.strategy;

import com.example.demo1.Entity.user;
import com.example.demo1.dto.LoginRequest;

/**
 * 登录策略接口
 */
public interface LoginStrategy {

    /**
     * 登录类型常量
     */
    String LOGIN_TYPE_PHONE = "phone";
    String LOGIN_TYPE_EMAIL = "email";
    String LOGIN_TYPE_WECHAT = "wechat";
    String LOGIN_TYPE_ALIPAY = "alipay";

    /**
     * 执行登录逻辑
     * 
     * @param loginRequest 登录请求
     * @return 登录成功的用户对象
     */
    user login(LoginRequest loginRequest);

    /**
     * 获取登录策略类型
     * 
     * @return 登录类型
     */
    String getType();
}
