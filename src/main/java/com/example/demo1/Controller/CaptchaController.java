package com.example.demo1.Controller;

import com.example.demo1.dto.ApiResponse;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private DefaultKaptcha captchaProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_TIME = 5; // 5分钟过期

    @GetMapping
    public ApiResponse<Map<String, String>> getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 设置响应头，禁止缓存
            setNoCacheHeaders(response);

            // 生成验证码文本
            String captchaText = captchaProducer.createText();

            // 使用会话ID作为标识
            String sessionId = request.getSession().getId();
            // 也可以使用UUID生成唯一标识，前端需要保存这个值
            String captchaKey = UUID.randomUUID().toString();

            // 保存验证码到Redis
            String redisKey = CAPTCHA_PREFIX + sessionId + "_captcha";
            redisTemplate.opsForValue().set(redisKey, captchaText, CAPTCHA_EXPIRE_TIME, TimeUnit.MINUTES);

            // 生成验证码图片
            BufferedImage captchaImage = captchaProducer.createImage(captchaText);

            // 转换图片为Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(captchaImage, "png", outputStream);
            byte[] captchaBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(captchaBytes);

            // 构建响应数据
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("captchaKey", sessionId); // 告诉前端用会话ID作为key
            resultMap.put("captchaImage", "data:image/png;base64," + base64Image);

            return ApiResponse.success("验证码生成成功", resultMap);

        } catch (Exception e) {
            return ApiResponse.error(500, "验证码生成失败: " + e.getMessage());
        }
    }

    /**
     * 设置HTTP响应头，禁止缓存
     */
    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}