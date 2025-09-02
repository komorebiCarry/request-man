package com.ljh.request.requestman.search;

import java.util.Collections;
import java.util.List;

/**
 * API搜索条目数据结构，用于在搜索弹窗中展示API信息。
 * 包含API的基本信息如URL、HTTP方法、方法名、类名、描述和参数类型等。
 *
 * @author leijianhui
 * @Description API搜索条目数据结构，用于在搜索弹窗中展示API信息。
 * @date 2025/06/19 21:00
 */
public class ApiSearchEntry {
    
    /**
     * API的URL路径
     */
    final String url;
    
    /**
     * HTTP请求方法
     */
    final String httpMethod;
    
    /**
     * 方法名称
     */
    final String methodName;
    
    /**
     * 类名称
     */
    final String className;
    
    /**
     * API描述信息
     */
    final String description;
    
    /**
     * 方法参数类型列表
     */
    final List<String> paramTypes;
    
    /**
     * 创建时间戳
     */
    final long timestamp;

    /**
     * 构造函数
     *
     * @param url         API的URL路径
     * @param httpMethod  HTTP请求方法
     * @param methodName  方法名称
     * @param className   类名称
     * @param description API描述信息
     * @param paramTypes  方法参数类型列表
     */
    public ApiSearchEntry(String url, String httpMethod, String methodName, String className, String description, List<String> paramTypes) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.methodName = methodName;
        this.className = className;
        this.description = description;
        this.paramTypes = paramTypes != null ? paramTypes : Collections.emptyList();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取显示文本，用于UI展示
     *
     * @return 格式化的HTML显示文本
     */
    public String getDisplayText() {
        return String.format("<html>%s&nbsp;&nbsp;%s&nbsp;&nbsp;<span style='color:#888888;'>%s#%s</span></html>",
                url != null ? url : "",
                description != null && !description.isEmpty() ? description : (methodName != null ? methodName : ""),
                className != null ? className : "",
                methodName != null ? methodName : "");
    }

    @Override
    public String toString() {
        return (methodName != null ? methodName : "") + "（" + (url != null ? url : "") + ")";
    }
    
    /**
     * 获取URL
     *
     * @return URL字符串
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * 获取HTTP方法
     *
     * @return HTTP方法字符串
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    
    /**
     * 获取方法名称
     *
     * @return 方法名称字符串
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 获取类名称
     *
     * @return 类名称字符串
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * 获取描述信息
     *
     * @return 描述信息字符串
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取参数类型列表
     *
     * @return 参数类型列表
     */
    public List<String> getParamTypes() {
        return paramTypes;
    }
    
    /**
     * 获取创建时间戳
     *
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
}
