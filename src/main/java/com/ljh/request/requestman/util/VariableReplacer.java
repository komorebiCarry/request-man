package com.ljh.request.requestman.util;

import com.intellij.openapi.project.Project;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author leijianhui
 * @Description 变量占位符替换工具类，支持${}格式变量的动态替换。
 * @date 2025/06/19 09:36
 */
public class VariableReplacer {
    /**
     * 变量占位符正则，匹配{{变量名}}
     */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    /**
     * 替换字符串中的所有{{变量名}}为变量池中的值，支持递归替换。
     *
     * @param project 项目对象
     * @param input   输入字符串
     * @return 替换后的字符串
     */
    public static String replace(Project project, String input) {
        if (input == null || input.isEmpty() || project == null) {
            return input;
        }
        String result = input;
        // 防止死循环
        int maxDepth = 5;
        for (int i = 0; i < maxDepth; i++) {
            Matcher matcher = VAR_PATTERN.matcher(result);
            boolean found = false;
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                found = true;
                String varName = matcher.group(1);
                String value = VariableManager.get(project, varName);
                matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value) : "");
            }
            matcher.appendTail(sb);
            result = sb.toString();
            if (!found) {
                break;
            }
        }
        return result;
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param input 输入字符串
     * @return 替换后的字符串
     * @deprecated 请使用 replace(Project project, String input) 方法
     */
    @Deprecated
    public static String replace(String input) {
        // 为了向后兼容，直接返回原字符串
        return input;
    }
} 