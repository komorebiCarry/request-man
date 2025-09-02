package com.ljh.request.requestman.util;

import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;

import java.util.List;
import java.util.Map;

/**
 * 请求参数构建器，用于收集和构建请求参数。
 * 该类封装了HTTP请求所需的所有参数信息，包括URL、方法、参数、请求体等。
 *
 * @author leijianhui
 * @Description 请求参数构建器，封装HTTP请求所需的所有参数信息
 * @date 2025/01/27 10:35
 */
public class RequestParams {
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * HTTP请求方法
     */
    private String method;
    
    /**
     * 请求参数列表
     */
    private List<ApiParam> params;
    
    /**
     * 请求体类型
     */
    private String bodyType;
    
    /**
     * 请求体参数列表
     */
    private List<ApiParam> bodyParams;
    
    /**
     * 请求体内容
     */
    private String bodyContent;
    
    /**
     * 二进制数据
     */
    private byte[] binaryData;
    
    /**
     * 请求头映射
     */
    private Map<String, String> headers;
    
    /**
     * Cookie映射
     */
    private Map<String, String> cookies;
    
    /**
     * 认证信息
     */
    private String auth;
    
    /**
     * 后置操作列表
     */
    private List<PostOpItem> postOps;
    
    /**
     * URL前缀
     */
    private String urlPrefix;

    /**
     * 默认构造函数
     */
    public RequestParams() {
        // 初始化默认值
        this.bodyType = "none";
        this.bodyContent = "";
    }

    // Getters and Setters
    public String getUrl() { 
        return url; 
    }
    
    public void setUrl(String url) { 
        this.url = url; 
    }
    
    public String getMethod() { 
        return method; 
    }
    
    public void setMethod(String method) { 
        this.method = method; 
    }
    
    public List<ApiParam> getParams() { 
        return params; 
    }
    
    public void setParams(List<ApiParam> params) { 
        this.params = params; 
    }
    
    public String getBodyType() { 
        return bodyType; 
    }
    
    public void setBodyType(String bodyType) { 
        this.bodyType = bodyType; 
    }
    
    public List<ApiParam> getBodyParams() { 
        return bodyParams; 
    }
    
    public void setBodyParams(List<ApiParam> bodyParams) { 
        this.bodyParams = bodyParams; 
    }
    
    public String getBodyContent() { 
        return bodyContent; 
    }
    
    public void setBodyContent(String bodyContent) { 
        this.bodyContent = bodyContent; 
    }
    
    public byte[] getBinaryData() { 
        return binaryData; 
    }
    
    public void setBinaryData(byte[] binaryData) { 
        this.binaryData = binaryData; 
    }
    
    public Map<String, String> getHeaders() { 
        return headers; 
    }
    
    public void setHeaders(Map<String, String> headers) { 
        this.headers = headers; 
    }
    
    public Map<String, String> getCookies() { 
        return cookies; 
    }
    
    public void setCookies(Map<String, String> cookies) { 
        this.cookies = cookies; 
    }
    
    public String getAuth() { 
        return auth; 
    }
    
    public void setAuth(String auth) { 
        this.auth = auth; 
    }
    
    public List<PostOpItem> getPostOps() { 
        return postOps; 
    }
    
    public void setPostOps(List<PostOpItem> postOps) { 
        this.postOps = postOps; 
    }
    
    public String getUrlPrefix() { 
        return urlPrefix; 
    }
    
    public void setUrlPrefix(String urlPrefix) { 
        this.urlPrefix = urlPrefix; 
    }

    @Override
    public String toString() {
        return "RequestParams{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", bodyType='" + bodyType + '\'' +
                ", urlPrefix='" + urlPrefix + '\'' +
                '}';
    }
}

