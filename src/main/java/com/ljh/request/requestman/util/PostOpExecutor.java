package com.ljh.request.requestman.util;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;
import com.ljh.request.requestman.ui.VariablePanel;
import com.ljh.request.requestman.util.RequestManBundle;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author leijianhui
 * @Description 后置操作执行工具类，支持变量提取与赋值。
 * @date 2025/06/19 09:36
 */
public class PostOpExecutor {
    /**
     * 执行所有后置操作，将提取结果写入项目变量池。
     *
     * @param project      项目对象
     * @param responseBody 响应内容（一般为JSON字符串）
     * @param postOps      后置操作列表
     */
    public static void execute(Project project, String responseBody, List<PostOpItem> postOps) {
        if (StrUtil.isBlank(responseBody) || postOps == null || postOps.isEmpty() || project == null) {
            return;
        }
        boolean changed = false;
        for (PostOpItem item : postOps) {
            if (item == null || StrUtil.isBlank(item.name) || StrUtil.isBlank(item.type) || StrUtil.isBlank(item.value)) {
                continue;
            }
            String result = null;
            if ("JSONPath".equals(item.type)) {
                result = extractByJsonPathWithAffix(responseBody, item.value);
            } else if ("TEXT".equals(item.type)) {
                result = extractByRegex(responseBody, item.value);
            }
            if (result != null) {
                VariableManager.put(project, item.name, result);
                changed = true;
            }
        }
        // 自动刷新项目变量面板
        if (changed) {
            VariablePanel.reloadIfExists();
        }
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param responseBody 响应内容（一般为JSON字符串）
     * @param postOps      后置操作列表
     * @deprecated 请使用 execute(Project project, String responseBody, List<PostOpItem> postOps) 方法
     */
    @Deprecated
    public static void execute(String responseBody, List<PostOpItem> postOps) {
        // 为了向后兼容，不执行任何操作
    }

    /**
     * 用JSONPath表达式提取内容
     *
     * @param json     响应内容
     * @param jsonPath JSONPath表达式
     * @return 提取结果，失败返回null
     */
    private static String extractByJsonPath(String json, String jsonPath) {
        try {
            Object value = cn.hutool.json.JSONUtil.parseObj(json).getByPath(jsonPath);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 用正则表达式提取内容（取第一个分组）
     *
     * @param text  响应内容
     * @param regex 正则表达式
     * @return 提取结果，失败返回null
     */
    private static String extractByRegex(String text, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 支持前后缀拼接的JSONPath提取
     * 表达式格式：前缀+JSONPath+后缀，空格等字符原样保留
     */
    private static String extractByJsonPathWithAffix(String json, String expr) {
        if (expr == null) {
            return null;
        }
        int jsonPathStart = expr.indexOf('$');
        if (jsonPathStart < 0) {
            // 没有JSONPath，直接返回原表达式
            return expr;
        }
        // JSONPath结尾：只包含字母、数字、点、下标、下划线、引号
        int jsonPathEnd = jsonPathStart;
        while (jsonPathEnd < expr.length()) {
            char c = expr.charAt(jsonPathEnd);
            if (Character.isLetterOrDigit(c) || c == '$' || c == '.' || c == '_' || c == '[' || c == ']' || c == '\'' || c == '"') {
                jsonPathEnd++;
            } else {
                break;
            }
        }
        String prefix = expr.substring(0, jsonPathStart);
        String jsonPath = expr.substring(jsonPathStart, jsonPathEnd);
        String suffix = expr.substring(jsonPathEnd);
        String value = extractByJsonPath(json, jsonPath);
        if (value != null) {
            return prefix + value + suffix;
        }
        return null;
    }
} 