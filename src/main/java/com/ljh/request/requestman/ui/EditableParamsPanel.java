package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 可编辑参数面板，支持所有列可编辑、增删行、右键删除。
 * @date 2025/06/18 15:36
 */
public class EditableParamsPanel extends JPanel {
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);
    private final DefaultTableModel tableModel;
    private final JTable table;
    private javax.swing.event.TableModelListener addRowListener;

    public EditableParamsPanel(List<ApiParam> paramList) {
        super(new BorderLayout());
        String[] columnNames = {"参数名", "参数值", "类型", "说明", ""};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 最后一行为"添加参数"行，所有列可编辑
                if (row == getRowCount() - 1) {
                    return column < 4;
                }
                // 其他行所有列可编辑
                return column < 4;
            }
        };
        // 初始化数据
        if (paramList != null) {
            for (ApiParam p : paramList) {
                tableModel.addRow(new Object[]{p.getName(), p.getValue(), p.getType(), p.getDescription(), ""});
            }
        }
        addEmptyRow();
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        // 监听"添加参数"行输入
        addRowListener = e -> {
            int lastRow = tableModel.getRowCount() - 1;
            if (lastRow < 0) return;
            String name = (String) tableModel.getValueAt(lastRow, 0);
            if (name != null && !name.trim().isEmpty()) {
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
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow < 0 || tableModel.getValueAt(lastRow, 0) != null && !((String) tableModel.getValueAt(lastRow, 0)).isEmpty()) {
            tableModel.addRow(new Object[]{"", "", "string", "", ""});
        }
    }

    /**
     * 获取所有有效参数（不含最后一行空行）
     */
    public List<ApiParam> getParams() {
        List<ApiParam> list = new ArrayList<>();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount - 1; i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String value = (String) tableModel.getValueAt(i, 1);
            String type = (String) tableModel.getValueAt(i, 2);
            String desc = (String) tableModel.getValueAt(i, 3);
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
     * 设置参数数据（用于回显）
     */
    public void setParams(List<ApiParam> paramList) {
        tableModel.removeTableModelListener(addRowListener);
        tableModel.setRowCount(0);
        if (paramList != null) {
            for (ApiParam p : paramList) {
                tableModel.addRow(new Object[]{p.getName(), p.getValue(), p.getType(), p.getDescription(), ""});
            }
        }
        addEmptyRow();
        tableModel.addTableModelListener(addRowListener);
    }
} 