package com.example.demo1.dto;

/**
 * OAuth2UserInfo的Builder类
 * 手动实现Builder模式，解决Lombok可能存在的问题
 */
public class OAuth2UserInfoBuilder {
    private String platform;
    private String platformUserId;
    private String unionId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private String country;
    private String province;
    private String city;
    private String rawData;

    public OAuth2UserInfoBuilder platform(String platform) {
        this.platform = platform;
        return this;
    }

    public OAuth2UserInfoBuilder platformUserId(String platformUserId) {
        this.platformUserId = platformUserId;
        return this;
    }

    public OAuth2UserInfoBuilder unionId(String unionId) {
        this.unionId = unionId;
        return this;
    }

    public OAuth2UserInfoBuilder nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public OAuth2UserInfoBuilder avatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public OAuth2UserInfoBuilder gender(String gender) {
        this.gender = gender;
        return this;
    }

    public OAuth2UserInfoBuilder country(String country) {
        this.country = country;
        return this;
    }

    public OAuth2UserInfoBuilder province(String province) {
        this.province = province;
        return this;
    }

    public OAuth2UserInfoBuilder city(String city) {
        this.city = city;
        return this;
    }

    public OAuth2UserInfoBuilder rawData(String rawData) {
        this.rawData = rawData;
        return this;
    }

    public OAuth2UserInfo build() {
        OAuth2UserInfo userInfo = new OAuth2UserInfo();
        userInfo.setPlatform(platform);
        userInfo.setPlatformUserId(platformUserId);
        userInfo.setUnionId(unionId);
        userInfo.setNickname(nickname);
        userInfo.setAvatarUrl(avatarUrl);
        userInfo.setGender(gender);
        userInfo.setCountry(country);
        userInfo.setProvince(province);
        userInfo.setCity(city);
        userInfo.setRawData(rawData);
        return userInfo;
    }
}