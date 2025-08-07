package com.ljh.request.requestman.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description JSONPath提取器工具类，支持从JSON中提取字段路径和值。
 * @date 2025/01/29 16:30
 */
public class JsonPathExtractor {

    /**
     * 从JSON字符串中提取指定路径的值
     *
     * @param jsonStr  JSON字符串
     * @param jsonPath JSONPath表达式
     * @return 提取的值，失败返回null
     */
    public static String extractValue(String jsonStr, String jsonPath) {
        if (StrUtil.isBlank(jsonStr) || StrUtil.isBlank(jsonPath)) {
            return null;
        }

        try {
            Object value = JSONUtil.parseObj(jsonStr).getByPath(jsonPath);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从JSON字符串中提取所有可能的字段路径
     *
     * @param jsonStr JSON字符串
     * @return 字段路径列表
     */
    public static List<JsonPathField> extractAllPaths(String jsonStr) {
        List<JsonPathField> paths = new ArrayList<>();
        if (StrUtil.isBlank(jsonStr)) {
            return paths;
        }

        try {
            Object json = JSONUtil.parse(jsonStr);
            extractPathsRecursive(json, "", paths);
        } catch (Exception e) {
            // JSON解析失败，返回空列表
        }

        return paths;
    }

    /**
     * 递归提取JSON中的所有字段路径
     *
     * @param obj         JSON对象
     * @param currentPath 当前路径
     * @param paths       路径列表
     */
    private static void extractPathsRecursive(Object obj, String currentPath, List<JsonPathField> paths) {
        if (obj == null) {
            return;
        }

        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            for (String key : jsonObj.keySet()) {
                String newPath = currentPath.isEmpty() ? "$." + key : currentPath + "." + key;
                Object value = jsonObj.get(key);

                // 添加当前字段
                paths.add(new JsonPathField(newPath, value, getValueType(value)));

                // 递归处理嵌套对象
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    extractPathsRecursive(value, newPath, paths);
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            for (int i = 0; i < jsonArray.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                Object value = jsonArray.get(i);

                // 添加当前字段
                paths.add(new JsonPathField(newPath, value, getValueType(value)));

                // 递归处理嵌套对象
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    extractPathsRecursive(value, newPath, paths);
                }
            }
        }
    }

    /**
     * 获取值的类型描述
     *
     * @param value 值
     * @return 类型描述
     */
    private static String getValueType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof JSONArray) {
            return "array";
        }
        if (value instanceof JSONObject) {
            return "object";
        }
        return "unknown";
    }

    /**
     * JSONPath字段信息
     */
    public static class JsonPathField {
        private final String path;
        private final Object value;
        private final String type;

        public JsonPathField(String path, Object value, String type) {
            this.path = path;
            this.value = value;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        public String getDisplayValue() {
            if (value == null) {
                return "null";
            }
            if (value instanceof String) {
                String str = (String) value;
                return str.length() > 50 ? str.substring(0, 50) + "..." : str;
            }
            if (value instanceof JSONObject || value instanceof JSONArray) {
                return "{" + type + "}";
            }
            return value.toString();
        }

        @Override
        public String toString() {
            return path + " (" + type + ")";
        }
    }
} 