package com.ljh.request.requestman.model;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.ljh.request.requestman.ui.CookiesPanel;
import com.ljh.request.requestman.ui.HeadersPanel;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import com.ljh.request.requestman.ui.PostOpPanel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
    private String name;

    /**
     * Java方法名
     */
    private String methodName;

    /**
     * 接口URL
     */
    private String url;

    /**
     * HTTP请求方式（GET/POST/PUT/DELETE等）
     */
    private String httpMethod;

    /**
     * 参数信息列表
     */
    private List<ApiParam> params;

    /**
     * 请求体参数信息列表（仅@ReqeustBody参数）
     */
    private List<ApiParam> bodyParams;

    /**
     * 接口详细描述（如JavaDoc注释）
     */
    private String description;

    /**
     * 响应参数信息列表（自动递归生成）
     */
    private List<ApiParam> responseParams;

    /**
     * api参数，用于唯一校验，不递归
     */
    private List<String> paramTypes;

    /**
     * 接口所属类名
     */
    private String className;

    /**
     * 请求头参数
     */
    private List<HeadersPanel.HeaderItem> headers = new ArrayList<>();
    /**
     * cookie
     */
    private List<CookiesPanel.CookieItem> cookieItems = new ArrayList<>();
    /**
     * 认证模式（0=继承，1=自定义）
     */
    private int authMode = 0;

    /**
     * 认证值
     */
    private String authValue = "";

    /**
     * 请求体类型（如json、form-data等） 冗余
     */
    private String bodyType;

    /**
     * 请求体（可选，支持JSON等） 冗余
     */
    private String body;

    /**
     * 后置操作列表
     */
    private List<PostOpPanel.PostOpItem> postOps = new ArrayList<>();

    public List<CookiesPanel.CookieItem> getCookieItems() {
        return cookieItems;
    }

    public void setCookieItems(List<CookiesPanel.CookieItem> cookieItems) {
        this.cookieItems = cookieItems;
    }

    public List<HeadersPanel.HeaderItem> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeadersPanel.HeaderItem> headers) {
        this.headers = headers;
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

    public List<PostOpPanel.PostOpItem> getPostOps() {
        return postOps;
    }

    public void setPostOps(List<PostOpPanel.PostOpItem> postOps) {
        this.postOps = postOps;
    }

    public void setParams(List<ApiParam> params) {
        this.params = params;
    }

    public void setBodyParams(List<ApiParam> bodyParams) {
        this.bodyParams = bodyParams;
    }

    public void setParamTypes(List<String> paramTypes) {
        this.paramTypes = paramTypes;
    }


    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyType() {
        return bodyType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    //---------- 基础字段


    public void setName(String name) {
        this.name = name;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setResponseParams(List<ApiParam> responseParams) {
        this.responseParams = responseParams;
    }

    public void setClassName(String className) {
        this.className = className;
    }

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
        this.headers = apiInfo.headers;
        this.cookieItems = apiInfo.cookieItems;
        this.authMode = apiInfo.authMode;
        this.authValue = apiInfo.authValue;
        this.postOps = apiInfo.postOps;
        this.bodyType = guessDefaultBodyType(apiInfo.bodyParams);
        if (!StringUtils.equalsAny(this.bodyType, "form-data", "x-www-form-urlencoded")) {
            this.body = ApiInfoExtractor.getApiInfoBodyJson(apiInfo.bodyParams);
        } else {
            this.body = "";
        }
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
        this.bodyType = guessDefaultBodyType(this.bodyParams);
        if (!StringUtils.equalsAny(this.bodyType, "form-data", "x-www-form-urlencoded")) {
            this.body = ApiInfoExtractor.getApiInfoBodyJson(this.bodyParams);
        } else {
            this.body = "";
        }
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

    /**
     * 根据body参数类型自动推断默认Body类型。
     * 优先考虑Spring注解的consumes属性，其次根据参数类型特征推断。
     *
     * @param bodyParamList 请求体参数列表
     * @return 默认Body类型
     */
    public static String guessDefaultBodyType(List<ApiParam> bodyParamList) {
        if (bodyParamList == null || bodyParamList.isEmpty()) {
            return "none";
        }

        // 1. 优先检查是否有明确的Content-Type注解信息
        for (ApiParam param : bodyParamList) {
            String contentType = param.getContentType();
            if (contentType != null && !contentType.trim().isEmpty()) {
                return mapContentTypeToBodyType(contentType);
            }
        }

        // 2. 检查是否有文件类型参数
        boolean hasFile = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            return dataTypeName.equals("file") ||
                    rawType.contains("multipartfile") ||
                    rawType.contains("file") ||
                    rawType.contains("inputstream") ||
                    rawType.contains("byte[]");
        });

        if (hasFile) {
            return "form-data";
        }

        // 3. 检查是否有二进制数据
        boolean hasBinary = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            return dataTypeName.equals("binary") ||
                    rawType.contains("byte[]") ||
                    rawType.contains("bytebuffer") ||
                    rawType.contains("inputstream") ||
                    rawType.contains("outputstream") ||
                    rawType.contains("bufferedimage");
            // 注意：移除了 rawType.contains("multipartfile")，因为MultipartFile应该优先识别为文件类型
        });

        if (hasBinary) {
            return "binary";
        }

        // 4. 检查是否有XML相关类型
        boolean hasXml = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            String description = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
            return dataTypeName.equals("xml") ||
                    rawType.contains("document") ||
                    rawType.contains("element") ||
                    rawType.contains("node") ||
                    description.contains("xml") ||
                    description.contains("soap");
        });

        if (hasXml) {
            return "xml";
        }

        // 7. 分析参数类型特征
        boolean hasComplexObject = false;
        boolean hasBasicType = false;
        boolean hasArray = false;
        boolean hasString = false;

        for (ApiParam param : bodyParamList) {
            if (param.getDataType() == null) {
                continue;
            }

            String dataTypeName = param.getDataType().name().toLowerCase();
            String rawType = param.getRawType() != null ? param.getRawType().toLowerCase() : "";

            // 检查是否是字符串类型
            if (dataTypeName.equals("string") || rawType.equals("java.lang.string")) {
                hasString = true;
            }
            // 检查是否是基本类型
            else if (isBasicType(dataTypeName, rawType)) {
                hasBasicType = true;
            }
            // 检查是否是数组
            else if (dataTypeName.equals("array") || rawType.contains("[]") ||
                    rawType.contains("list") || rawType.contains("set")) {
                hasArray = true;
            }
            // 检查是否是复杂对象
            else if (isComplexObject(dataTypeName, rawType)) {
                hasComplexObject = true;
            }
        }

        // 8. 根据参数特征推断Body类型
        if (hasComplexObject) {
            // 复杂对象通常使用JSON
            return "json";
        } else if (hasArray && !hasBasicType) {
            // 对象数组通常使用JSON
            return "json";
        } else if (hasBasicType && !hasComplexObject && !hasArray) {
            // 只有基本类型参数，可以使用form-urlencoded
            return "x-www-form-urlencoded";
        } else if (hasBasicType && hasArray) {
            // 基本类型+数组，使用JSON更合适
            return "json";
        } else if (hasString && bodyParamList.size() == 1) {
            // 单个字符串参数，可能是JSON字符串
            return "json";
        }

        // 9. 默认使用JSON（最通用的选择）
        return "json";
    }

    /**
     * 将Content-Type映射到Body类型
     *
     * @param contentType Content-Type字符串
     * @return 对应的Body类型
     */
    private static String mapContentTypeToBodyType(String contentType) {
        if (contentType == null) {
            return "json";
        }

        String lowerContentType = contentType.toLowerCase();

        // JSON相关
        if (lowerContentType.contains("application/json") ||
                lowerContentType.contains("text/json")) {
            return "json";
        }

        // XML相关
        if (lowerContentType.contains("application/xml") ||
                lowerContentType.contains("text/xml") ||
                lowerContentType.contains("application/soap+xml")) {
            return "xml";
        }

        // 表单相关
        if (lowerContentType.contains("application/x-www-form-urlencoded")) {
            return "x-www-form-urlencoded";
        }
        if (lowerContentType.contains("multipart/form-data")) {
            return "form-data";
        }

        // 文本相关 - 根据内容类型分配到合适的类型
        if (lowerContentType.contains("text/plain")) {
            return "json"; // 纯文本通常作为JSON字符串处理
        }
        if (lowerContentType.contains("text/html")) {
            return "xml"; // HTML作为XML处理
        }
        if (lowerContentType.contains("text/css")) {
            return "json"; // CSS作为JSON字符串处理
        }
        if (lowerContentType.contains("text/javascript")) {
            return "json"; // JavaScript作为JSON字符串处理
        }

        // 二进制相关
        if (lowerContentType.contains("application/octet-stream") ||
                lowerContentType.contains("application/pdf") ||
                lowerContentType.contains("image/") ||
                lowerContentType.contains("audio/") ||
                lowerContentType.contains("video/") ||
                lowerContentType.contains("application/zip") ||
                lowerContentType.contains("application/rar") ||
                lowerContentType.contains("application/excel") ||
                lowerContentType.contains("application/msword") ||
                lowerContentType.contains("application/protobuf")) {
            return "binary";
        }

        // 其他特殊类型
        if (lowerContentType.contains("application/yaml") ||
                lowerContentType.contains("text/yaml")) {
            return "json"; // YAML作为JSON处理
        }

        if (lowerContentType.contains("application/protobuf") ||
                lowerContentType.contains("application/x-protobuf")) {
            return "binary"; // Protocol Buffers作为二进制处理
        }

        // 默认返回JSON（最通用的选择）
        return "json";
    }

    /**
     * 判断是否是基本类型
     *
     * @param dataTypeName 数据类型名称
     * @param rawType      原始类型
     * @return 是否是基本类型
     */
    private static boolean isBasicType(String dataTypeName, String rawType) {
        return dataTypeName.equals("string") ||
                dataTypeName.equals("integer") ||
                dataTypeName.equals("number") ||
                dataTypeName.equals("boolean") ||
                rawType.equals("java.lang.string") ||
                rawType.equals("java.lang.integer") ||
                rawType.equals("java.lang.long") ||
                rawType.equals("java.lang.double") ||
                rawType.equals("java.lang.float") ||
                rawType.equals("java.lang.boolean") ||
                rawType.equals("int") ||
                rawType.equals("long") ||
                rawType.equals("double") ||
                rawType.equals("float") ||
                rawType.equals("boolean");
    }

    /**
     * 判断是否是复杂对象
     *
     * @param dataTypeName 数据类型名称
     * @param rawType      原始类型
     * @return 是否是复杂对象
     */
    private static boolean isComplexObject(String dataTypeName, String rawType) {
        // 排除基本类型和数组
        if (isBasicType(dataTypeName, rawType) ||
                dataTypeName.equals("array") ||
                rawType.contains("[]") ||
                rawType.contains("list") ||
                rawType.contains("set")) {
            return false;
        }

        // 检查是否是Java内置类型
        if (rawType.startsWith("java.lang.") ||
                rawType.startsWith("java.util.") ||
                rawType.startsWith("java.time.") ||
                rawType.startsWith("java.math.")) {
            return false;
        }

        // 其他都认为是复杂对象
        return true;
    }
} 