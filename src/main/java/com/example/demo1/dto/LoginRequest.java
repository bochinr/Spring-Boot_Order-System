package com.example.demo1.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    /**
     * 登录类型（phone/email/wechat/alipay）
     */
    private String loginType;

    /**
     * 手机号/邮箱/第三方授权code
     */
    private String principal;

    /**
     * 密码/验证码
     */
    private String credential;

    /**
     * 额外参数，用于第三方登录
     */
    private String extraParam;
}