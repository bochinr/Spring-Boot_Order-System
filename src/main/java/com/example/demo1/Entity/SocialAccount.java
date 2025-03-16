package com.example.demo1.Entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "social_accounts", uniqueConstraints = @UniqueConstraint(columnNames = { "platform_type",
        "platform_user_id" }))
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private user user;

    @Column(name = "platform_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlatformType platformType;

    @Column(name = "platform_user_id", nullable = false)
    private String platformUserId;

    @Column(name = "union_id")
    private String unionId;

    // 平台类型枚举
    public enum PlatformType {
        WECHAT,
        ALIPAY,
        QQ,
        WEIBO,
        GITHUB
    }

    // 确保getter和setter方法存在
    public user getUser() {
        return user;
    }

    public void setUser(user user) {
        this.user = user;
    }
}