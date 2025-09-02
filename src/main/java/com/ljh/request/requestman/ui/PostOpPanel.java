package com.ljh.request.requestman.ui;

import cn.hutool.json.JSONUtil;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.util.JsonExampleGenerator;
import com.ljh.request.requestman.ui.JsonPathExtractorDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import com.ljh.request.requestman.util.RequestManBundle;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 后置操作参数面板，支持变量名称、类型、变量值的增删改查。
 * @date 2025/06/17 19:48
 */
public class PostOpPanel extends JPanel {
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);
    private static final String[] TYPE_OPTIONS = {"JSONPath", "TEXT"};
    private final DefaultTableModel tableModel;
    private final JTable table;
    private javax.swing.event.TableModelListener addRowListener;

    // 响应面板引用，用于获取响应内容
    private ResponseCollapsePanel responsePanel;
    // 当前接口信息，用于获取响应定义
    private ApiInfo currentApiInfo;

    public PostOpPanel() {
        super(new BorderLayout());
        String[] columnNames = {RequestManBundle.message("postop.col.name"), RequestManBundle.message("postop.col.type"), RequestManBundle.message("postop.col.value")};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 最后一行为"添加变量"行，只有前两列可编辑
                if (row == getRowCount() - 1) {
                    return column == 0 || column == 2;
                }
                // 变量名称、类型、变量值可编辑
                return column < 3;
            }
        };
        table = new JTable(tableModel) {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 1) {
                    JComboBox<String> comboBox = new JComboBox<>(TYPE_OPTIONS);
                    return new DefaultCellEditor(comboBox);
                }
                if (column == 2) {
                    // 检查当前行的类型是否为JSONPath表达式
                    String type = (String) getValueAt(row, 1);
                    if ("JSONPath".equals(type)) {
                        return new JsonPathValueEditor();
                    }
                }
                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 2) {
                    // 检查当前行的类型是否为JSONPath表达式
                    String type = (String) getValueAt(row, 1);
                    if ("JSONPath".equals(type)) {
                        return new JsonPathValueRenderer();
                    }
                }
                return super.getCellRenderer(row, column);
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        // 添加"添加变量"行
        addEmptyRow();
        // 监听"添加变量"行输入
        addRowListener = e -> {
            int lastRow = tableModel.getRowCount() - 1;
            // 防止表格为空时越界访问
            if (lastRow < 0) {
                return;
            }
            String name = (String) tableModel.getValueAt(lastRow, 0);
            if (name != null && !name.trim().isEmpty()) {
                // 有输入时自动添加新行
                addEmptyRow();
            }
        };
        tableModel.addTableModelListener(addRowListener);

        // 监听类型变化，当类型变为JSONPath表达式时刷新表格
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 1) { // 类型列
                table.repaint();
            }
        });

        // 右键菜单支持删除行
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem(RequestManBundle.message("common.delete"));
        deleteItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount() - 1) {
                tableModel.removeRow(row);
            }
        });
        popupMenu.add(deleteItem);
        table.setComponentPopupMenu(popupMenu);
        // 右键点击时自动选中当前行，保证删除操作生效
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        setPreferredSize(PARAM_PANEL_SIZE);
    }

    /**
     * 设置响应面板引用
     */
    public void setResponsePanel(ResponseCollapsePanel responsePanel) {
        this.responsePanel = responsePanel;
    }

    /**
     * 设置当前接口信息
     */
    public void setCurrentApiInfo(ApiInfo apiInfo) {
        this.currentApiInfo = apiInfo;
    }

    /**
     * 添加空行（用于"添加变量"）
     */
    private void addEmptyRow() {
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow < 0 || tableModel.getValueAt(lastRow, 0) != null && !((String) tableModel.getValueAt(lastRow, 0)).isEmpty()) {
            tableModel.addRow(new Object[]{"", TYPE_OPTIONS[0], "", ""});
        }
    }

    /**
     * 获取所有有效后置操作参数（不含最后一行空行）
     */
    public List<PostOpItem> getPostOpData() {
        List<PostOpItem> list = new ArrayList<>();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount - 1; i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String type = (String) tableModel.getValueAt(i, 1);
            String value = (String) tableModel.getValueAt(i, 2);
            if (name != null && !name.trim().isEmpty()) {
                list.add(new PostOpItem(name, type, value));
            }
        }
        return list;
    }

    /**
     * 设置后置操作参数（用于持久化恢复）
     */
    public void setPostOpData(List<PostOpItem> items) {
        tableModel.removeTableModelListener(addRowListener); // 先移除监听
        tableModel.setRowCount(0);
        if (items != null) {
            for (PostOpItem item : items) {
                tableModel.addRow(new Object[]{item.name, item.type, item.value, ""});
            }
        }
        addEmptyRow();
        tableModel.addTableModelListener(addRowListener); // 再加回监听
    }

    /**
     * JSONPath值编辑器，包含输入框和提取器按钮
     */
    private class JsonPathValueEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JTextField textField;
        private final JButton extractButton;

        public JsonPathValueEditor() {
            panel = new JPanel(new BorderLayout(2, 0));
            textField = new JTextField();
            extractButton = new JButton("📋");
            extractButton.setToolTipText(RequestManBundle.message("jsonpath.title"));
            extractButton.setPreferredSize(new Dimension(25, 20));
            extractButton.setFont(new Font("Dialog", Font.PLAIN, 10));

            panel.add(textField, BorderLayout.CENTER);
            panel.add(extractButton, BorderLayout.EAST);

            extractButton.addActionListener(e -> {
                openJsonPathExtractor();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText(value != null ? value.toString() : "");
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        private void openJsonPathExtractor() {
            String responseText = "";

            // 优先获取返回响应内容
            if (responsePanel != null) {
                responseText = responsePanel.getResponseText();
            }

            // 如果没有返回响应内容（为空或默认提示），则使用响应定义
            if (responseText == null || responseText.trim().isEmpty() ||
                    "点击'发送'按钮获取返回结果".equals(responseText.trim())) {
                // 获取响应定义作为示例
                responseText = getResponseDefinitionJson();
            }

            // 获取当前窗口作为父窗口
            Window parentWindow = SwingUtilities.getWindowAncestor(PostOpPanel.this);
            Frame parentFrame = null;
            if (parentWindow instanceof Frame) {
                parentFrame = (Frame) parentWindow;
            }

            JsonPathExtractorDialog dialog = new JsonPathExtractorDialog(parentFrame, StringUtils.isBlank(responseText) ? "" : JSONUtil.toJsonPrettyStr(responseText));
            dialog.setVisible(true);

            // 只有当用户确认了选择且有选择路径时才设置值
            if (dialog.isConfirmed()) {
                String selectedPath = dialog.getSelectedJsonPath();
                if (selectedPath != null && !selectedPath.trim().isEmpty()) {
                    textField.setText(selectedPath);
                }
            }
        }
    }

    /**
     * JSONPath值渲染器，显示输入框和提取器按钮
     */
    private class JsonPathValueRenderer extends JPanel implements TableCellRenderer {
        private final JTextField textField;
        private final JButton extractButton;

        public JsonPathValueRenderer() {
            setLayout(new BorderLayout(2, 0));
            textField = new JTextField();
            textField.setEditable(false);
            extractButton = new JButton("📋");
            extractButton.setToolTipText(RequestManBundle.message("jsonpath.extractor.tooltip"));
            extractButton.setPreferredSize(new Dimension(25, 20));
            extractButton.setFont(new Font("Dialog", Font.PLAIN, 10));

            add(textField, BorderLayout.CENTER);
            add(extractButton, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            textField.setText(value != null ? value.toString() : "");

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                textField.setBackground(table.getSelectionBackground());
                textField.setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                textField.setBackground(table.getBackground());
                textField.setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * 获取响应定义的JSON格式
     */
    private String getResponseDefinitionJson() {
        if (currentApiInfo != null && currentApiInfo.getResponseParams() != null && !currentApiInfo.getResponseParams().isEmpty()) {
            // 使用JsonExampleGenerator生成响应定义的JSON
            return JsonExampleGenerator.genJsonWithComment(currentApiInfo.getResponseParams(), 0);
        }
        // 如果没有响应定义，返回空字符串
        return "";
    }

    /**
     * 后置操作参数数据结构
     */
    public static class PostOpItem  implements Serializable {

        private static final long serialVersionUID = 7929475524342962850L;
        public String name;
        public String type;
        public String value;

        public PostOpItem(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
} 