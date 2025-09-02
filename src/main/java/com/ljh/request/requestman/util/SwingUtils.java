package com.ljh.request.requestman.util;

import com.ljh.request.requestman.ui.EditableBodyPanel;
import com.ljh.request.requestman.ui.HeadersPanel;

import javax.swing.*;
import java.lang.reflect.Field;

/**
 * @author ljh
 * @Description 工具
 * @date 2025/8/8 14:58
 */
public class SwingUtils {

    /**
     * 工具方法：安全获取Panel中的JTable实例
     *
     * @param panel HeadersPanel实例
     * @return JTable对象，若获取失败返回null
     */
    public static JTable getTable(Object panel) {
        return getTable(panel, "table");
    }

    /**
     * 工具方法：安全获取Panel中的JTable实例
     *
     * @param panel HeadersPanel实例
     * @return JTable对象，若获取失败返回null
     */
    public static JTable getTable(Object panel, String name) {
        try {
            Object tableObj = getObject(panel, name);
            if (tableObj == null) {
                return null;
            }
            if (tableObj instanceof JTable) {
                return (JTable) tableObj;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 工具方法：安全获取Panel中的JTable实例
     *
     * @param panel HeadersPanel实例
     * @return JTable对象，若获取失败返回null
     */
    public static Object getObject(Object panel, String name) {
        if (panel == null) {
            return null;
        }
        try {
            Field tableField = panel.getClass().getDeclaredField(name);
            tableField.setAccessible(true);
            return tableField.get(panel);
        } catch (Exception ignored) {
        }
        return null;
    }
}
