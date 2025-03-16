package com.example.demo1.exception;

/**
 * 短信发送异常类
 */
public class SmsSendException extends RuntimeException {
    private int code;
    private String message;

    public SmsSendException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public SmsSendException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}