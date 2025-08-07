package com.ljh.request.requestman.model;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author leijianhui
 * @Description 接口信息数据结构，包含url、方法、参数、请求体等。
 * @date 2025/06/17 16:12
 */
public class ApiInfo {
    /**
     * 接口注释/中文名
     */
    private final String name;

    /**
     * Java方法名
     */
    private final String methodName;

    /**
     * 接口URL
     */
    private final String url;

    /**
     * HTTP请求方式（GET/POST/PUT/DELETE等）
     */
    private final String httpMethod;

    /**
     * 参数信息列表
     */
    private final List<ApiParam> params;

    /**
     * 请求体参数信息列表（仅@ReqeustBody参数）
     */
    private final List<ApiParam> bodyParams;

    /**
     * 接口详细描述（如JavaDoc注释）
     */
    private final String description;

    /**
     * 响应参数信息列表（自动递归生成）
     */
    private final List<ApiParam> responseParams;

    /**
     * api参数，用于唯一校验，不递归
     */
    private final List<String> paramTypes;

    /**
     * 接口所属类名
     */
    private final String className;

    public ApiInfo(ApiInfo apiInfo) {
        this.name = apiInfo.name;
        this.methodName = apiInfo.methodName;
        this.url = apiInfo.url;
        this.httpMethod = apiInfo.httpMethod;
        this.params = apiInfo.params;
        this.bodyParams = apiInfo.bodyParams;
        this.paramTypes = apiInfo.paramTypes;
        this.description = apiInfo.description;
        this.responseParams = apiInfo.responseParams;
        this.className = apiInfo.className;
    }

    /**
     * 全字段构造方法
     *
     * @param name           接口注释/中文名
     * @param methodName     Java方法名
     * @param url            接口URL
     * @param httpMethod     HTTP请求方式
     * @param params         参数信息列表
     * @param bodyParams     请求体参数信息列表
     * @param description    接口详细描述
     * @param responseParams 响应参数信息列表
     * @param className      接口所属类名
     */
    public ApiInfo(String name, String methodName, String url, String httpMethod, List<ApiParam> params, List<ApiParam> bodyParams, List<String> paramTypes, String description, List<ApiParam> responseParams, String className) {
        this.name = name;
        this.methodName = methodName;
        this.url = url;
        this.httpMethod = httpMethod;
        this.params = params != null ? params : Collections.emptyList();
        this.bodyParams = bodyParams != null ? bodyParams : Collections.emptyList();
        this.paramTypes = paramTypes != null ? paramTypes : Collections.emptyList();
        this.description = description;
        this.responseParams = responseParams != null ? responseParams : Collections.emptyList();
        this.className = className;
    }

    /**
     * 简化构造方法（无参数、无描述）
     */
    public ApiInfo(String name, String methodName, String url, String httpMethod, String className) {
        this(name, methodName, url, httpMethod, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "", Collections.emptyList(), className);
    }


    public String getName() {
        return name;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getUrl() {
        return url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public List<ApiParam> getParams() {
        return params;
    }

    public List<ApiParam> getBodyParams() {
        return bodyParams;
    }

    public String getDescription() {
        return description;
    }

    public List<ApiParam> getResponseParams() {
        return responseParams;
    }

    public String getClassName() {
        return className;
    }


    /**
     * 获取参数类型全限定名列表（用于方法签名比对）
     *
     * @return 参数类型全限定名列表
     */
    public List<String> getParamTypes() {
        return paramTypes;
    }


    /**
     * 获取名称或者备注
     */
    public String getNameOrDescription() {

        return StringUtils.isBlank(name) ? description : name.matches("^[A-Za-z]+$") ? description : name;
    }


    /**
     * 用于UI弹窗展示的接口信息
     */
    public String getDisplayText() {
        return String.format("<html>%s&nbsp;&nbsp;%s&nbsp;&nbsp;<span style='color:#888888;'>%s#%s</span></html>",
                url != null ? url : "",
                description != null ? description : methodName != null ? methodName : "",
                className != null ? className : "",
                methodName != null ? methodName : "");
    }

    /**
     * 通过PsiMethod和注解快速构建ApiInfo对象
     *
     * @param method     方法对象
     * @param annotation 映射注解
     * @param className  接口所属类名
     * @return ApiInfo
     */
    public static ApiInfo fromMethod(PsiMethod method, PsiAnnotation annotation, String className) {
        String url = "";
        var value = annotation.findAttributeValue("value");
        if (value != null) {
            url = value.getText().replace("\"", "");
        }
        String qName = annotation.getQualifiedName();
        String httpMethod = "GET";
        if (qName != null) {
            int idx = qName.lastIndexOf('.');
            if (idx >= 0) {
                String shortName = qName.substring(idx + 1);
                if (shortName.equals("GetMapping")) {
                    httpMethod = "GET";
                } else if (shortName.equals("PostMapping")) {
                    httpMethod = "POST";
                } else if (shortName.equals("PutMapping")) {
                    httpMethod = "PUT";
                } else if (shortName.equals("DeleteMapping")) {
                    httpMethod = "DELETE";
                } else if (shortName.equals("PatchMapping")) {
                    httpMethod = "PATCH";
                } else if (shortName.equals("RequestMapping")) {
                    httpMethod = "REQUEST";
                }
            }
        }
        List<ApiParam> params = ApiInfoExtractor.extractRequestParams(method);
        // 解析接口注释/名称
        String name = method.getName();
        // 解析JavaDoc注释
        String description = "";
        for (PsiAnnotation ann : method.getAnnotations()) {
            String annQName = ann.getQualifiedName();
            if (annQName != null && (annQName.endsWith("ApiOperation") || annQName.endsWith("Operation"))) {
                var annValue = ann.findAttributeValue("value");
                if (annValue != null) {
                    name = annValue.getText().replaceAll("[\"{}]", "");
                } else {
                    var summary = ann.findAttributeValue("summary");
                    if (summary != null) {
                        name = summary.getText().replaceAll("[\"{}]", "");
                    }
                    var desc = ann.findAttributeValue("description");
                    if (desc != null) {
                        description = desc.getText().replaceAll("[\"{}]", "");
                    }
                }
            }
        }

        if (method.getDocComment() != null && !method.getDocComment().getText().isEmpty()) {
            description = method.getDocComment().getText();
        }
        List<ApiParam> bodyParams = ApiInfoExtractor.extractBodyParams(method, false);
        return new ApiInfo(name, method.getName(), url, httpMethod, params, bodyParams, ApiInfoExtractor.getParamTypes(method), description, Collections.emptyList(), className);
    }

    @Override
    public String toString() {
        // 优先显示接口注释（description），无则显示接口名称（name），并附带方法名
        String display = (description != null && !description.isEmpty()) ? description : name;
        return display + "（" + methodName + "）";
    }

} 