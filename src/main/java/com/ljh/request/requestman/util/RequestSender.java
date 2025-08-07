package com.ljh.request.requestman.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author leijianhui
 * @Description 请求发送工具类，支持多种HTTP请求的发送与响应处理。负责统一组装请求参数、变量替换、发起HTTP请求（Hutool）、处理响应、调用后置操作执行器。
 * @date 2025/06/19 09:36
 */
public class RequestSender {
    /**
     * 发送HTTP请求，返回响应字符串。
     */
    public static String sendRequest(Project project, String url, String method, List<ApiParam> params, String bodyType,
                                     List<ApiParam> bodyParams, String bodyContent, byte[] binaryData, Map<String, String> headers,
                                     Map<String, String> cookies, String auth, List<PostOpItem> postOps, String urlPrefix) {
        // 1. 变量替换（URL、Params、Body、Headers、Cookies、Auth）
        url = VariableReplacer.replace(project, url);
        if (StrUtil.isNotBlank(urlPrefix)) {
            url = urlPrefix + url;
        }
        Map<String, String> paramMap = paramListToMap(params);
        paramMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        // 路径变量替换
        url = replacePathVariables(url, paramMap);
        // 移除已用作路径变量的参数
        Set<String> usedKeys = new HashSet<>();
        Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(url);
        while (m.find()) {
            usedKeys.add(m.group(1));
        }
        for (String key : usedKeys) {
            paramMap.remove(key);
        }
        Map<String, String> headerMap = headers != null ? new HashMap<>(headers) : new HashMap<>();
        headerMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        Map<String, String> cookieMap = cookies != null ? new HashMap<>(cookies) : new HashMap<>();
        cookieMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        String realAuth = VariableReplacer.replace(project, auth);
        // 2. 构建请求
        HttpRequest request = HttpRequest.of(url).method(Method.valueOf(method));
        // 2.1 Headers
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
        // 2.2 Cookies
        if (!cookieMap.isEmpty()) {
            StringBuilder cookieStr = new StringBuilder();
            for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                cookieStr.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
            }
            request.header("Cookie", cookieStr.toString());
        }
        // 2.3 Auth（如有，加入header）
        if (StrUtil.isNotBlank(realAuth)) {
            request.header("Authorization", realAuth);
        }
        // 2.4 Params/Body
        if ("form-data".equals(bodyType) || "x-www-form-urlencoded".equals(bodyType)) {
            Map<String, String> bodyMap = paramListToMap(bodyParams);
            bodyMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));

            if ("form-data".equals(bodyType)) {
                // 处理form-data，支持文件上传
                for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // 检查是否是文件路径
                    if (value != null && !value.startsWith("<") && new File(value).exists()) {
                        // 是文件，使用form方法上传
                        request.form(key, new File(value));
                    } else {
                        // 是普通参数，使用form方法
                        request.form(key, value);
                    }
                }
            } else {
                // x-www-form-urlencoded使用form方法
                request.form(toObjectMap(bodyMap));
            }
        } else if ("json".equals(bodyType)) {
            request.body(StrUtil.isNotBlank(bodyContent) ? VariableReplacer.replace(project, bodyContent) : "");
            request.header("Content-Type", "application/json");
        } else if ("xml".equals(bodyType)) {
            request.body(StrUtil.isNotBlank(bodyContent) ? VariableReplacer.replace(project, bodyContent) : "");
            request.header("Content-Type", "application/xml");
        } else if ("binary".equals(bodyType)) {
            // 二进制数据处理，不进行变量替换以避免破坏文件内容
            if (binaryData != null && binaryData.length > 0) {
                request.body(binaryData);
                request.header("Content-Type", "application/octet-stream");
            }
        }
        // 3. 发送请求
        HttpResponse execute = request.execute();
        String respStr = execute.body();
        // 4. 执行后置操作
        PostOpExecutor.execute(project, respStr, postOps);
        return respStr;
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     */
    @Deprecated
    public static String sendRequest(String url, String method, List<ApiParam> params, String bodyType,
                                     List<ApiParam> bodyParams, String bodyContent, byte[] binaryData, Map<String, String> headers,
                                     Map<String, String> cookies, String auth, List<PostOpItem> postOps, String urlPrefix) {
        // 为了向后兼容，返回空字符串
        return "";
    }

    /**
     * 发送HTTP请求，返回原始响应对象。
     */
    public static HttpResponse sendRequestRaw(Project project, String url, String method, List<ApiParam> params, String bodyType,
                                              List<ApiParam> bodyParams, String bodyContent, byte[] binaryData, Map<String, String> headers,
                                              Map<String, String> cookies, String auth, String urlPrefix, List<PostOpItem> postOps) {
        url = VariableReplacer.replace(project, url);
        if (StrUtil.isNotBlank(urlPrefix)) {
            url = urlPrefix + url;
        }
        Map<String, String> paramMap = paramListToMap(params);
        paramMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        url = replacePathVariables(url, paramMap);
        Set<String> usedKeys = new HashSet<>();
        Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(url);
        while (m.find()) {
            usedKeys.add(m.group(1));
        }
        for (String key : usedKeys) {
            paramMap.remove(key);
        }
        Map<String, String> headerMap = headers != null ? new HashMap<>(headers) : new HashMap<>();
        headerMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        Map<String, String> cookieMap = cookies != null ? new HashMap<>(cookies) : new HashMap<>();
        cookieMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));
        String realAuth = VariableReplacer.replace(project, auth);
        HttpRequest request = HttpRequest.of(url).method(Method.valueOf(method));
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
        if (!cookieMap.isEmpty()) {
            StringBuilder cookieStr = new StringBuilder();
            for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                cookieStr.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
            }
            request.header("Cookie", cookieStr.toString());
        }
        if (StrUtil.isNotBlank(realAuth)) {
            request.header("Authorization", realAuth);
        }
        if ("form-data".equals(bodyType) || "x-www-form-urlencoded".equals(bodyType)) {
            Map<String, String> bodyMap = paramListToMap(bodyParams);
            bodyMap.replaceAll((k, v) -> VariableReplacer.replace(project, v));

            if ("form-data".equals(bodyType)) {
                // 处理form-data，支持文件上传
                for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // 检查是否是文件路径
                    if (value != null && !value.startsWith("<") && new File(value).exists()) {
                        // 是文件，使用form方法上传
                        request.form(key, new File(value));
                    } else {
                        // 是普通参数，使用form方法
                        request.form(key, value);
                    }
                }
            } else {
                // x-www-form-urlencoded使用form方法
                request.form(toObjectMap(bodyMap));
            }
        } else if ("json".equals(bodyType)) {
            request.body(StrUtil.isNotBlank(bodyContent) ? VariableReplacer.replace(project, bodyContent) : "");
            request.header("Content-Type", "application/json");
        } else if ("xml".equals(bodyType)) {
            request.body(StrUtil.isNotBlank(bodyContent) ? VariableReplacer.replace(project, bodyContent) : "");
            request.header("Content-Type", "application/xml");
        } else if ("binary".equals(bodyType)) {
            // 二进制数据处理，不进行变量替换以避免破坏文件内容
            if (binaryData != null && binaryData.length > 0) {
                request.body(binaryData);
                request.header("Content-Type", "application/octet-stream");
            }
        }
        // 发送请求并返回原始响应
        HttpResponse execute = request.execute();
        // 4. 执行后置操作
        PostOpExecutor.execute(project, execute.body(), postOps);
        return execute;
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     */
    @Deprecated
    public static HttpResponse sendRequestRaw(String url, String method, List<ApiParam> params, String bodyType,
                                              List<ApiParam> bodyParams, String bodyContent, byte[] binaryData, Map<String, String> headers,
                                              Map<String, String> cookies, String auth, String urlPrefix, List<PostOpItem> postOps) {
        // 为了向后兼容，返回null
        return null;
    }

    /**
     * 参数列表转Map
     *
     * @param paramList 参数列表
     * @return Map
     */
    private static Map<String, String> paramListToMap(List<ApiParam> paramList) {
        Map<String, String> map = new HashMap<>();
        if (paramList != null) {
            for (ApiParam p : paramList) {
                if (StrUtil.isNotBlank(p.getName())) {
                    map.put(p.getName(), p.getValue() != null ? p.getValue() : "");
                }
            }
        }
        return map;
    }

    /**
     * Map<String, String> 转 Map<String, Object>
     *
     * @param map 输入map
     * @return Map<String, Object>
     */
    private static Map<String, Object> toObjectMap(Map<String, String> map) {
        Map<String, Object> objMap = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                objMap.put(entry.getKey(), entry.getValue());
            }
        }
        return objMap;
    }

    /**
     * 路径变量替换，将url中的{xxx}用paramMap中的值替换
     */
    private static String replacePathVariables(String url, Map<String, String> paramMap) {
        if (url == null || paramMap == null) return url;
        String result = url;
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + key + "}", value);
        }
        return result;
    }
} 