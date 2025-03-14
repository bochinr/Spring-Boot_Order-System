package com.example.demo1.Controller;
import com.example.demo1.Entity.food;
import com.example.demo1.Mapper.foodMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequestMapping("/food")
public class foodController {
    @Autowired
    foodMapper foodMp;

    @RequestMapping("list")
    public String list(Model model, HttpSession session) {
        List<food> foodList = foodMp.findAll();
        model.addAttribute("foodList", foodList);
        
        // 添加购物车信息到模型
        model.addAttribute("cart", session.getAttribute("cart"));
        model.addAttribute("cartTotal", session.getAttribute("cartTotal"));
        
        // 添加当前用户信息
        model.addAttribute("currentUser", session.getAttribute("currentUser"));
        return "foods";
    }

    @RequestMapping("detail")
    public String detail(@RequestParam String name, Model model) {
        food food = foodMp.findByName(name);
        if (food == null) {
            throw new RuntimeException("Food not found with name: " + name);
        }
        model.addAttribute("food", food);
        return "food";
    }
}

