package com.example.demo1.auth.strategy;

import com.example.demo1.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录策略工厂，根据登录类型获取对应的登录策略
 */
@Component
public class LoginStrategyFactory {

    private final Map<String, LoginStrategy> strategyMap = new HashMap<>();

    /**
     * 注入所有登录策略实现，并初始化策略映射
     */
    @Autowired
    public LoginStrategyFactory(List<LoginStrategy> strategies) {
        strategies.forEach(strategy -> strategyMap.put(strategy.getType(), strategy));
    }

    /**
     * 根据登录类型获取登录策略
     * 
     * @param loginType 登录类型
     * @return 登录策略
     * @throws AuthenticationException 如果找不到对应的策略
     */
    public LoginStrategy getStrategy(String loginType) {
        LoginStrategy strategy = strategyMap.get(loginType);
        if (strategy == null) {
            throw new AuthenticationException("不支持的登录类型: " + loginType);
        }
        return strategy;
    }
}