package com.ljh.request.requestman.util;

import com.intellij.openapi.diagnostic.Logger;

/**
 * 统一日志工具类，使用IntelliJ平台的日志机制
 *
 * @author leijianhui
 * @Description 统一日志工具类，提供debug、info、warn、error等日志级别。
 * @date 2025/01/27 11:30
 */
public class LogUtil {

    private static final Logger LOG = Logger.getInstance("RequestMan");

    /**
     * 记录调试信息
     *
     * @param message 日志消息
     */
    public static void debug(String message) {
        LOG.debug(message);
    }

    /**
     * 记录调试信息（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void debug(String message, Throwable throwable) {
        LOG.debug(message, throwable);
    }

    /**
     * 记录一般信息
     *
     * @param message 日志消息
     */
    public static void info(String message) {
        LOG.info(message);
    }

    /**
     * 记录一般信息（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void info(String message, Throwable throwable) {
        LOG.info(message, throwable);
    }

    /**
     * 记录警告信息
     *
     * @param message 日志消息
     */
    public static void warn(String message) {
        LOG.warn(message);
    }

    /**
     * 记录警告信息（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void warn(String message, Throwable throwable) {
        LOG.warn(message, throwable);
    }

    /**
     * 记录错误信息
     *
     * @param message 日志消息
     */
    public static void error(String message) {
        LOG.error(message);
    }

    /**
     * 记录错误信息（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void error(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }
} 