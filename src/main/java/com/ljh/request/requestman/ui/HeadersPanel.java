package com.ljh.request.requestman.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leijianhui
 * @Description 请求头参数面板，支持增删行、可编辑、持久化。
 * @date 2025/06/19 09:36
 */
public class HeadersPanel extends JPanel {
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);
    private final DefaultTableModel tableModel;
    private final JTable table;
    private javax.swing.event.TableModelListener addRowListener;

    public HeadersPanel() {
        super(new BorderLayout());
        String[] columnNames = {"参数名", "参数值", "类型", "说明", ""};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 最后一行为"添加参数"行，只有前两列可编辑
                if (row == getRowCount() - 1) {
                    return column == 0 || column == 1;
                }
                // 其他行前三列可编辑
                return column < 3;
            }
        };
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        // 添加"添加参数"行
        addEmptyRow();
        // 监听"添加参数"行输入
        addRowListener = e -> {
            int lastRow = tableModel.getRowCount() - 1;
            // 防止表格为空时越界访问，符合阿里巴巴规范
            if (lastRow < 0) {
                return;
            }
            String name = (String) tableModel.getValueAt(lastRow, 0);
            String value = (String) tableModel.getValueAt(lastRow, 1);
            if (name != null && !name.trim().isEmpty()) {
                // 有输入时自动添加新行
                addEmptyRow();
            }
        };
        tableModel.addTableModelListener(addRowListener);
        // 右键菜单支持删除行
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除");
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
     * 添加空行（用于"添加参数"）
     */
    private void addEmptyRow() {
        // 只添加一行空行
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow < 0 || tableModel.getValueAt(lastRow, 0) != null && !((String) tableModel.getValueAt(lastRow, 0)).isEmpty()) {
            tableModel.addRow(new Object[]{"", "", "string", ""});
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
     * 获取所有有效Header参数（不含最后一行空行）
     */
    public List<HeaderItem> getHeadersData() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        List<HeaderItem> list = new ArrayList<>();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount - 1; i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String value = (String) tableModel.getValueAt(i, 1);
            String type = (String) tableModel.getValueAt(i, 2);
            String desc = (String) tableModel.getValueAt(i, 3);
            if (name != null && !name.trim().isEmpty()) {
                list.add(new HeaderItem(name, value, type, desc));
            }
        }
        return list;
    }

    /**
     * 设置Header参数（用于持久化恢复）
     */
    public void setHeadersData(List<HeaderItem> headers) {
        tableModel.removeTableModelListener(addRowListener); // 先移除监听
        tableModel.setRowCount(0);
        if (headers != null) {
            for (HeaderItem h : headers) {
                tableModel.addRow(new Object[]{h.name, h.value, h.type, h.desc});
            }
        }
        addEmptyRow();
        tableModel.addTableModelListener(addRowListener); // 再加回监听
    }

    /**
     * 获取所有header参数，返回Map
     */
    public Map<String, String> getHeadersMap() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String value = (String) tableModel.getValueAt(i, 1);
            if (name != null && !name.trim().isEmpty()) {
                map.put(name, value != null ? value : "");
            }
        }
        return map;
    }

    /**
     * Header参数数据结构
     */
    public static class HeaderItem {
        public String name;
        public String value;
        public String type;
        public String desc;

        public HeaderItem(String name, String value, String type, String desc) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.desc = desc;
        }
    }
} 