package com.example.demo1.service;

import com.example.demo1.Entity.SocialAccount;
import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.config.AlipayConfig;
import com.example.demo1.config.WechatConfig;
import com.example.demo1.dto.OAuth2UserInfo;
import com.example.demo1.exception.OAuth2Exception;
import com.example.demo1.repository.SocialAccountRepository;
import com.example.demo1.service.impl.OAuth2ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.alipay.api.AlipayClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2ServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserService userService;

    @Mock
    private WechatConfig wechatConfig;

    @Mock
    private AlipayConfig alipayConfig;

    @Mock
    private AlipayClient alipayClient;

    @Mock
    private userMapper userMp;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @InjectMocks
    private OAuth2ServiceImpl oauth2Service;

    // 测试数据
    private final String wechatAppId = "test_app_id";
    private final String wechatAppSecret = "test_app_secret";
    private final String wechatRedirectUri = "http://localhost:8080/api/oauth/wechat/callback";

    @BeforeEach
    public void setup() {
        // 设置模拟配置
        lenient().when(wechatConfig.getAppId()).thenReturn(wechatAppId);
        lenient().when(wechatConfig.getAppSecret()).thenReturn(wechatAppSecret);
        lenient().when(wechatConfig.getRedirectUri()).thenReturn(wechatRedirectUri);
    }

    @Test
    @DisplayName("测试生成微信OAuth2.0授权URL")
    public void testGenerateWechatAuthUrl() {
        // 生成测试状态值
        String state = UUID.randomUUID().toString();

        // 设置必要的模拟行为
        when(wechatConfig.getAppId()).thenReturn(wechatAppId);
        when(wechatConfig.getRedirectUri()).thenReturn(wechatRedirectUri);

        // 调用被测方法
        String authUrl = oauth2Service.generateAuthUrl("wechat", state);

        // 验证URL包含必要参数
        assertNotNull(authUrl, "授权URL不应该为空");
        assertTrue(authUrl.contains("appid=" + wechatAppId), "授权URL应该包含appid");
        assertTrue(authUrl.contains("redirect_uri="), "授权URL应该包含redirect_uri");
        assertTrue(authUrl.contains("state=" + state), "授权URL应该包含state参数");
        assertTrue(authUrl.contains("scope="), "授权URL应该包含scope参数");
        assertTrue(authUrl.contains("response_type=code"), "授权URL应该包含response_type参数");
    }

    @Test
    @DisplayName("测试微信OAuth2.0用户信息获取")
    public void testGetWechatUserInfo() throws Exception {
        // 模拟授权码
        String code = "test_auth_code";
        String state = "test_state";

        // 模拟获取访问令牌的响应
        Map<String, Object> accessTokenResponse = new HashMap<>();
        accessTokenResponse.put("access_token", "test_access_token");
        accessTokenResponse.put("openid", "test_openid");
        accessTokenResponse.put("unionid", "test_unionid");

        // 模拟获取用户信息的响应
        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("openid", "test_openid");
        userInfoResponse.put("unionid", "test_unionid");
        userInfoResponse.put("nickname", "微信用户");
        userInfoResponse.put("headimgurl", "http://example.com/avatar.jpg");

        // 设置WechatConfig模拟行为
        when(wechatConfig.getAppId()).thenReturn(wechatAppId);
        when(wechatConfig.getAppSecret()).thenReturn(wechatAppSecret);

        // 设置RestTemplate的行为
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(accessTokenResponse, HttpStatus.OK);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(tokenResponseEntity, userInfoResponseEntity);

        // 调用被测方法
        OAuth2UserInfo userInfo = oauth2Service.getUserInfo("wechat", code, state);

        // 验证结果
        assertNotNull(userInfo, "用户信息不应该为空");
        assertEquals("test_openid", userInfo.getPlatformUserId(), "OpenID应该匹配");
        assertEquals("test_unionid", userInfo.getUnionId(), "UnionID应该匹配");
        assertEquals("微信用户", userInfo.getNickname(), "昵称应该匹配");
        assertEquals("http://example.com/avatar.jpg", userInfo.getAvatarUrl(), "头像URL应该匹配");
        assertEquals("WECHAT", userInfo.getPlatform(), "平台应该是微信");
    }

    @Test
    @DisplayName("测试微信OAuth2.0错误处理")
    public void testWechatOAuth2ErrorHandling() {
        // 模拟授权码
        String code = "invalid_code";
        String state = "test_state";

        // 模拟错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errcode", 40029);
        errorResponse.put("errmsg", "invalid code");

        // 设置WechatConfig模拟行为
        when(wechatConfig.getAppId()).thenReturn(wechatAppId);
        when(wechatConfig.getAppSecret()).thenReturn(wechatAppSecret);

        // 设置RestTemplate的行为
        ResponseEntity<Map> errorResponseEntity = new ResponseEntity<>(errorResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(errorResponseEntity);

        // 验证抛出OAuth2Exception
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> {
            oauth2Service.getUserInfo("wechat", code, state);
        }, "应该抛出OAuth2Exception");

        // 验证异常内容包含错误消息
        assertTrue(exception.getMessage().contains("40029") ||
                exception.getMessage().contains("invalid code"),
                "异常消息应该包含错误代码或错误原因");
    }

    @Test
    @DisplayName("测试创建或关联用户")
    public void testCreateOrLinkUser() {
        // 模拟OAuth2用户信息
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .platform("WECHAT")
                .platformUserId("test_openid")
                .unionId("test_unionid")
                .nickname("微信用户")
                .avatarUrl("http://example.com/avatar.jpg")
                .build();

        // 模拟新用户
        user newUser = new user();
        newUser.setId(1);
        newUser.setName("微信用户");

        // 模拟社交账号为空
        when(socialAccountRepository.findByPlatformTypeAndPlatformUserId(
                eq("WECHAT"), eq("test_openid"))).thenReturn(null);

        // 模拟通过unionId找不到用户
        when(userService.findByUnionId("WECHAT", "test_unionid")).thenReturn(null);

        // 模拟创建用户
        when(userService.createUserFromOAuth2(any(OAuth2UserInfo.class))).thenReturn(newUser);

        // 调用被测方法
        user result = oauth2Service.createOrLinkUser(userInfo);

        // 验证结果
        assertNotNull(result, "返回的用户不应该为空");

        // 验证UserService方法被正确调用
        verify(userService).findByUnionId("WECHAT", "test_unionid");
        verify(userService).createUserFromOAuth2(userInfo);
    }

    @Test
    @DisplayName("测试关联已存在的用户")
    public void testLinkExistingUser() {
        // 模拟OAuth2用户信息
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .platform("WECHAT")
                .platformUserId("test_openid")
                .unionId("test_unionid")
                .nickname("微信用户")
                .avatarUrl("http://example.com/avatar.jpg")
                .build();

        // 模拟已存在的用户
        user existingUser = new user();
        existingUser.setId(1);
        existingUser.setName("已存在用户");

        // 模拟社交账号为空
        when(socialAccountRepository.findByPlatformTypeAndPlatformUserId(
                eq("WECHAT"), eq("test_openid"))).thenReturn(null);

        // 设置UserService的行为
        when(userService.findByUnionId("WECHAT", "test_unionid")).thenReturn(existingUser);

        // 调用被测方法
        user result = oauth2Service.createOrLinkUser(userInfo);

        // 验证结果
        assertNotNull(result, "返回的用户不应该为空");
        assertEquals(existingUser.getId(), result.getId(), "用户ID应该匹配");
        assertEquals(existingUser.getName(), result.getName(), "用户名应该匹配");

        // 验证UserService方法被正确调用
        verify(userService).findByUnionId("WECHAT", "test_unionid");
        // 不应该创建新用户
        verify(userService, never()).createUserFromOAuth2(any(OAuth2UserInfo.class));
    }

    @Test
    @DisplayName("测试不支持的OAuth2平台")
    public void testUnsupportedPlatform() {
        // 调用被测方法并验证抛出异常
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> {
            oauth2Service.generateAuthUrl("unsupported_platform", "test_state");
        }, "应该抛出OAuth2Exception");

        // 验证异常内容
        assertTrue(exception.getMessage().contains("不支持的平台"), "异常消息应该指明不支持的平台");
    }
}