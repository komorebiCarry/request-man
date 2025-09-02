package com.ljh.request.requestman.ui;

import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.Environment;
import com.ljh.request.requestman.util.ProjectSettingsManager;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * 环境选择组件，使用自定义的EnvironmentComboBox。
 *
 * @author leijianhui
 * @Description 环境选择组件，支持环境切换和管理。
 * @date 2025/01/27 16:45
 */
public class EnvironmentSelector extends JPanel {

    private final Project project;
    private final EnvironmentComboBox environmentComboBox;

    private List<Environment> allEnvironments;

    public EnvironmentSelector(Project project) {
        this.project = project;
        this.allEnvironments = ProjectSettingsManager.getAllEnvironments(project);

        // 创建管理环境的ActionListener
        ActionListener manageEnvironmentAction = e -> openEnvironmentManager();

        // 创建自定义的EnvironmentComboBox
        this.environmentComboBox = new EnvironmentComboBox(project, allEnvironments, manageEnvironmentAction);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // 设置ComboBox的宽度，只显示Icon，但下拉框需要足够宽度
        environmentComboBox.setPreferredSize(new Dimension(70, environmentComboBox.getPreferredSize().height));
        environmentComboBox.setMaximumSize(new Dimension(70, environmentComboBox.getMaximumSize().height));

        // 设置下拉框的最小宽度，确保能完整显示环境名称
        environmentComboBox.setPrototypeDisplayValue(null);

        // 添加鼠标悬停提示文本
        environmentComboBox.setToolTipText(RequestManBundle.message("env.selector.tooltip"));

        // 添加到主面板
        add(environmentComboBox);
    }

    /**
     * 刷新环境列表
     */
    public void refreshEnvironments() {
        allEnvironments = ProjectSettingsManager.getAllEnvironments(project);
        environmentComboBox.refreshEnvironments(allEnvironments);
    }

    /**
     * 获取当前选中的环境
     */
    public Environment getSelectedEnvironment() {
        return environmentComboBox.getSelectedEnvironment();
    }

    /**
     * 设置选中的环境
     */
    public void setSelectedEnvironment(Environment environment) {
        environmentComboBox.setSelectedEnvironment(environment);
    }

    /**
     * 打开环境管理器
     */
    private void openEnvironmentManager() {

        // 打开设置页面并切换到环境管理tab
        RequestManSettingsConfigurable.showSettingsDialog(project, 1);
    }

    /**
     * 添加环境选择监听器
     */
    public void addEnvironmentChangeListener(ActionListener listener) {
        environmentComboBox.addActionListener(listener);
    }

    /**
     * 移除环境选择监听器
     */
    public void removeEnvironmentChangeListener(ActionListener listener) {
        environmentComboBox.removeActionListener(listener);
    }
} 