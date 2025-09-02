package com.ljh.request.requestman.enums;

import com.ljh.request.requestman.util.RequestManBundle;

/**
 * 参数位置枚举
 *
 * @author leijianhui
 * @Description 定义参数在请求中的位置类型
 * @date 2025/01/27 15:30
 */
public enum ParamLocation {
    /**
     * 路径参数，如 /api/users/{id}
     */
    PATH("Path", "param.location.path"),
    
    /**
     * 查询参数，如 /api/users?page=1&size=10
     */
    QUERY("Query", "param.location.query");
    
    private final String code;
    private final String descriptionKey;
    
    ParamLocation(String code, String descriptionKey) {
        this.code = code;
        this.descriptionKey = descriptionKey;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return RequestManBundle.message(descriptionKey);
    }
    
    @Override
    public String toString() {
        return code;
    }
}
