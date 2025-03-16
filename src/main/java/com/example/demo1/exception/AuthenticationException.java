package com.example.demo1.exception;

/**
 * 认证异常
 */
public class AuthenticationException extends RuntimeException {

    private int code;

    public AuthenticationException(String message) {
        super(message);
        this.code = 401;
    }

    public AuthenticationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}