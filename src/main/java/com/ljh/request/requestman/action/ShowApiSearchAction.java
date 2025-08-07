package com.ljh.request.requestman.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.search.ApiSearchPopup;
import org.jetbrains.annotations.NotNull;

/**
 * @author leijianhui
 * @Description IDEA全局Action：弹出接口搜索窗口（ApiSearchPopup），支持快捷键（Ctrl+Alt+/）和菜单触发。
 * @date 2025/06/19 21:00
 */
public class ShowApiSearchAction extends AnAction {
    /**
     * 触发Action时弹出接口搜索窗口。
     *
     * @param e 事件对象，包含当前Project等上下文
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            ApiSearchPopup popup = new ApiSearchPopup(project);
            popup.show();
        }
    }
} 