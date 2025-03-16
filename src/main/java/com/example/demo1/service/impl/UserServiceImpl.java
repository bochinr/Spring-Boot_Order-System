package com.example.demo1.service.impl;

import com.example.demo1.Entity.user;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.dto.OAuth2UserInfo;
import com.example.demo1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * UserService接口的实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private userMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public user findByUnionId(String platform, String unionId) {
        // 模拟实现，实际应根据平台和unionId查询用户
        return null;
    }

    @Override
    public user createUserFromOAuth2(OAuth2UserInfo userInfo) {
        // 创建新用户
        user newUser = new user();
        newUser.setName(userInfo.getNickname() != null
                ? userInfo.getNickname().substring(0, Math.min(userInfo.getNickname().length(), 8))
                : "用户" + System.currentTimeMillis());
        // 设置一个随机密码
        newUser.setPassword(passwordEncoder.encode("随机密码" + System.currentTimeMillis()));
        newUser.setAge(0); // 默认年龄

        // 保存用户
        userMapper.register(newUser.getName(), newUser.getPassword(), newUser.getAge());

        return userMapper.findByname(newUser.getName());
    }

    @Override
    public user loginByPhone(String phone, String password) {
        // 模拟实现，实际应根据手机号查询用户并验证密码
        return null;
    }

    @Override
    public user loginByEmail(String email, String password) {
        // 模拟实现，实际应根据邮箱查询用户并验证密码
        return null;
    }

    @Override
    public user registerUser(String username, String password, String phone, String email) {
        // 检查用户名是否已存在
        user existingUser = userMapper.findByname(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);

        // 注册用户
        userMapper.register(username, encodedPassword, 0);

        return userMapper.findByname(username);
    }

    @Override
    public user getUserById(String userId) {
        try {
            int id = Integer.parseInt(userId);
            return userMapper.findById(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的用户ID格式");
        }
    }

    @Override
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        try {
            int id = Integer.parseInt(userId);
            user user = userMapper.findById(id);

            if (user == null) {
                return false;
            }

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return false;
            }

            // 更新密码
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            // 调用userMapper更新密码的方法
            int result = userMapper.updatePassword(id, encodedNewPassword);

            return result > 0;
        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的用户ID格式");
        }
    }
}