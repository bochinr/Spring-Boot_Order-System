package com.example.demo1.exception;

/**
 * OAuth2授权异常
 */
public class OAuth2Exception extends RuntimeException {

    private int code;

    public OAuth2Exception(String message) {
        super(message);
        this.code = 401;
    }

    public OAuth2Exception(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}