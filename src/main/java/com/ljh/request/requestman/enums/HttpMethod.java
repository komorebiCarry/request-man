package com.ljh.request.requestman.enums;

/**
 * @author leijianhui
 * @Description HTTP请求方式枚举，便于类型安全和扩展。
 * @date 2025/06/17 16:32
 */
public enum HttpMethod {
    /**
     * GET请求
     */
    GET,
    /**
     * POST请求
     */
    POST,
    /**
     * PUT请求
     */
    PUT,
    /**
     * DELETE请求
     */
    DELETE,
    /**
     * PATCH请求
     */
    PATCH,
    /**
     * 通用请求
     */
    REQUEST,
    /**
     * 未知请求类型
     */
    UNKNOWN;
} 