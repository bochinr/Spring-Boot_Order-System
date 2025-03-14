package com.example.demo1;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public class GlobalInterceptor implements HandlerInterceptor {
    // ... 其他方法实现 ...
    // 在请求处理之前调用
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在这里可以添加你的业务逻辑，比如权限校验等
        // 返回true表示继续流程，返回false表示中断请求
//        String token= (String)request.getSession().getAttribute("token");
        String token= getCookie(request,"token");
        if(token==null|| token.isEmpty())
            return  false;
        Map<String,Object> mp= JwtUtil.parseToken(token);
        if(mp==null)
            return false;
       // System.out.println(request);
       // System.out.println(response);
        return true; // 继续流程
    }
    static String getCookie(HttpServletRequest request,String key)
    {
        Cookie[] cookies = request.getCookies();
        if(cookies==null)
            return null;
        for(Cookie ck: cookies)
        {
            if(key.equals(ck.getName()))
                return ck.getValue();
        }
        return null;
    }


    // 在请求处理之后，视图渲染之前调用，此时可以修改ModelAndView等数据结构
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 这里可以添加一些后处理的逻辑
    }

    // 在整个请求结束之后调用，主要用于清理资源等操作
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 这里可以添加一些清理资源的逻辑
    }
}