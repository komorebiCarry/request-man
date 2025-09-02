package com.ljh.request.requestman.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.ljh.request.requestman.util.RequestManBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author leijianhui
 * @Description 显示/隐藏 RequestMan 工具窗口的 Action
 * @date 2025/01/19 10:00
 */
public class ShowRequestManPanelAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("RequestMan");
        
        if (toolWindow != null) {
            if (toolWindow.isVisible()) {
                // 如果面板已显示，则隐藏
                toolWindow.hide(null);
            } else {
                // 如果面板未显示，则显示并激活
                toolWindow.show(null);
                toolWindow.activate(null);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只有在有项目打开时才启用此 Action
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
        
        // 设置显示文本和描述
        e.getPresentation().setText(RequestManBundle.message("action.show.panel"));
        e.getPresentation().setDescription(RequestManBundle.message("action.show.panel.description"));
    }

}
