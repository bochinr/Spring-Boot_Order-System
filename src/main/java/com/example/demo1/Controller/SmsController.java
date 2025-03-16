package com.example.demo1.Controller;

import com.example.demo1.dto.ApiResponse;
import com.example.demo1.exception.SmsSendException;
import com.example.demo1.service.SmsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 发送短信验证码
     * 
     * @param phone   手机号
     * @param captcha 图形验证码
     * @return 发送结果
     */
    @GetMapping("/code")
    public ApiResponse<Void> sendSmsCode(
            @RequestParam String phone,
            @RequestParam String captcha,
            HttpServletRequest request) {

        // 参数校验
        if (!StringUtils.hasText(phone)) {
            throw new SmsSendException(400, "手机号不能为空");
        }

        if (!StringUtils.hasText(captcha)) {
            throw new SmsSendException(400, "图形验证码不能为空");
        }

        // 获取会话ID
        String sessionId = request.getSession().getId();

        // 验证图形验证码
        String redisKey = CAPTCHA_PREFIX + sessionId + "_captcha";
        Object savedCaptcha = redisTemplate.opsForValue().get(redisKey);

        if (savedCaptcha == null) {
            throw new SmsSendException(400, "验证码已过期，请刷新验证码");
        }

        if (!captcha.equalsIgnoreCase(savedCaptcha.toString())) {
            throw new SmsSendException(400, "图形验证码错误");
        }

        // 验证通过后删除验证码，防止重复使用
        redisTemplate.delete(redisKey);

        // 发送短信验证码
        smsService.sendSmsCode(phone, captcha);

        return ApiResponse.success("发送成功");
    }
}