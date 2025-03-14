package com.example.demo1.Controller;

import com.example.demo1.Entity.user;
import com.example.demo1.JwtUtil;
import com.example.demo1.Mapper.userMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/user")
public class userController {
    @Autowired
    private userMapper userMp;

    @PostMapping("/login")
    public String login(String name, String psw, Model model, HttpSession session, HttpServletResponse response) {
        user user = userMp.findByname(name);
        if (user == null) {
            model.addAttribute("info", "用户不存在");
            model.addAttribute("name", name);
            return "fail";
        }
        if (!user.getPassword().equals(psw)) {
            model.addAttribute("info", "密码错误");
            model.addAttribute("name", name);
            return "fail";
        }
        
        // 生成token并设置到Cookie中
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", name);
        String token = JwtUtil.generateToken(claims);
        Cookie cookie = new Cookie("token", token);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        session.setAttribute("currentUser", name);
        return "redirect:/food/list";
    }

    @GetMapping("/toRegister")
    public String toRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String register(String username, String password, int age, Model model) {
        user existingUser = userMp.findByname(username);
        if (existingUser != null) {
            model.addAttribute("info", "用户名已存在");
            model.addAttribute("name", username);
            return "fail";
        }
        userMp.register(username, password, age);
        return "redirect:/index.html";
    }

    @GetMapping("/listAll")
    public String listAll(Model model, HttpSession session) {
        try {
            // 确保用户已登录
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "redirect:/index.html";
            }

            // 获取所有用户
            List<user> users = userMp.findAll();
            if (users == null || users.isEmpty()) {
                model.addAttribute("message", "没有找到任何用户");
            }
            
            // 添加数据到模型
            model.addAttribute("users", users);
            model.addAttribute("currentUser", currentUser);
            return "userList";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "获取用户列表时发生错误：" + e.getMessage());
            return "userList";
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody Map<String, Integer> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer userId = params.get("id");
            if (userId == null) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            userMp.deleteById(userId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/search")
    public String search(@RequestParam String searchName, Model model, HttpSession session) {
        // 确保用户已登录
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/index.html";
        }

        // 搜索用户
        user foundUser = userMp.findByname(searchName);
        List<user> users = new ArrayList<>();
        if (foundUser != null) {
            users.add(foundUser);
        }

        // 添加数据到模型
        model.addAttribute("users", users);
        model.addAttribute("currentUser", currentUser);
        if (users.isEmpty()) {
            model.addAttribute("message", "未找到用户：" + searchName);
        }
        return "userList";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam String name, Model model, HttpSession session) {
        // 确保用户已登录
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/index.html";
        }

        // 获取用户详情
        user userDetail = userMp.findByname(name);
        if (userDetail == null) {
            model.addAttribute("message", "未找到用户：" + name);
            return "userDetail";
        }

        // 添加数据到模型
        model.addAttribute("user", userDetail);
        model.addAttribute("currentUser", currentUser);
        return "userDetail";
    }
} 