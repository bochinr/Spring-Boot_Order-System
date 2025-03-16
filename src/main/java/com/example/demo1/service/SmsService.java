package com.example.demo1.service;

/**
 * 短信服务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     * 
     * @param phone   手机号
     * @param captcha 图形验证码（用于校验）
     * @return 发送结果
     */
    void sendSmsCode(String phone, String captcha);

    /**
     * 验证短信验证码
     * 
     * @param phone 手机号
     * @param code  短信验证码
     * @return 是否验证通过
     */
    boolean verifySmsCode(String phone, String code);
}