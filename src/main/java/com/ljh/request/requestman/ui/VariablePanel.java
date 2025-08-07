package com.ljh.request.requestman.ui;

import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.util.VariableManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leijianhui
 * @Description 项目变量管理面板，支持变量的增删改查与持久化。
 * @date 2025/06/19 09:36
 */
public class VariablePanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton addButton;
    private final JButton deleteButton;
    private final JButton saveButton;
    private final JButton clearButton;
    // 静态保存唯一实例，便于外部调用自动刷新
    private static VariablePanel instance;

    /**
     * 当前项目对象
     */
    private Project currentProject;

    public VariablePanel(Project project) {
        super(new BorderLayout());
        this.currentProject = project;
        instance = this;

        String[] columnNames = {"变量名", "变量值"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 按钮区
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton = new JButton("新增");
        deleteButton = new JButton("删除");
        saveButton = new JButton("保存");
        clearButton = new JButton("清空");
        btnPanel.add(addButton);
        btnPanel.add(deleteButton);
        btnPanel.add(saveButton);
        btnPanel.add(clearButton);
        add(btnPanel, BorderLayout.SOUTH);

        // 加载变量
        reloadTable();

        // 新增按钮事件
        addButton.addActionListener(e -> {
            tableModel.addRow(new Object[]{"", ""});
        });
        // 删除按钮事件
        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRow(row);
            }
        });
        // 保存按钮事件
        saveButton.addActionListener(e -> saveAll());
        // 清空按钮事件
        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "确定要清空所有变量吗？", "确认", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (currentProject != null) {
                    VariableManager.clear(currentProject);
                }
                reloadTable();
            }
        });
    }

    /**
     * 重新加载表格数据
     */
    private void reloadTable() {
        tableModel.setRowCount(0);
        if (currentProject != null) {
            Map<String, String> vars = VariableManager.getAll(currentProject);
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }

    /**
     * 外部调用：如果实例存在则刷新表格
     */
    public static void reloadIfExists() {
        if (instance != null) {
            instance.reloadTable();
        }
    }

    /**
     * 停止表格编辑状态，确保编辑内容写入TableModel
     */
    private void stopTableEditing() {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * 保存所有变量到项目变量池，只保存表格中显示的变量
     */
    private void saveAll() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "无法获取当前项目，保存失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveAllInternal();
        JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        reloadTable();
    }

    /**
     * 内部保存方法，供apply调用，不显示提示信息
     */
    public void saveAllOnApply() {
        if (currentProject == null) {
            return;
        }

        saveAllInternal();
    }

    /**
     * 内部保存逻辑
     */
    private void saveAllInternal() {
        // 保存前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        // 清空变量池
        VariableManager.clear(currentProject);
        // 只保存表格中显示的变量
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = String.valueOf(tableModel.getValueAt(i, 0)).trim();
            String value = String.valueOf(tableModel.getValueAt(i, 1));
            if (!name.isEmpty()) {
                VariableManager.put(currentProject, name, value);
            }
        }
    }

    /**
     * 检查是否有未保存的修改
     */
    public boolean hasUnsavedChanges() {
        if (currentProject == null) {
            return false;
        }

        // 检查前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        // 获取当前保存的变量
        Map<String, String> savedVars = VariableManager.getAll(currentProject);

        // 获取表格中的变量
        Map<String, String> tableVars = new HashMap<>();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = String.valueOf(tableModel.getValueAt(i, 0)).trim();
            String value = String.valueOf(tableModel.getValueAt(i, 1));
            if (!name.isEmpty()) {
                tableVars.put(name, value);
            }
        }

        // 比较两个Map是否相同
        return !savedVars.equals(tableVars);
    }
}