package com.ljh.request.requestman.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

/**
 * 项目工具类，提供统一的项目获取和管理功能
 * 解决多项目环境下项目获取不准确的问题
 * 使用单例缓存机制，避免重复获取Project对象
 *
 * @author leijianhui
 * @Description 项目工具类，统一管理项目获取逻辑，支持单例缓存
 * @date 2025/01/27 16:30
 */
public class ProjectUtils {


    /**
     * 获取当前项目（安全方式，带缓存）
     * 优先从当前焦点获取项目，如果无法获取则使用第一个打开的项目作为后备方案
     * 使用缓存机制避免重复获取，提高性能
     *
     * @return 当前项目，如果无法获取则返回null
     */
    public static Project getCurrentProject() {

        // 缓存无效或过期，重新获取
        Project newProject = getCurrentProjectInternal();

        return newProject;
    }

    /**
     * 内部获取当前项目的方法（不涉及缓存）
     *
     * @return 当前项目，如果无法获取则返回null
     */
    private static Project getCurrentProjectInternal() {
        try {
            WindowManager windowManager = WindowManager.getInstance();
            if (windowManager != null) {
                // 尝试获取当前活动窗口对应的 Frame
                IdeFrame activeFrame = windowManager.getIdeFrame(null);
                if (activeFrame != null) {
                    Project activeProject = activeFrame.getProject();
                    if (activeProject != null && isProjectValid(activeProject)) {
                        return activeProject;
                    }
                }

                // 如果上面没有找到，再 fallback 到遍历所有 frame
                IdeFrame[] frames = windowManager.getAllProjectFrames();
                for (IdeFrame frame : frames) {
                    Project project = frame.getProject();
                    if (project != null && isProjectValid(project)) {
                        return project;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 验证项目是否有效
     *
     * @param project 项目对象
     * @return 如果项目有效返回true，否则返回false
     */
    private static boolean isProjectValid(Project project) {
        return project != null && !project.isDisposed();
    }


    /**
     * 强制刷新项目缓存
     * 立即重新获取当前项目并更新缓存
     *
     * @return 当前项目，如果无法获取则返回null
     */
    public static Project refreshCurrentProject() {
        return getCurrentProject();
    }

    /**
     * 获取当前项目（带异常处理）
     * 如果获取失败会抛出异常，适用于必须获取到项目的场景
     *
     * @return 当前项目
     * @throws IllegalStateException 如果无法获取到任何项目
     */
    public static Project getCurrentProjectOrThrow() {
        Project project = getCurrentProject();
        if (project == null) {
            throw new IllegalStateException("无法获取当前项目，请确保至少有一个项目已打开");
        }
        return project;
    }

    /**
     * 检查是否有项目打开
     *
     * @return 如果有项目打开返回true，否则返回false
     */
    public static boolean hasOpenProjects() {
        try {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : openProjects) {
                if (isProjectValid(project)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }

    /**
     * 获取打开的项目数量
     *
     * @return 打开的项目数量
     */
    public static int getOpenProjectCount() {
        try {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            int count = 0;
            for (Project project : openProjects) {
                if (isProjectValid(project)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取指定索引的项目
     * 如果索引超出范围，返回null
     *
     * @param index 项目索引
     * @return 指定索引的项目，如果索引无效返回null
     */
    public static Project getProjectByIndex(int index) {
        try {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            if (index >= 0 && index < openProjects.length) {
                Project project = openProjects[index];
                if (isProjectValid(project)) {
                    return project;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

} 