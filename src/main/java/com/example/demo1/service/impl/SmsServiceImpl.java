package com.example.demo1.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.example.demo1.config.AliyunSmsConfig;
import com.example.demo1.exception.SmsSendException;
import com.example.demo1.service.SmsService;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE_TIME = 5; // 5分钟

    // 使用ConcurrentHashMap存储每个手机号的限流器
    private final Map<String, RateLimiter> phoneRateLimiters = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AliyunSmsConfig aliyunSmsConfig;

    @Autowired
    private Client smsClient;

    @Override
    public void sendSmsCode(String phone, String captcha) {
        // 手机号格式校验（图形验证码已经在控制器中验证过了）
        if (!isValidPhoneNumber(phone)) {
            throw new SmsSendException(400, "手机号格式不正确");
        }

        // 3. 限流检查
        RateLimiter rateLimiter = phoneRateLimiters.computeIfAbsent(phone, k -> RateLimiter.create(1.0 / 60.0)); // 每分钟最多1次
        if (!rateLimiter.tryAcquire()) {
            throw new SmsSendException(429, "发送过于频繁，请1分钟后再试");
        }

        // 4. 生成随机验证码
        String code = generateRandomCode(6);

        try {
            // 5. 调用阿里云SDK发送短信（此处仅模拟，实际写入日志）
            sendSms(phone, code);

            // 6. 将验证码保存到Redis，设置过期时间5分钟
            String key = SMS_CODE_PREFIX + phone;
            redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_TIME, TimeUnit.MINUTES);

            log.info("短信验证码发送成功 - 手机号: {}, 验证码: {}", phone, code);
        } catch (Exception e) {
            log.error("短信发送失败: {}", e.getMessage(), e);
            throw new SmsSendException("短信发送失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySmsCode(String phone, String code) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            return false;
        }

        String key = SMS_CODE_PREFIX + phone;
        Object savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode != null && code.equals(savedCode.toString())) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void sendSms(String phone, String code) throws Exception {
        // 实际项目中应该调用阿里云SDK发送短信
        // 这里我们只是模拟发送，将验证码输出到日志

        // 创建发送短信请求
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(aliyunSmsConfig.getSignName())
                .setTemplateCode(aliyunSmsConfig.getTemplateCode())
                .setTemplateParam("{\"code\":\"" + code + "\"}");

        // 在实际环境中取消注释下面的代码来发送短信
        // smsClient.sendSms(request);

        // 模拟发送成功，实际是写入日志
        log.info("模拟短信发送 - 手机号: {}, 验证码: {}, 模板: {}", phone, code, aliyunSmsConfig.getTemplateCode());
    }
}