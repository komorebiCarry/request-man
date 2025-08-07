package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 参数面板，展示Query参数表格。参数名、类型、说明只读，参数值可编辑
 * @date 2025/06/18 15:06
 */
public class ParamsPanel extends JPanel {
    /**
     * 参数面板统一尺寸
     */
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);

    /**
     * 参数表格对象，便于持久化操作
     */
    private JTable paramTable;

    /**
     * 构造方法，初始化参数表格。
     *
     * @param paramList 参数列表
     */
    public ParamsPanel(List<ApiParam> paramList) {
        super(new BorderLayout());
        // 顶部标题
        JLabel titleLabel = new JLabel("Query 参数");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        add(titleLabel, BorderLayout.NORTH);
        // 构建参数表格
        paramTable = buildParamTable(paramList);
        JScrollPane scrollPane = new JScrollPane(paramTable);
        add(scrollPane, BorderLayout.CENTER);
        setPreferredSize(PARAM_PANEL_SIZE);
    }

    /**
     * 构建参数表格。
     *
     * @param paramList 参数列表
     * @return JTable对象
     */
    private JTable buildParamTable(List<ApiParam> paramList) {
        String[] columnNames = {"参数名", "参数值", "类型", "说明"};
        Object[][] data = prepareTableData(paramList);
        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只有"参数值"列可编辑
                return column == 1;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /**
     * 准备表格数据。
     *
     * @param paramList 参数列表
     * @return 二维数组数据
     */
    private Object[][] prepareTableData(List<ApiParam> paramList) {
        Object[][] data = new Object[paramList.size()][4];
        for (int i = 0; i < paramList.size(); i++) {
            ApiParam p = paramList.get(i);
            data[i][0] = p.getName();
            data[i][1] = ""; // 参数值初始为空
            data[i][2] = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
            data[i][3] = p.getDescription();
        }
        return data;
    }

    /**
     * 停止表格编辑状态，确保编辑内容写入TableModel
     */
    private void stopTableEditing() {
        if (paramTable != null && paramTable.isEditing()) {
            paramTable.getCellEditor().stopCellEditing();
        }
    }

    /**
     * 获取所有参数值（顺序与构造时参数顺序一致）
     *
     * @return 参数值列表
     */
    public List<String> getParamsValueList() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        List<String> values = new ArrayList<>();
        for (int i = 0; i < paramTable.getRowCount(); i++) {
            Object v = paramTable.getValueAt(i, 1);
            values.add(v != null ? v.toString() : "");
        }
        return values;
    }

    /**
     * 设置所有参数值（顺序与构造时参数顺序一致）
     *
     * @param values 参数值列表
     */
    public void setParamsValueList(List<String> values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < Math.min(paramTable.getRowCount(), values.size()); i++) {
            paramTable.setValueAt(values.get(i), i, 1);
        }
    }

    /**
     * 获取当前表格中的参数对象列表（含参数名、值、类型、说明）
     *
     * @return 参数对象列表
     */
    public List<ApiParam> getParams() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        List<ApiParam> list = new ArrayList<>();
        JTable table = this.paramTable;
        if (table == null) {
            return list;
        }
        int rowCount = table.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = String.valueOf(table.getValueAt(i, 0));
            String value = String.valueOf(table.getValueAt(i, 1));
            String type = String.valueOf(table.getValueAt(i, 2));
            String desc = String.valueOf(table.getValueAt(i, 3));
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
} 