package com.example.demo1.Controller;

import com.example.demo1.Entity.CartItem;
import com.example.demo1.Entity.food;
import com.example.demo1.Mapper.foodMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private foodMapper foodMp;

    @PostMapping("/add")
    public Map<String, Object> addToCart(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String foodName = request.get("name");
        
        // 从数据库获取食品信息
        food foodItem = foodMp.findByName(foodName);
        if (foodItem == null) {
            response.put("success", false);
            return response;
        }

        // 获取或创建购物车
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
        }

        // 检查商品是否已在购物车中
        boolean found = false;
        for (CartItem item : cart) {
            if (item.getName().equals(foodName)) {
                item.setQuantity(item.getQuantity() + 1);
                found = true;
                break;
            }
        }

        // 如果是新商品，添加到购物车
        if (!found) {
            cart.add(new CartItem(foodName, 1, foodItem.getPrice()));
        }

        // 更新购物车总金额
        updateCartTotal(session, cart);

        // 保存购物车到会话
        session.setAttribute("cart", cart);
        
        response.put("success", true);
        return response;
    }

    @PostMapping("/remove")
    public Map<String, Object> removeFromCart(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String foodName = request.get("name");
        
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart != null) {
            cart.removeIf(item -> item.getName().equals(foodName));
            updateCartTotal(session, cart);
            session.setAttribute("cart", cart);
        }
        
        response.put("success", true);
        return response;
    }

    @PostMapping("/update")
    public Map<String, Object> updateQuantity(@RequestBody Map<String, Object> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String foodName = (String) request.get("name");
        int change = (int) request.get("change");
        
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart != null) {
            for (CartItem item : cart) {
                if (item.getName().equals(foodName)) {
                    int newQuantity = item.getQuantity() + change;
                    if (newQuantity > 0) {
                        item.setQuantity(newQuantity);
                    } else {
                        cart.remove(item);
                    }
                    break;
                }
            }
            updateCartTotal(session, cart);
            session.setAttribute("cart", cart);
        }
        
        response.put("success", true);
        return response;
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 清空购物车
        session.removeAttribute("cart");
        session.removeAttribute("cartTotal");
        
        response.put("success", true);
        return response;
    }

    private void updateCartTotal(HttpSession session, List<CartItem> cart) {
        double total = 0;
        for (CartItem item : cart) {
            total += item.getPrice() * item.getQuantity();
        }
        session.setAttribute("cartTotal", total);
    }
} 