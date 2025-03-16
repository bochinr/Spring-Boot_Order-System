package com.example.demo1.auth.strategy;

import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.dto.LoginRequest;
import com.example.demo1.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮箱密码登录策略
 */
@Component
public class EmailLoginStrategy implements LoginStrategy {

    @Autowired
    private userMapper userMp;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public user login(LoginRequest loginRequest) {
        String email = loginRequest.getPrincipal();
        String password = loginRequest.getCredential();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new AuthenticationException("邮箱和密码不能为空");
        }

        // 查找用户
        user user = userMp.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("该邮箱未注册");
        }

        // 验证密码
        // 注意：实际系统中应该使用passwordEncoder.matches(password, user.getPassword())
        // 但由于现有系统可能没有加密存储密码，这里直接比较明文密码
        if (!password.equals(user.getPassword()) &&
                !passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("密码错误");
        }

        return user;
    }

    @Override
    public String getType() {
        return LOGIN_TYPE_EMAIL;
    }
}