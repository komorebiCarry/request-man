package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description x-www-form-urlencoded请求体参数面板，展示参数表格。
 * @date 2025/06/19 09:36
 */
public class XWwwFormUrlencodedBodyPanel extends JPanel {
    public XWwwFormUrlencodedBodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());
        String[] columnNames = {"参数名", "参数值", "类型", "说明"};
        Object[][] data = new Object[bodyParamList.size()][4];
        for (int i = 0; i < bodyParamList.size(); i++) {
            ApiParam p = bodyParamList.get(i);
            data[i][0] = p.getName();
            data[i][1] = getDefaultValue(p);
            data[i][2] = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
            data[i][3] = p.getDescription();
        }
        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只有"参数值"列可编辑
                return column == 1;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 根据参数类型生成x-www-form-urlencoded参数默认值
     */
    private Object getDefaultValue(ApiParam p) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "integer":
                return 0;
            case "boolean":
                return false;
            case "number":
                return 0.0;
            case "array":
                return "[]";
            case "file":
                return "<file>";
            default:
                return "";
        }
    }

    /**
     * 停止表格编辑状态，确保编辑内容写入TableModel
     */
    private void stopTableEditing() {
        JTable table = (JTable) ((JScrollPane) getComponent(0)).getViewport().getView();
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
        JTable table = (JTable) ((JScrollPane) getComponent(0)).getViewport().getView();
        for (int i = 0; i < table.getRowCount(); i++) {
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