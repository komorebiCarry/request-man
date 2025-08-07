package com.ljh.request.requestman.constant;

/**
 * @author leijianhui
 * @Description Spring相关注解名称常量，便于统一管理和复用。
 * @date 2025/06/17 16:32
 */
public final class SpringAnnotationConstants {
    /**
     * GET请求注解
     */
    public static final String GET_MAPPING = "GetMapping";
    /**
     * POST请求注解
     */
    public static final String POST_MAPPING = "PostMapping";
    /**
     * PUT请求注解
     */
    public static final String PUT_MAPPING = "PutMapping";
    /**
     * DELETE请求注解
     */
    public static final String DELETE_MAPPING = "DeleteMapping";
    /**
     * PATCH请求注解
     */
    public static final String PATCH_MAPPING = "PatchMapping";
    /**
     * 通用请求注解
     */
    public static final String REQUEST_MAPPING = "RequestMapping";
    /**
     * Swagger注解：ApiOperation
     */
    public static final String API_OPERATION = "ApiOperation";
    /**
     * Swagger注解：Operation
     */
    public static final String OPERATION = "Operation";

    /**
     * 私有构造方法，禁止实例化该工具类。
     */
    private SpringAnnotationConstants() {
        throw new UnsupportedOperationException("SpringAnnotationConstants为常量类，禁止实例化");
    }
} 