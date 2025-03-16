package com.example.demo1.repository;

import com.example.demo1.Entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 社交账号数据访问接口
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 根据平台类型和平台用户ID查找社交账号
     * 
     * @param platformType   平台类型
     * @param platformUserId 平台用户ID
     * @return 社交账号
     */
    SocialAccount findByPlatformTypeAndPlatformUserId(String platformType, String platformUserId);
}