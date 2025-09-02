package com.ljh.request.requestman.ui;

import javax.swing.*;
import java.awt.*;
import com.ljh.request.requestman.util.RequestManBundle;

/**
 * @author leijianhui
 * @Description 前置操作参数面板，展示前置操作相关参数。
 * @date 2025/06/17 16:32
 */
public class PreOpPanel extends JPanel {
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);

    public PreOpPanel() {
        super(new BorderLayout());
        String[] columnNames = {RequestManBundle.message("preop.col.name"), RequestManBundle.message("preop.col.param"), RequestManBundle.message("preop.col.desc"), ""};
        Object[][] data = {};
        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        setPreferredSize(PARAM_PANEL_SIZE);
    }
} 