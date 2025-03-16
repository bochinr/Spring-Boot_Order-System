package com.example.demo1.repository;

import com.example.demo1.Entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 登录日志数据访问接口
 */
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

}