package com.example.demo1.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 */
@Controller
public class PageController {

    /**
     * 显示第三方登录页面
     */
    @GetMapping("/oauth-login")
    public String oauthLogin() {
        return "oauth-login";
    }
}