package com.ljh.request.requestman.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.PopupCornerType;
import com.intellij.ui.WindowRoundedCornersManager;
import com.ljh.request.requestman.util.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 自定义搜索对话框，模拟JBPopup样式
 *
 * @author leijianhui
 * @Description 自定义搜索对话框，提供类似JBPopup的样式和交互体验
 * @date 2025/01/29 18:30
 */
public class SearchDialog extends DialogWrapper {

    private final JComponent contentPanel;
    private JComponent focusComponent;

    public SearchDialog(Project project, JComponent contentPanel) {
        super(project);
        this.contentPanel = contentPanel;
        setTitle("API Search");
        setModal(false);
        setResizable(false);
        // 模拟JBPopup样式：无边框、无标题栏
        setUndecorated(true);
        init();
    }

    /**
     * 设置需要获得焦点的组件
     */
    public void setFocusComponent(JComponent component) {
        this.focusComponent = component;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        // 不显示按钮
        return new Action[0];
    }

    @Override
    protected JComponent createContentPane() {
        // 创建无边框的内容面板，模拟JBPopup样式
        JPanel contentPane = new JPanel(new BorderLayout());
        // 减少边距从8改为4
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        // 半透明背景
        contentPane.setBackground(new Color(60, 60, 60, 240));
        contentPane.add(contentPanel, BorderLayout.CENTER);
        return contentPane;
    }

    @Override
    protected void createDefaultActions() {
        // 不创建默认的确定/取消按钮
    }

    @Override
    protected @NotNull Action getHelpAction() {
        // 返回一个空的 Action 而不是 null，满足 @NotNull 约束
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 空实现，不执行任何操作
            }
        };
    }

    @Override
    protected void init() {
        super.init();
        // 设置窗口属性，模拟JBPopup
        getWindow().setAlwaysOnTop(true);
        // 透明背景
        getWindow().setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    public void show() {
        super.show();

        // 使用JBPopup的圆角实现方式
        SwingUtilities.invokeLater(() -> {
            Window window = getWindow();
            if (window != null) {
                // 检查是否支持圆角
                if (WindowRoundedCornersManager.isAvailable()) {
                    // 使用JBPopup相同的圆角参数
                    Object roundedCornerParams = PopupCornerType.RoundedWindow;
                    WindowRoundedCornersManager.setRoundedCorners(window, roundedCornerParams);
                }

                // 添加窗口失焦监听器，当切换到其他应用时关闭弹窗
                window.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
                    @Override
                    public void windowGainedFocus(java.awt.event.WindowEvent e) {
                        // 窗口获得焦点时不做处理
                    }

                    @Override
                    public void windowLostFocus(java.awt.event.WindowEvent e) {
                        // 延迟检查是否真的切换到了其他应用
                        SwingUtilities.invokeLater(() -> {
                            // 检查当前活动窗口是否还是我们的窗口或其子窗口
                            Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                            if (activeWindow == null || !isRelatedWindow(activeWindow, window)) {
                                // 如果活动窗口不是我们的窗口或其子窗口，说明真的切换到了其他应用
                                if (window.isVisible()) {
                                    dispose();
                                }
                            }
                        });
                    }
                });
            }
        });

        // 对话框显示后设置焦点
        if (focusComponent != null) {
            SwingUtilities.invokeLater(() -> {
                // 延迟设置焦点，确保对话框完全显示
                Timer focusTimer = new Timer(100, e -> {
                    if (focusComponent.isVisible() && focusComponent.isEnabled()) {
                        focusComponent.requestFocusInWindow();
                        LogUtil.debug("SearchDialog: 设置焦点到组件: " + focusComponent.getClass().getSimpleName());
                    }
                });
                focusTimer.setRepeats(false);
                focusTimer.start();
            });
        }
    }

    /**
     * 检查窗口是否相关（是同一个窗口或其子窗口）
     */
    private boolean isRelatedWindow(Window activeWindow, Window ourWindow) {
        if (activeWindow == ourWindow) {
            return true;
        }

        // 检查是否是子窗口
        Window parent = activeWindow.getOwner();
        while (parent != null) {
            if (parent == ourWindow) {
                return true;
            }
            parent = parent.getOwner();
        }

        // 检查是否是JBPopup（结果弹窗）
        if (activeWindow instanceof JWindow) {
            JWindow jWindow = (JWindow) activeWindow;
            // 检查是否是JBPopup的窗口
            if (jWindow.getRootPane() != null && jWindow.getRootPane().getClientProperty("JBPopup") != null) {
                return true;
            }
        }

        return false;
    }
} 