package com.example.demo1.Controller;

import com.example.demo1.Entity.user;
import com.example.demo1.JwtUtil;
import com.example.demo1.Mapper.userMapper;
import com.example.demo1.auth.JwtTokenProvider;
import com.example.demo1.security.JwtBlacklistService;
import com.example.demo1.security.RequireVerification;
import com.example.demo1.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 用户相关操作控制器
 */
@Controller
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private userMapper userMp;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    @Autowired
    private UserService userService;

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

    /**
     * 修改密码（需要二次验证）
     * 
     * @param request     HTTP请求
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    @RequireVerification("修改密码")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request) {

        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            // 获取用户ID
            String token = getTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未授权，请先登录"));
            }

            Integer userIdInt = jwtTokenProvider.getUserIdFromToken(token);
            String userId = String.valueOf(userIdInt);

            // 修改密码
            boolean success = userService.changePassword(userId, oldPassword, newPassword);

            if (success) {
                // 密码修改成功，将当前token加入黑名单，强制用户重新登录
                jwtBlacklistService.addToBlacklist(token);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "密码修改成功，请重新登录");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("密码修改失败，旧密码不匹配"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("密码修改失败: " + e.getMessage()));
        }
    }

    /**
     * 注销登录（将当前token加入黑名单）
     * 
     * @param request HTTP请求
     * @return 注销结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.ok(createSuccessResponse("已注销登录"));
            }

            // 将token加入黑名单
            jwtBlacklistService.addToBlacklist(token);

            return ResponseEntity.ok(createSuccessResponse("已注销登录"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("注销失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户信息
     * 
     * @param request HTTP请求
     * @return 用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestBody Map<String, String> request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未授权，请先登录"));
            }

            Integer userIdInt = jwtTokenProvider.getUserIdFromToken(token);
            String userId = String.valueOf(userIdInt);
            user userInfo = userService.getUserById(userId);

            if (userInfo == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("用户不存在"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 从请求中提取JWT token
     */
    private String getTokenFromRequest(Map<String, String> request) {
        String bearerToken = request.get("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}