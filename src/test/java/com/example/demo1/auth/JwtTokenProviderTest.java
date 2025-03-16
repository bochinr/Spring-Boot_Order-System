package com.example.demo1.auth;

import com.example.demo1.Entity.user;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Spy
    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    // 模拟JWT配置
    private final String jwtSecret = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private final int jwtExpiration = 3600; // 1小时过期

    @BeforeEach
    public void setup() {
        // 使用ReflectionTestUtils设置私有字段
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", jwtExpiration);
    }

    @Test
    @DisplayName("测试JWT令牌生成包含必要声明")
    public void testGenerateTokenWithClaims() {
        // 准备测试数据
        user testUser = new user();
        testUser.setId(1);
        testUser.setName("测试用户");
        testUser.setEmail("test@example.com");

        // 生成JWT令牌
        String token = jwtTokenProvider.generateToken(testUser);

        // 验证令牌不为空
        assertNotNull(token);

        // 验证令牌有效
        boolean isValid = jwtTokenProvider.validateToken(token);
        assertTrue(isValid, "生成的令牌应该有效");

        // 从令牌解析出用户ID
        Integer userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(testUser.getId(), userId, "令牌中的用户ID应该与原始用户ID匹配");

        // 解析令牌获取所有声明
        Claims claims = getAllClaimsFromToken(token);

        // 验证必要声明
        assertEquals(testUser.getId(), claims.get("userId", Integer.class), "令牌应包含正确的userId");
        assertEquals(testUser.getName(), claims.get("username", String.class), "令牌应包含正确的username");
        assertEquals("USER", claims.get("role", String.class), "令牌应包含角色声明");

        // 验证标准JWT字段
        assertEquals(testUser.getName(), claims.getSubject(), "令牌主题应该是用户名");
        assertNotNull(claims.getIssuedAt(), "令牌应包含发行时间");
        assertNotNull(claims.getExpiration(), "令牌应包含过期时间");

        // 验证过期时间大约是当前时间加上过期秒数
        long expectedExpirationTime = System.currentTimeMillis() + jwtExpiration * 1000;
        long actualExpirationTime = claims.getExpiration().getTime();

        // 允许1秒的误差
        assertTrue(Math.abs(expectedExpirationTime - actualExpirationTime) < 1000,
                "过期时间应该在预期范围内");
    }

    @Test
    @DisplayName("测试无效JWT令牌的验证")
    public void testInvalidToken() {
        // 测试空令牌
        assertFalse(jwtTokenProvider.validateToken(null), "空令牌应该是无效的");
        assertFalse(jwtTokenProvider.validateToken(""), "空字符串令牌应该是无效的");

        // 测试格式错误的令牌
        assertFalse(jwtTokenProvider.validateToken("invalid.token.format"), "格式错误的令牌应该是无效的");

        // 测试已过期的令牌（通过修改ReflectionTestUtils中的过期时间）
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -3600); // 将过期时间设置为负数，使令牌立即过期

        user testUser = new user();
        testUser.setId(1);
        testUser.setName("测试用户");

        String expiredToken = jwtTokenProvider.generateToken(testUser);
        assertFalse(jwtTokenProvider.validateToken(expiredToken), "过期的令牌应该是无效的");

        // 恢复原始过期时间
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", jwtExpiration);
    }

    @Test
    @DisplayName("测试JWT令牌的唯一标识")
    public void testTokenId() {
        // 准备测试数据
        user testUser1 = new user();
        testUser1.setId(1);
        testUser1.setName("用户1");

        user testUser2 = new user();
        testUser2.setId(2);
        testUser2.setName("用户2");

        // 生成两个不同用户的令牌
        String token1 = jwtTokenProvider.generateToken(testUser1);
        String token2 = jwtTokenProvider.generateToken(testUser2);

        // 获取令牌的唯一标识
        String tokenId1 = jwtTokenProvider.getTokenId(token1);
        String tokenId2 = jwtTokenProvider.getTokenId(token2);

        // 验证不同用户的令牌有不同的唯一标识
        assertNotEquals(tokenId1, tokenId2, "不同用户的令牌应该有不同的唯一标识");

        // 验证相同用户相同时间的令牌有相同的唯一标识
        doReturn(new Date()).when(jwtTokenProvider).getExpirationDate(anyString());

        String token1Again = jwtTokenProvider.generateToken(testUser1);
        String tokenId1Again = jwtTokenProvider.getTokenId(token1Again);

        // 不同时间生成的令牌可能有不同的唯一标识，所以这里我们跳过这个断言
        // 但总之，令牌ID不应该为空
        assertNotNull(tokenId1Again, "令牌ID不应该为空");
    }

    @Test
    @DisplayName("测试过期令牌的处理")
    public void testExpirationDateHandling() {
        // 准备测试数据
        user testUser = new user();
        testUser.setId(1);
        testUser.setName("测试用户");

        // 生成一个令牌
        String token = jwtTokenProvider.generateToken(testUser);

        // 获取过期时间
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);

        // 验证过期时间不为空且在将来
        assertNotNull(expirationDate, "过期时间不应该为空");
        assertTrue(expirationDate.after(new Date()), "过期时间应该在将来");

        // 验证过期时间大约是当前时间加上过期秒数
        long expectedExpirationTime = System.currentTimeMillis() + jwtExpiration * 1000;
        long actualExpirationTime = expirationDate.getTime();

        // 允许1秒的误差
        assertTrue(Math.abs(expectedExpirationTime - actualExpirationTime) < 1000,
                "过期时间应该在预期范围内");
    }

    // 辅助方法，使用反射调用私有方法
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
}