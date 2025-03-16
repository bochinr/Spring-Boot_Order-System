package com.example.demo1.service.impl;

import com.example.demo1.Entity.LoginLog;
import com.example.demo1.repository.LoginLogRepository;
import com.example.demo1.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Override
    public LoginLog recordLoginSuccess(Integer userId, String loginType, String ipAddress, String userAgent) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setLoginType(loginType);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(true);
        return loginLogRepository.save(loginLog);
    }

    @Override
    public LoginLog recordLoginFailure(String loginType, String ipAddress, String userAgent, String failReason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setLoginType(loginType);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(false);
        loginLog.setFailReason(failReason);
        return loginLogRepository.save(loginLog);
    }
}