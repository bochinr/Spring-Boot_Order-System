package com.example.demo1.service;

import com.example.demo1.Entity.user;
import com.example.demo1.dto.OAuth2UserInfo;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 通过第三方平台唯一标识查找用户
     * 
     * @param platform 平台类型（WECHAT/ALIPAY）
     * @param unionId  统一标识（UnionID）
     * @return 用户对象，如果不存在则返回 null
     */
    user findByUnionId(String platform, String unionId);

    /**
     * 根据第三方用户信息创建本地用户
     * 
     * @param userInfo 第三方用户信息
     * @return 创建的用户对象
     */
    user createUserFromOAuth2(OAuth2UserInfo userInfo);

    /**
     * 使用手机号和密码登录
     * 
     * @param phone    手机号
     * @param password 密码
     * @return 登录成功的用户对象
     */
    user loginByPhone(String phone, String password);

    /**
     * 使用邮箱和密码登录
     * 
     * @param email    邮箱
     * @param password 密码
     * @return 登录成功的用户对象
     */
    user loginByEmail(String email, String password);

    /**
     * 注册新用户
     * 
     * @param username 用户名
     * @param password 密码
     * @param phone    手机号
     * @param email    邮箱
     * @return 注册成功的用户对象
     */
    user registerUser(String username, String password, String phone, String email);

    /**
     * 根据用户ID获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    user getUserById(String userId);

    /**
     * 修改用户密码
     * 
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改是否成功
     */
    boolean changePassword(String userId, String oldPassword, String newPassword);
}