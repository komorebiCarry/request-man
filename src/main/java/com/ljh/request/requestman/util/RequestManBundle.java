package com.ljh.request.requestman.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 文案资源访问工具，读取 messages/RequestManBundle*.properties。
 * 不修改全局Locale，仅根据 LanguageManager 的当前设置解析。
 *
 * @author leijianhui
 * @Description 国际化文案访问工具
 * @date 2025/06/20 10:00
 */
public class RequestManBundle {

    private static final String BUNDLE_FQN = "messages.RequestManBundle";

    private RequestManBundle() {
    }

    /**
     * 获取文案
     */
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_FQN) String key, Object... params) {
        Locale locale = LanguageManager.getCurrentLocale();
        try {
            // 禁止回退到系统默认Locale，避免在选择英文时被系统中文覆盖
            ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_FQN, locale, control);
            String pattern = bundle.getString(key);
            if (params == null || params.length == 0) {
                return pattern;
            }
            return MessageFormat.format(pattern, params);
        } catch (MissingResourceException e) {
            try {
                ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
                ResourceBundle fallback = ResourceBundle.getBundle(BUNDLE_FQN, Locale.ENGLISH, control);
                String pattern = fallback.getString(key);
                return (params == null || params.length == 0) ? pattern : MessageFormat.format(pattern, params);
            } catch (Exception ignore) {
                return key;
            }
        }
    }
}


