package com.example.demo1.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录日志实体类
 */
@Data
@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Integer userId;

    /**
     * 登录类型
     */
    @Column(name = "login_type", length = 20)
    private String loginType;

    /**
     * 登录IP
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 登录设备信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 登录状态（成功/失败）
     */
    @Column(name = "status")
    private Boolean status;

    /**
     * 失败原因
     */
    @Column(name = "fail_reason", length = 255)
    private String failReason;

    /**
     * 登录时间
     */
    @Column(name = "login_time")
    private LocalDateTime loginTime;

    public LoginLog() {
        this.loginTime = LocalDateTime.now();
    }
}