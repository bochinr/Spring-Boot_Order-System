package com.example.demo1.dto;

import lombok.Data;

/**
 * 第三方用户信息DTO
 */
@Data
public class OAuth2UserInfo {

    /**
     * 平台类型（WECHAT/ALIPAY）
     */
    private String platform;

    /**
     * 平台唯一标识（OpenID/UserId）
     */
    private String platformUserId;

    /**
     * 统一标识（UnionID）
     */
    private String unionId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private String gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 原始响应数据（JSON字符串）
     */
    private String rawData;

    /**
     * 默认构造函数
     */
    public OAuth2UserInfo() {
    }

    /**
     * 提供静态builder方法
     */
    public static OAuth2UserInfoBuilder builder() {
        return new OAuth2UserInfoBuilder();
    }
}