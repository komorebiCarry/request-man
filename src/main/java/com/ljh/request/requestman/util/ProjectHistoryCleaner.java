package com.ljh.request.requestman.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

/**
 * 项目历史记录清理工具类，负责在项目关闭时清理该项目相关的历史记录。
 *
 * @author leijianhui
 * @Description 项目历史记录清理工具类。
 * @date 2025/01/27 10:30
 */
public class ProjectHistoryCleaner {

    /**
     * 清理项目相关的所有历史记录
     *
     * @param project 项目对象
     */
    public static void clearProjectHistory(Project project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();

        // MySearchTextField使用的是全局的PropertiesComponent，所以我们也用全局的
        PropertiesComponent globalProperties = PropertiesComponent.getInstance();

        // 清理ApiSearchPopup的历史记录
        String apiSearchHistoryKey = "requestman.apiSearchPopup." + projectName;

        // 清理全局级存储 - 使用setValue(null)而不是unsetValue
        globalProperties.unsetValue(apiSearchHistoryKey);

        ApplicationManager.getApplication().saveSettings();
        // 可以在这里添加其他类型的历史记录清理
        // 例如：参数搜索历史、URL搜索历史等
    }

    /**
     * 获取项目历史记录统计信息
     *
     * @return 统计信息字符串
     */
    public static String getHistoryStats() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        StringBuilder stats = new StringBuilder();
        stats.append("=== RequestMan 历史记录统计 ===\n");

        // 这里可以添加统计逻辑，但目前PropertiesComponent没有直接的方法获取所有键
        // 我们可以通过已知的键名模式来统计

        return stats.toString();
    }
}