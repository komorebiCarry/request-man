package com.ljh.request.requestman.util;

import com.intellij.ide.util.PropertiesComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 语言管理器，负责保存与切换插件内语言，并通知监听器刷新UI。
 *
 * @author leijianhui
 * @Description 管理语言偏好，支持运行时切换并通知监听器
 * @date 2025/06/20 10:00
 */
public class LanguageManager {

    /**
     * 配置存储的Key
     */
    private static final String KEY_LANGUAGE = "requestman.language";

    /**
     * 默认语言
     */
    private static final String DEFAULT_LANGUAGE_CODE = "en";

    /**
     * 监听器列表
     */
    private static final List<LanguageChangeListener> LISTENERS = new ArrayList<>();

    /**
     * 当前语言代码，例如：en、zh_CN
     */
    private static String currentLanguageCode;

    static {
        currentLanguageCode = PropertiesComponent.getInstance().getValue(KEY_LANGUAGE, DEFAULT_LANGUAGE_CODE);
        if (currentLanguageCode == null || currentLanguageCode.trim().isEmpty()) {
            currentLanguageCode = DEFAULT_LANGUAGE_CODE;
        }
    }

    /**
     * 获取当前语言代码。
     */
    public static String getLanguageCode() {
        return currentLanguageCode;
    }

    /**
     * 获取当前Locale。
     */
    public static Locale getCurrentLocale() {
        if (Objects.equals("zh_CN", currentLanguageCode)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }

    /**
     * 设置语言并通知监听器。
     *
     * @param languageCode 语言代码："en" 或 "zh_CN"
     */
    public static void setLanguage(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = DEFAULT_LANGUAGE_CODE;
        }
        if (Objects.equals(currentLanguageCode, languageCode)) {
            return;
        }
        currentLanguageCode = languageCode;
        PropertiesComponent.getInstance().setValue(KEY_LANGUAGE, currentLanguageCode);
        notifyLanguageChanged();
    }

    /**
     * 注册语言变更监听器。
     */
    public static void registerListener(LanguageChangeListener listener) {
        if (listener == null) {
            return;
        }
        LISTENERS.add(listener);
    }

    /**
     * 取消注册语言变更监听器。
     */
    public static void unregisterListener(LanguageChangeListener listener) {
        if (listener == null) {
            return;
        }
        LISTENERS.remove(listener);
    }

    private static void notifyLanguageChanged() {
        Locale locale = getCurrentLocale();
        for (LanguageChangeListener listener : new ArrayList<>(LISTENERS)) {
            try {
                listener.onLanguageChanged(locale);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 语言变更监听接口。
     */
    public interface LanguageChangeListener {
        /**
         * 当语言变更时回调。
         */
        void onLanguageChanged(Locale newLocale);
    }
}


