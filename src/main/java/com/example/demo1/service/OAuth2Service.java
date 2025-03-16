package com.example.demo1.service;

import com.example.demo1.Entity.user;
import com.example.demo1.dto.OAuth2UserInfo;

/**
 * 第三方授权服务接口
 */
public interface OAuth2Service {

    /**
     * 生成授权URL
     * 
     * @param platform 平台类型（wechat/alipay）
     * @param state    状态参数，用于防止CSRF攻击
     * @return 授权URL
     */
    String generateAuthUrl(String platform, String state);

    /**
     * 获取第三方用户信息
     * 
     * @param platform 平台类型（wechat/alipay）
     * @param code     授权码
     * @param state    状态参数
     * @return 用户信息
     */
    OAuth2UserInfo getUserInfo(String platform, String code, String state);

    /**
     * 创建或关联本地用户
     * 
     * @param userInfo 第三方用户信息
     * @return 本地用户
     */
    user createOrLinkUser(OAuth2UserInfo userInfo);
}