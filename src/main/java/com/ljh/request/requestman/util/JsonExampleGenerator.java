package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.ljh.request.requestman.model.ApiParam;

import java.util.List;
import java.util.Random;

/**
 * @author leijianhui
 * @Description JSON示例生成工具类，支持根据参数结构生成示例JSON。
 * @date 2025/06/18 15:06
 */
public class JsonExampleGenerator {

    /**
     * 生成json
     */
    public static String genJsonWithComment(List<ApiParam> params, int indent) {
        StringBuilder sb = new StringBuilder();
        String pad = "    ".repeat(indent);
        if (params == null || params.isEmpty()) {
            sb.append(pad).append("{}");
            return sb.toString();
        }
        sb.append(pad).append("{").append("\n");
        for (int i = 0; i < params.size(); i++) {
            ApiParam p = params.get(i);
            sb.append(pad).append("    ").append('"').append(p.getName()).append('"').append(": ");
            if (p.getDataType() != null && p.getDataType().name().equalsIgnoreCase("ARRAY")) {
                // 数组类型，递归children为数组元素结构
                sb.append("[");
                if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                    sb.append("\n");
                    sb.append(genJsonWithComment(p.getChildren(), indent + 2));
                    sb.append("\n").append(pad).append("    ");
                }
                sb.append("]");
            } else if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                sb.append(genJsonWithComment(p.getChildren(), indent + 1));
            } else {
                sb.append(getDefaultValue(p));
            }
            if (i < params.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(pad).append("}");
        return sb.toString();
    }

    /**
     * 递归生成嵌套JSON字符串
     *
     * @param params 参数树
     * @return JSON字符串
     */
    public static String generateJson(List<ApiParam> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < params.size(); i++) {
            ApiParam p = params.get(i);
            sb.append("    \"").append(p.getName()).append("\": ");
            if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                sb.append(generateJson(p.getChildren()));
            } else {
                sb.append(getDefaultValue(p));
            }
            if (i < params.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * 根据参数类型生成默认值
     *
     * @param p 参数对象
     * @return 默认值字符串
     */
    private static String getDefaultValue(ApiParam p) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "integer":
                return "0";
            case "boolean":
                return "false";
            case "number":
                return "0.0";
            case "array":
                return "[]";
            case "file":
                return "\"<file>\"";
            default:
                return "\"string\"";
        }
    }


    /**
     * 递归生成嵌套JSON字符串（带随机值）
     *
     * @param params 参数树
     * @return JSON字符串
     */
    public static String generateRandomJson(List<ApiParam> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        Random random = new Random();
        for (int i = 0; i < params.size(); i++) {
            ApiParam p = params.get(i);
            sb.append("    \"").append(p.getName()).append("\": ");
            if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                sb.append(generateRandomJson(p.getChildren()));
            } else {
                sb.append(getRandomValue(p, random));
            }
            if (i < params.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * 根据参数类型生成随机值
     *
     * @param p      参数对象
     * @param random 随机数生成器
     * @return 随机值字符串
     */
    private static String getRandomValue(ApiParam p, Random random) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "integer": {
                return String.valueOf(random.nextInt(1000));
            }
            case "boolean": {
                return String.valueOf(random.nextBoolean());
            }
            case "number": {
                return String.format("%.2f", random.nextDouble() * 1000);
            }
            case "array": {
                return "[]";
            }
            case "file": {
                return "\"<file>\"";
            }
            case "string":
            default: {
                String name = p.getName().toLowerCase();
                if (name.contains("date") || name.contains("time")) {
                    return "\"" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\"";
                }
                return "\"" + getRandomString(random, 6) + "\"";
            }
        }
    }

    /**
     * 生成指定长度的随机字符串
     *
     * @param random 随机数生成器
     * @param length 长度
     * @return 随机字符串
     */
    private static String getRandomString(Random random, int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 将参数列表序列化为JSON字符串
     *
     * @param params 参数列表
     * @return JSON字符串
     */
    public static String toJson(List<ApiParam> params) {
        return JSONUtil.toJsonStr(params);
    }
} 