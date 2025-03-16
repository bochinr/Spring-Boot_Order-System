package com.example.demo1.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.example.demo1.Entity.SocialAccount;
import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.config.AlipayConfig;
import com.example.demo1.config.WechatConfig;
import com.example.demo1.dto.OAuth2UserInfo;
import com.example.demo1.exception.OAuth2Exception;
import com.example.demo1.repository.SocialAccountRepository;
import com.example.demo1.service.OAuth2Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 第三方授权服务实现
 */
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ServiceImpl.class);
    private static final String WECHAT = "wechat";
    private static final String ALIPAY = "alipay";

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private userMapper userMp;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String generateAuthUrl(String platform, String state) {
        if (state == null) {
            state = UUID.randomUUID().toString();
        }

        try {
            if (WECHAT.equals(platform)) {
                // 微信授权URL
                String redirectUri = URLEncoder.encode(wechatConfig.getRedirectUri(),
                        StandardCharsets.UTF_8.toString());
                return "https://open.weixin.qq.com/connect/oauth2/authorize?" +
                        "appid=" + wechatConfig.getAppId() +
                        "&redirect_uri=" + redirectUri +
                        "&response_type=code" +
                        "&scope=snsapi_userinfo" +
                        "&state=" + state +
                        "#wechat_redirect";
            } else if (ALIPAY.equals(platform)) {
                // 支付宝授权URL，scope=auth_user表示获取用户信息
                String redirectUri = URLEncoder.encode(alipayConfig.getRedirectUri(),
                        StandardCharsets.UTF_8.toString());
                return "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?"
                        + "app_id=" + alipayConfig.getAppId()
                        + "&scope=auth_user"
                        + "&redirect_uri=" + redirectUri
                        + "&state=" + state;
            } else {
                throw new OAuth2Exception(400, "不支持的平台类型: " + platform);
            }
        } catch (Exception e) {
            log.error("生成授权URL异常", e);
            throw new OAuth2Exception("生成授权URL失败: " + e.getMessage());
        }
    }

    @Override
    public OAuth2UserInfo getUserInfo(String platform, String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new OAuth2Exception(400, "授权码不能为空");
        }

        try {
            if (WECHAT.equals(platform)) {
                return getWechatUserInfo(code);
            } else if (ALIPAY.equals(platform)) {
                return getAlipayUserInfo(code);
            } else {
                throw new OAuth2Exception(400, "不支持的平台类型: " + platform);
            }
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            throw new OAuth2Exception("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取微信用户信息
     */
    private OAuth2UserInfo getWechatUserInfo(String code) {
        try {
            // 1. 通过code获取access_token和openid
            String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                    "appid=" + wechatConfig.getAppId() +
                    "&secret=" + wechatConfig.getAppSecret() +
                    "&code=" + code +
                    "&grant_type=authorization_code";

            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            // 检查是否有错误
            if (tokenJson.has("errcode") && tokenJson.get("errcode").asInt() != 0) {
                String errMsg = tokenJson.has("errmsg") ? tokenJson.get("errmsg").asText() : "未知错误";
                throw new OAuth2Exception(401, "微信授权失败: " + errMsg);
            }

            String accessToken = tokenJson.get("access_token").asText();
            String openId = tokenJson.get("openid").asText();
            String unionId = tokenJson.has("unionid") ? tokenJson.get("unionid").asText() : null;

            // 2. 获取用户信息
            String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?" +
                    "access_token=" + accessToken +
                    "&openid=" + openId +
                    "&lang=zh_CN";

            ResponseEntity<String> userInfoResponse = restTemplate.getForEntity(userInfoUrl, String.class);
            JsonNode userInfoJson = objectMapper.readTree(userInfoResponse.getBody());

            // 检查是否有错误
            if (userInfoJson.has("errcode") && userInfoJson.get("errcode").asInt() != 0) {
                String errMsg = userInfoJson.has("errmsg") ? userInfoJson.get("errmsg").asText() : "未知错误";
                throw new OAuth2Exception(401, "获取微信用户信息失败: " + errMsg);
            }

            // 构建返回对象
            return OAuth2UserInfo.builder()
                    .platform(WECHAT)
                    .platformUserId(openId)
                    .unionId(unionId)
                    .nickname(userInfoJson.has("nickname") ? userInfoJson.get("nickname").asText() : null)
                    .avatarUrl(userInfoJson.has("headimgurl") ? userInfoJson.get("headimgurl").asText() : null)
                    .gender(userInfoJson.has("sex") ? userInfoJson.get("sex").asText() : null)
                    .country(userInfoJson.has("country") ? userInfoJson.get("country").asText() : null)
                    .province(userInfoJson.has("province") ? userInfoJson.get("province").asText() : null)
                    .city(userInfoJson.has("city") ? userInfoJson.get("city").asText() : null)
                    .rawData(userInfoResponse.getBody())
                    .build();
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            log.error("获取微信用户信息异常", e);
            throw new OAuth2Exception("获取微信用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取支付宝用户信息
     */
    private OAuth2UserInfo getAlipayUserInfo(String code) throws AlipayApiException {
        // 通过auth_code获取access_token
        AlipaySystemOauthTokenRequest tokenRequest = new AlipaySystemOauthTokenRequest();
        tokenRequest.setCode(code);
        tokenRequest.setGrantType("authorization_code");

        AlipaySystemOauthTokenResponse tokenResponse = alipayClient.execute(tokenRequest);

        if (!tokenResponse.isSuccess()) {
            throw new OAuth2Exception(401, "支付宝授权失败: " + tokenResponse.getMsg());
        }

        // 获取用户信息
        AlipayUserInfoShareRequest userInfoRequest = new AlipayUserInfoShareRequest();
        AlipayUserInfoShareResponse userInfoResponse = alipayClient.execute(userInfoRequest,
                tokenResponse.getAccessToken());

        if (!userInfoResponse.isSuccess()) {
            throw new OAuth2Exception(401, "获取支付宝用户信息失败: " + userInfoResponse.getMsg());
        }

        try {
            // 构建返回对象
            return OAuth2UserInfo.builder()
                    .platform(ALIPAY)
                    .platformUserId(tokenResponse.getUserId())
                    .nickname(userInfoResponse.getNickName())
                    .avatarUrl(userInfoResponse.getAvatar())
                    .gender(userInfoResponse.getGender()) // "F"=女性,"M"=男性
                    .province(userInfoResponse.getProvince())
                    .city(userInfoResponse.getCity())
                    .rawData(objectMapper.writeValueAsString(userInfoResponse))
                    .build();
        } catch (Exception e) {
            throw new OAuth2Exception("解析支付宝用户信息失败");
        }
    }

    @Override
    @Transactional
    public user createOrLinkUser(OAuth2UserInfo userInfo) {
        try {
            // 检查是否已有关联用户
            user existingUser = null;

            // 通过第三方平台ID查找用户
            if (WECHAT.equals(userInfo.getPlatform())) {
                existingUser = userMp.findByWechatOpenId(userInfo.getPlatformUserId());
            } else if (ALIPAY.equals(userInfo.getPlatform())) {
                existingUser = userMp.findByAlipayUserId(userInfo.getPlatformUserId());
            }

            // 通过社交账号关联表查找
            if (existingUser == null) {
                SocialAccount socialAccount = socialAccountRepository.findByPlatformTypeAndPlatformUserId(
                        userInfo.getPlatform().toUpperCase(), userInfo.getPlatformUserId());

                if (socialAccount != null) {
                    existingUser = socialAccount.getUser();
                }
            }

            // 如果找到已存在用户，更新第三方账号信息
            if (existingUser != null) {
                if (WECHAT.equals(userInfo.getPlatform()) && !StringUtils.hasText(existingUser.getWechatOpenid())) {
                    userMp.updateWechatOpenId(existingUser.getId(), userInfo.getPlatformUserId());
                    existingUser.setWechatOpenid(userInfo.getPlatformUserId());
                } else if (ALIPAY.equals(userInfo.getPlatform())
                        && !StringUtils.hasText(existingUser.getAlipayUserid())) {
                    userMp.updateAlipayUserId(existingUser.getId(), userInfo.getPlatformUserId());
                    existingUser.setAlipayUserid(userInfo.getPlatformUserId());
                }

                return existingUser;
            }

            // 如果没有找到关联用户，创建新用户
            user newUser = new user();
            newUser.setName(userInfo.getNickname() != null ? userInfo.getNickname()
                    : userInfo.getPlatform() + "_user_" + userInfo.getPlatformUserId().substring(0, 8));
            newUser.setPassword(UUID.randomUUID().toString()); // 随机密码
            newUser.setAge(0); // 默认年龄

            if (WECHAT.equals(userInfo.getPlatform())) {
                newUser.setWechatOpenid(userInfo.getPlatformUserId());
            } else if (ALIPAY.equals(userInfo.getPlatform())) {
                newUser.setAlipayUserid(userInfo.getPlatformUserId());
            }

            userMp.createUser(newUser);

            // 如果有unionId，创建社交账号关联
            if (StringUtils.hasText(userInfo.getUnionId())) {
                SocialAccount socialAccount = new SocialAccount();
                socialAccount.setUser(newUser);
                socialAccount.setPlatformType(SocialAccount.PlatformType.valueOf(userInfo.getPlatform().toUpperCase()));
                socialAccount.setPlatformUserId(userInfo.getPlatformUserId());
                socialAccount.setUnionId(userInfo.getUnionId());
                socialAccountRepository.save(socialAccount);
            }

            return newUser;
        } catch (Exception e) {
            log.error("创建或关联用户异常", e);
            throw new OAuth2Exception("创建或关联用户失败: " + e.getMessage());
        }
    }
}