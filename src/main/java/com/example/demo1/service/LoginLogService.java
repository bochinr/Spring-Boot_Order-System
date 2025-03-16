package com.example.demo1.service;

import com.example.demo1.Entity.LoginLog;

/**
 * 登录日志服务接口
 */
public interface LoginLogService {

    /**
     * 记录登录成功日志
     * 
     * @param userId    用户ID
     * @param loginType 登录类型
     * @param ipAddress IP地址
     * @param userAgent 用户设备信息
     * @return 保存的日志记录
     */
    LoginLog recordLoginSuccess(Integer userId, String loginType, String ipAddress, String userAgent);

    /**
     * 记录登录失败日志
     * 
     * @param loginType  登录类型
     * @param ipAddress  IP地址
     * @param userAgent  用户设备信息
     * @param failReason 失败原因
     * @return 保存的日志记录
     */
    LoginLog recordLoginFailure(String loginType, String ipAddress, String userAgent, String failReason);
}