package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description form-data请求体参数面板，支持文件选择和参数编辑。
 * @date 2025/06/19 09:36
 */
public class FormDataBodyPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;

    public FormDataBodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());

        // 创建表格模型
        String[] columnNames = {"参数名", "参数值", "类型", "说明", "操作"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只有“参数值”列和“操作”列可编辑
                return column == 1 || column == 4;
            }
        };

        // 创建表格
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(120); // 参数名
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // 参数值
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // 类型
        table.getColumnModel().getColumn(3).setPreferredWidth(150); // 说明
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // 操作

        // 设置按钮渲染器和编辑器
        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox(), this));

        // 初始化数据
        for (ApiParam p : bodyParamList) {
            addParamRow(p);
        }

        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加参数");
        JButton removeButton = new JButton("删除参数");

        addButton.addActionListener(e -> addEmptyRow());
        removeButton.addActionListener(e -> removeSelectedRow());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        // 布局
        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * 添加参数行
     */
    private void addParamRow(ApiParam param) {
        String name = param.getName();
        String value = getDefaultValue(param);
        String type = param.getDataType() != null ? param.getDataType().name().toLowerCase() : "string";
        String description = param.getDescription();
        String action = "file".equals(type) ? "选择文件" : "";

        tableModel.addRow(new Object[]{name, value, type, description, action});
    }

    /**
     * 添加空行
     */
    private void addEmptyRow() {
        tableModel.addRow(new Object[]{"", "", "string", "", ""});
    }

    /**
     * 删除选中的行
     */
    private void removeSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow);
        }
    }

    /**
     * 选择文件
     */
    void selectFile(int row) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            tableModel.setValueAt(selectedFile.getAbsolutePath(), row, 1);
        }
    }

    /**
     * 根据参数类型生成form-data参数默认值
     */
    private String getDefaultValue(ApiParam p) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "integer":
                return "0";
            case "boolean":
                return "false";
            case "number":
                return "0.0";
            case "array":
                return "[]";
            case "file":
                return "<请选择文件>";
            default:
                return "";
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
     * 获取所有参数（不含空行）
     */
    public List<ApiParam> getParams() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        List<ApiParam> list = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = String.valueOf(tableModel.getValueAt(i, 0));
            String value = String.valueOf(tableModel.getValueAt(i, 1));
            String type = String.valueOf(tableModel.getValueAt(i, 2));
            String desc = String.valueOf(tableModel.getValueAt(i, 3));

            if (name != null && !name.trim().isEmpty()) {
                ApiParam param = new ApiParam();
                param.setName(name);
                param.setValue(value);
                param.setType(type);
                param.setDescription(desc);
                list.add(param);
            }
        }
        return list;
    }

    /**
     * 按钮渲染器
     */
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null && !value.toString().isEmpty()) {
                setText(value.toString());
                setVisible(true);
            } else {
                setVisible(false);
            }
            return this;
        }
    }

    /**
     * 按钮编辑器
     */
    private static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private FormDataBodyPanel parentPanel;

        public ButtonEditor(JCheckBox checkBox, FormDataBodyPanel parentPanel) {
            super(checkBox);
            this.parentPanel = parentPanel;
            button = new JButton("选择文件");
            button.setOpaque(true);
            setClickCountToStart(1);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value != null && !value.toString().isEmpty()) {
                button.setText(value.toString());
            }

            // 清除之前的事件监听器并添加新的
            for (java.awt.event.ActionListener listener : button.getActionListeners()) {
                button.removeActionListener(listener);
            }

            button.addActionListener(e -> {
                if (parentPanel != null) {
                    parentPanel.selectFile(row);
                }
                fireEditingStopped();
            });

            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }
} 