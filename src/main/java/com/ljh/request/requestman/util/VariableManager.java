package com.ljh.request.requestman.util;

import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.util.ProjectSettingsManager;

import java.util.Collections;
import java.util.Map;

/**
 * 全局变量管理工具类，支持变量的增删改查与持久化。
 * 现在基于项目级别管理变量，每个项目有独立的变量池。
 *
 * @author leijianhui
 * @Description 全局变量管理工具类，支持项目级别的变量管理。
 * @date 2025/06/19 09:36
 */
public class VariableManager {

    /**
     * 加载所有变量到内存（启动时调用）
     * 现在这个方法不再需要，因为变量是按项目动态加载的
     */
    @Deprecated
    public static void loadAll() {
        // 这个方法现在不再需要，变量是按项目动态加载的
        // 保留是为了向后兼容
    }

    /**
     * 获取所有变量（只读Map）
     *
     * @param project 项目对象
     * @return 变量池快照
     */
    public static Map<String, String> getAll(Project project) {
        return ProjectSettingsManager.getAllProjectVariables(project);
    }

    /**
     * 获取变量值
     *
     * @param project 项目对象
     * @param name    变量名
     * @return 变量值，若不存在返回null
     */
    public static String get(Project project, String name) {
        return ProjectSettingsManager.getProjectVariable(project, name);
    }

    /**
     * 新增或更新变量
     *
     * @param project 项目对象
     * @param name    变量名
     * @param value   变量值
     */
    public static void put(Project project, String name, String value) {
        ProjectSettingsManager.setProjectVariable(project, name, value);
    }

    /**
     * 删除变量
     *
     * @param project 项目对象
     * @param name    变量名
     */
    public static void remove(Project project, String name) {
        ProjectSettingsManager.removeProjectVariable(project, name);
    }

    /**
     * 清空所有变量
     *
     * @param project 项目对象
     */
    public static void clear(Project project) {
        ProjectSettingsManager.clearProjectVariables(project);
    }

    /**
     * 判断变量是否存在
     *
     * @param project 项目对象
     * @param name    变量名
     * @return 是否存在
     */
    public static boolean contains(Project project, String name) {
        return ProjectSettingsManager.containsProjectVariable(project, name);
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param name 变量名
     * @return 变量值，若不存在返回null
     * @deprecated 请使用 get(Project project, String name) 方法
     */
    @Deprecated
    public static String get(String name) {
        // 为了向后兼容，返回null
        return null;
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param name  变量名
     * @param value 变量值
     * @deprecated 请使用 put(Project project, String name, String value) 方法
     */
    @Deprecated
    public static void put(String name, String value) {
        // 为了向后兼容，不执行任何操作
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param name 变量名
     * @deprecated 请使用 remove(Project project, String name) 方法
     */
    @Deprecated
    public static void remove(String name) {
        // 为了向后兼容，不执行任何操作
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @deprecated 请使用 clear(Project project) 方法
     */
    @Deprecated
    public static void clear() {
        // 为了向后兼容，不执行任何操作
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @param name 变量名
     * @return 是否存在
     * @deprecated 请使用 contains(Project project, String name) 方法
     */
    @Deprecated
    public static boolean contains(String name) {
        // 为了向后兼容，返回false
        return false;
    }

    /**
     * 向后兼容的方法，使用全局变量（已废弃）
     *
     * @return 变量池快照
     * @deprecated 请使用 getAll(Project project) 方法
     */
    @Deprecated
    public static Map<String, String> getAll() {
        // 为了向后兼容，返回空Map
        return Collections.emptyMap();
    }
} 