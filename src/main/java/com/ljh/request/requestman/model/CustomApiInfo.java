package com.ljh.request.requestman.model;

import com.ljh.request.requestman.ui.CookiesPanel;
import com.ljh.request.requestman.ui.HeadersPanel;
import com.ljh.request.requestman.ui.PostOpPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author leijianhui
 * @Description 自定义接口信息数据结构，支持持久化和多种参数类型。
 * @date 2025/06/19 09:36
 */
public class CustomApiInfo implements Serializable {

    private static final long serialVersionUID = 6664074267456260461L;
    /**
     * 接口名称
     */
    private String name;
    /**
     * 请求URL
     */
    private String url;
    /**
     * HTTP方法
     */
    private String httpMethod;
    /**
     * 参数列表
     */
    private List<ApiParam> params;
    /**
     * 请求体（可选，支持JSON等）
     */
    private String body;
    /**
     * 接口描述
     */
    private String description;
    /**
     * 后置操作列表
     */
    private List<PostOpPanel.PostOpItem> postOps;
    /**
     * 请求体类型（如json、form-data等）
     */
    private String bodyType;
    /**
     * 请求体参数（form-data、urlencoded等结构化参数）
     */
    private List<ApiParam> bodyParams;
    /**
     * 认证模式（0=继承，1=自定义）
     */
    private int authMode = 0;
    /**
     * 自定义认证内容
     */
    private String authValue = "";
    /**
     * 请求头参数
     */
    private List<HeadersPanel.HeaderItem> headers = new ArrayList<>();
    /**
     * cookie
     */
    private List<CookiesPanel.CookieItem> cookieItems = new ArrayList<>();


    public CustomApiInfo() {
    }

    public CustomApiInfo(String name, String url, String httpMethod, List<ApiParam> params, String body, String description) {
        this(name, url, httpMethod, params, body, description, new ArrayList<>());
    }

    public CustomApiInfo(String name, String url, String httpMethod, List<ApiParam> params, String body, String description, List<PostOpPanel.PostOpItem> postOps) {
        this.name = name;
        this.url = url;
        this.httpMethod = httpMethod;
        this.params = params;
        this.body = body;
        this.description = description;
        this.postOps = postOps;
    }

    public List<CookiesPanel.CookieItem> getCookieItems() {
        return cookieItems;
    }

    public void setCookieItems(List<CookiesPanel.CookieItem> cookieItems) {
        this.cookieItems = cookieItems;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<ApiParam> getParams() {
        return params;
    }

    public void setParams(List<ApiParam> params) {
        this.params = params;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PostOpPanel.PostOpItem> getPostOps() {
        return postOps;
    }

    public void setPostOps(List<PostOpPanel.PostOpItem> postOps) {
        this.postOps = postOps;
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

    public int getAuthMode() {
        return authMode;
    }

    public void setAuthMode(int authMode) {
        this.authMode = authMode;
    }

    public String getAuthValue() {
        return authValue;
    }

    public void setAuthValue(String authValue) {
        this.authValue = authValue;
    }

    public List<com.ljh.request.requestman.ui.HeadersPanel.HeaderItem> getHeaders() {
        return headers;
    }

    public void setHeaders(List<com.ljh.request.requestman.ui.HeadersPanel.HeaderItem> headers) {
        this.headers = headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomApiInfo that = (CustomApiInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(url, that.url) && Objects.equals(httpMethod, that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, httpMethod);
    }

    @Override
    public String toString() {
        return name + " [" + httpMethod + "] " + url;
    }
} 