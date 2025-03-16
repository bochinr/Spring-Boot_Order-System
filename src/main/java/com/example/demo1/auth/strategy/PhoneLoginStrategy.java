package com.example.demo1.auth.strategy;

import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.dto.LoginRequest;
import com.example.demo1.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 手机号验证码登录策略
 */
@Component
public class PhoneLoginStrategy implements LoginStrategy {

    @Autowired
    private userMapper userMp;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";

    @Override
    public user login(LoginRequest loginRequest) {
        String phone = loginRequest.getPrincipal();
        String smsCode = loginRequest.getCredential();

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(smsCode)) {
            throw new AuthenticationException("手机号和验证码不能为空");
        }

        // 1. 校验手机验证码
        String key = SMS_CODE_PREFIX + phone;
        Object savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new AuthenticationException("验证码已过期");
        }

        if (!smsCode.equals(savedCode.toString())) {
            throw new AuthenticationException("验证码错误");
        }

        // 验证通过后删除验证码
        redisTemplate.delete(key);

        // 2. 查找用户
        user user = userMp.findByPhone(phone);

        // 3. 如果用户不存在，可以选择自动注册新用户
        if (user == null) {
            throw new AuthenticationException("该手机号未注册");
        }

        return user;
    }

    @Override
    public String getType() {
        return LOGIN_TYPE_PHONE;
    }
}