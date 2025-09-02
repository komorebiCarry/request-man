package com.ljh.request.requestman.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.ljh.request.requestman.model.Environment;
import com.ljh.request.requestman.util.ProjectSettingsManager;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

/**
 * 环境管理面板，用于在设置页面中管理多环境配置。
 *
 * @author leijianhui
 * @Description 环境管理面板，支持添加、编辑、删除环境配置。
 * @date 2025/01/27 17:00
 */
public class EnvironmentManagerPanel extends JPanel {

    private final Project project;
    private final JBTable environmentTable;
    private final DefaultTableModel tableModel;
    private final JButton addButton;
    private final JButton deleteButton;

    public EnvironmentManagerPanel(Project project) {
        this.project = project;
        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; // 只有环境名称和前置URL列可编辑，ID列不可编辑
            }
        };
        this.environmentTable = new JBTable(tableModel);
        this.addButton = new JButton(RequestManBundle.message("env.add"));
        this.deleteButton = new JButton(RequestManBundle.message("env.delete"));

        initComponents();
        loadEnvironments();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 初始化表格 - 添加隐藏的ID列
        tableModel.setColumnIdentifiers(new String[]{"ID", RequestManBundle.message("env.name"), RequestManBundle.message("env.preurl")});
        environmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        environmentTable.getTableHeader().setReorderingAllowed(false);

        // 隐藏ID列
        environmentTable.getColumnModel().getColumn(0).setMinWidth(0);
        environmentTable.getColumnModel().getColumn(0).setMaxWidth(0);
        environmentTable.getColumnModel().getColumn(0).setWidth(0);
        environmentTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        // 设置列宽
        environmentTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        environmentTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        // 添加表格编辑监听器，当用户直接编辑表格时保存修改
        environmentTable.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 0 && column > 0) { // 跳过ID列（column 0）
                    handleTableEdit(row, column);
                }
            }
        });

        // 添加双击编辑功能
        environmentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedEnvironment();
                }
            }
        });

        // 添加表格选择监听器
        environmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        // 添加按钮事件
        addButton.addActionListener(e -> addEnvironment());
        deleteButton.addActionListener(e -> deleteSelectedEnvironment());

        // 布局
        add(new JBScrollPane(environmentTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 初始按钮状态
        updateButtonStates();
    }

    /**
     * 加载环境列表
     */
    public void loadEnvironments() {
        tableModel.setRowCount(0);
        List<Environment> environments = ProjectSettingsManager.getAllEnvironments(project);

        for (Environment env : environments) {
            Vector<Object> row = new Vector<>();
            row.add(env.getId());      // ID列（隐藏）
            row.add(env.getName());    // 环境名称列
            row.add(env.getPreUrl());  // 前置URL列
            tableModel.addRow(row);
        }

        updateButtonStates();
    }

    /**
     * 添加环境
     */
    private void addEnvironment() {
        EnvironmentDialog dialog = new EnvironmentDialog(project, null);
        dialog.setResizable(false);
        dialog.setModal(true);

        if (dialog.showAndGet()) {
            Environment newEnv = dialog.getEnvironment();
            if (newEnv != null) {
                ProjectSettingsManager.addEnvironment(project, newEnv);
                loadEnvironments();

                // 刷新环境选择器
                refreshEnvironmentSelector();
            }
        }
    }

    /**
     * 编辑选中的环境
     */
    private void editSelectedEnvironment() {
        int selectedRow = environmentTable.getSelectedRow();
        if (selectedRow == -1) {
            Messages.showWarningDialog(this, RequestManBundle.message("env.select.first"), RequestManBundle.message("main.tip"));
            return;
        }

        String envId = (String) tableModel.getValueAt(selectedRow, 0);
        Environment env = ProjectSettingsManager.getEnvironmentById(project, envId);
        if (env == null) {
            Messages.showErrorDialog(this, RequestManBundle.message("env.notfound"), RequestManBundle.message("common.error"));
            return;
        }

        EnvironmentDialog dialog = new EnvironmentDialog(project, env);
        dialog.setResizable(false);
        dialog.setModal(true);

        if (dialog.showAndGet()) {
            Environment updatedEnv = dialog.getEnvironment();
            if (updatedEnv != null) {
                ProjectSettingsManager.updateEnvironment(project, updatedEnv);
                // 立即刷新表格显示
                loadEnvironments();

                // 刷新环境选择器
                refreshEnvironmentSelector();
            }
        }
    }

    /**
     * 删除选中的环境
     */
    private void deleteSelectedEnvironment() {
        int selectedRow = environmentTable.getSelectedRow();
        if (selectedRow == -1) {
            Messages.showWarningDialog(this, RequestManBundle.message("env.select.first"), RequestManBundle.message("main.tip"));
            return;
        }

        String envId = (String) tableModel.getValueAt(selectedRow, 0);
        String envName = (String) tableModel.getValueAt(selectedRow, 1);
        Environment env = ProjectSettingsManager.getEnvironmentById(project, envId);
        if (env == null) {
            Messages.showErrorDialog(this, RequestManBundle.message("env.notfound"), RequestManBundle.message("common.error"));
            return;
        }

        int result = Messages.showYesNoDialog(this,
                RequestManBundle.message("env.delete.confirm", envName),
                RequestManBundle.message("env.delete.title"),
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            ProjectSettingsManager.removeEnvironment(project, env.getId());
            loadEnvironments();

            // 刷新环境选择器
            refreshEnvironmentSelector();
        }
    }

    /**
     * 刷新环境选择器
     */
    private void refreshEnvironmentSelector() {
        // 简单的刷新逻辑，通过重新加载环境列表来更新选择器
        // 环境选择器会在下次打开时自动刷新
    }

    /**
     * 公共方法：刷新环境选择器
     */
    public void refreshEnvironmentSelectorPublic() {
        // 这个方法可以被外部调用来刷新环境选择器
        loadEnvironments();
    }

    /**
     * 获取环境配置的字符串表示，用于比较是否有变动
     */
    public static String getEnvironmentConfig(Project project) {
        if (project == null) {
            return "";
        }

        List<Environment> environments = ProjectSettingsManager.getAllEnvironments(project);
        StringBuilder sb = new StringBuilder();
        for (Environment env : environments) {
            sb.append(env.getId()).append(":").append(env.getName()).append(":").append(env.getPreUrl()).append(";");
        }
        return sb.toString();
    }


    /**
     * 处理表格编辑事件
     */
    private void handleTableEdit(int row, int column) {
        try {
            // ID列
            String envId = (String) tableModel.getValueAt(row, 0);
            // 环境名称列
            String envName = (String) tableModel.getValueAt(row, 1);
            // 前置URL列
            String preUrl = (String) tableModel.getValueAt(row, 2);

            Environment env = ProjectSettingsManager.getEnvironmentById(project, envId);
            if (env != null) {
                // 根据编辑的列更新相应的字段
                if (column == 1) {
                    if (!envName.trim().isEmpty()) {
                        // 检查名称是否重复
                        List<Environment> environments = ProjectSettingsManager.getAllEnvironments(project);
                        boolean nameExists = environments.stream()
                                .anyMatch(e -> e.getName().equals(envName) && !e.getId().equals(env.getId()));

                        if (nameExists) {
                            Messages.showErrorDialog(this, RequestManBundle.message("env.name.exists"), RequestManBundle.message("common.error"));
                            // 恢复原值
                            loadEnvironments();
                            return;
                        }
                        env.setName(envName.trim());
                    } else {
                        Messages.showErrorDialog(this, RequestManBundle.message("env.name.empty"), RequestManBundle.message("common.error"));
                        // 恢复原值
                        loadEnvironments();
                        return;
                    }
                } else if (column == 2) {
                    // 编辑的是前置URL
                    env.setPreUrl(preUrl != null ? preUrl.trim() : "");
                }

                // 保存修改
                ProjectSettingsManager.updateEnvironment(project, env);

                // 刷新环境选择器
                refreshEnvironmentSelector();
            }
        } catch (Exception e) {
            // 恢复原值
            loadEnvironments();
        }
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = environmentTable.getSelectedRow() != -1;
        deleteButton.setEnabled(hasSelection);
    }

    /**
     * 环境编辑对话框
     */
    private static class EnvironmentDialog extends DialogWrapper {
        private final Project project;
        private final Environment environment;
        private final JTextField nameField;
        private final JTextField preUrlField;

        public EnvironmentDialog(Project project, Environment environment) {
            super(project);
            this.project = project;
            this.environment = environment;

            this.nameField = new JTextField();
            this.preUrlField = new JTextField();

            setTitle(environment == null ? RequestManBundle.message("env.dialog.add.title") : RequestManBundle.message("env.dialog.edit.title"));
            setOKButtonText(RequestManBundle.message("common.ok"));
            setCancelButtonText(RequestManBundle.message("common.cancel"));

            // 确保在init()之前设置所有必要的属性
            setResizable(false);
            setModal(true);

            init();
            initFields();
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // 环境名称
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel(RequestManBundle.message("env.name") + ":"), gbc);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            panel.add(nameField, gbc);

            // 前置URL
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
            panel.add(new JLabel(RequestManBundle.message("env.preurl") + ":"), gbc);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            panel.add(preUrlField, gbc);

            // 设置文本框的首选大小
            nameField.setPreferredSize(new Dimension(300, 25));
            preUrlField.setPreferredSize(new Dimension(300, 25));

            panel.setPreferredSize(new Dimension(450, 150));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            return panel;
        }

        private void initFields() {
            if (environment != null) {
                nameField.setText(environment.getName());
                preUrlField.setText(environment.getPreUrl());
            }
        }

        @Override
        protected void doOKAction() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                Messages.showErrorDialog(this.getContentPane(), RequestManBundle.message("env.name.empty"), RequestManBundle.message("common.error"));
                return;
            }

            // 检查名称是否重复
            List<Environment> environments = ProjectSettingsManager.getAllEnvironments(project);
            boolean nameExists = environments.stream()
                    .anyMatch(env -> env.getName().equals(name) &&
                            (environment == null || !env.getId().equals(environment.getId())));

            if (nameExists) {
                Messages.showErrorDialog(this.getContentPane(), RequestManBundle.message("env.name.exists"), RequestManBundle.message("common.error"));
                return;
            }

            super.doOKAction();
        }

        public Environment getEnvironment() {
            if (environment == null) {
                // 新建环境
                String name = nameField.getText().trim();
                String preUrl = preUrlField.getText().trim();
                Environment newEnv = new Environment(name, preUrl);
                return newEnv;
            } else {
                // 更新环境
                String name = nameField.getText().trim();
                String preUrl = preUrlField.getText().trim();
                environment.setName(name);
                environment.setPreUrl(preUrl);
                return environment;
            }
        }
    }
} 