package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.ApiParamFlattener;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import com.ljh.request.requestman.util.RequestManBundle;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 统一参数表格面板，支持所有参数类型，包括文件选择功能
 * @date 2025/01/27 10:30
 */
public class ParamsTablePanel extends JPanel {

    /**
     * 参数用途枚举
     */
    public enum ParamUsage {
        /**
         * Body form-data - 支持文件选择
         */
        BODY_FORM_DATA,
        /**
         * Body urlencoded - 不支持文件选择
         */
        BODY_URLENCODED,
        /**
         * Headers - 只支持字符串类型
         */
        HEADERS,
        /**
         * Cookies - 只支持字符串类型
         */
        COOKIES,
        /**
         * 普通参数 - 支持所有类型，不支持文件选择
         */
        PARAMS
    }

    /**
     * 所有类型选项（包含文件）
     */
    private static final String[] ALL_TYPE_OPTIONS = {"string", "integer", "boolean", "number", "file"};

    /**
     * 只支持字符串类型
     */
    private static final String[] STRING_ONLY_OPTIONS = {"string"};

    /**
     * 支持除文件外的所有类型
     */
    private static final String[] NO_FILE_TYPE_OPTIONS = {"string", "integer", "boolean", "number"};

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final ParamUsage usage;
    private final String[] typeOptions;
    private final boolean isScanMode; // 是否为扫描模式
    private final Runnable onPathParamsChanged; // Path参数变化时的回调
    private boolean handlingLocationChange = false; // 防重入保护：避免位置变化监听器无限递归

    /**
     * 构造函数
     *
     * @param usage               参数用途
     * @param paramList           参数列表
     * @param isScanMode          是否为扫描模式
     * @param onPathParamsChanged Path参数变化时的回调
     */
    public ParamsTablePanel(ParamUsage usage, List<ApiParam> paramList, boolean isScanMode, Runnable onPathParamsChanged) {
        super(new BorderLayout());
        this.usage = usage;
        this.isScanMode = isScanMode;
        this.onPathParamsChanged = onPathParamsChanged;

        // 根据用途确定类型选项
        if (usage == ParamUsage.HEADERS || usage == ParamUsage.COOKIES) {
            this.typeOptions = STRING_ONLY_OPTIONS;
        } else if (usage == ParamUsage.BODY_URLENCODED) {
            this.typeOptions = NO_FILE_TYPE_OPTIONS;
        } else if (usage == ParamUsage.PARAMS) {
            this.typeOptions = NO_FILE_TYPE_OPTIONS;
        } else {
            this.typeOptions = ALL_TYPE_OPTIONS;
        }

        // 创建表格模型
        String[] columnNames = getColumnNames();
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 根据用途确定哪些列可编辑
                if (usage == ParamUsage.BODY_FORM_DATA) {
                    // form-data: 参数名、参数值、类型、说明、操作列均可编辑（操作列需可编辑以触发ButtonEditor）
                    return column < 5;
                } else if (usage == ParamUsage.PARAMS) {
                    // PARAMS: 根据扫描模式决定编辑权限
                    if (isScanMode) {
                        // 扫描模式：Path参数不可编辑，Query参数可编辑
                        if (column == 3) { // 位置列
                            return false; // 位置列不可编辑
                        }
                        // 检查当前行是否为Path参数
                        Object locationValue = tableModel.getValueAt(row, 3);
                        if (locationValue != null && "Path".equals(locationValue.toString())) {
                            // Path参数：参数名、类型、位置不可编辑，参数值、说明可编辑
                            return column == 1 || column == 4;
                        } else {
                            // Query参数：所有列都可编辑
                            return column < columnNames.length;
                        }
                    } else {
                        // 自定义模式：所有列都可编辑
                        return column < columnNames.length;
                    }
                } else if (usage == ParamUsage.HEADERS || usage == ParamUsage.COOKIES) {
                    // Headers和Cookies: 所有列都可编辑
                    return column < columnNames.length;
                } else {
                    // 其他: 所有列都可编辑
                    return column < columnNames.length;
                }
            }
        };

        // 创建表格
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);

        // 设置行高，确保下拉框和字体完全显示
        table.setRowHeight(28);

        // 设置列宽
        setupColumnWidths();

        // 设置渲染器和编辑器
        setupRenderersAndEditors();

        // 初始化数据
        initializeData(paramList);

        // 添加空行
        addEmptyRow();

        // 添加按钮面板（如果需要）
        if (usage == ParamUsage.BODY_FORM_DATA | usage == ParamUsage.BODY_URLENCODED || usage == ParamUsage.PARAMS || usage == ParamUsage.HEADERS || usage == ParamUsage.COOKIES) {
            addButtonPanel();
        }

        // 为form-data类型添加类型变化监听器，实时更新按钮状态
        if (usage == ParamUsage.BODY_FORM_DATA) {
            addTypeChangeListener();
        }

        // 注意：位置变化监听器已移至AutoSaveManager统一处理，避免重复监听
        // 为PARAMS类型添加位置变化监听器，实时更新URL
        // if (usage == ParamUsage.PARAMS && !isScanMode) {
        //     addLocationChangeListener();
        // }

        // 布局
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * 构造函数重载（向后兼容）
     *
     * @param usage      参数用途
     * @param paramList  参数列表
     * @param isScanMode 是否为扫描模式
     */
    public ParamsTablePanel(ParamUsage usage, List<ApiParam> paramList, boolean isScanMode) {
        this(usage, paramList, isScanMode, null);
    }

    /**
     * 构造函数重载（向后兼容，用于Headers、Cookies、Body等）
     *
     * @param usage     参数用途
     * @param paramList 参数列表
     */
    public ParamsTablePanel(ParamUsage usage, List<ApiParam> paramList) {
        this(usage, paramList, false, null);
    }

    /**
     * 根据用途获取列名
     */
    private String[] getColumnNames() {
        switch (usage) {
            case BODY_FORM_DATA:
                return new String[]{RequestManBundle.message("params.name"), RequestManBundle.message("params.value"), RequestManBundle.message("params.type"), RequestManBundle.message("params.desc"), RequestManBundle.message("params.action")};
            case PARAMS:
                return new String[]{RequestManBundle.message("params.name"), RequestManBundle.message("params.value"), RequestManBundle.message("params.type"), RequestManBundle.message("params.location"), RequestManBundle.message("params.desc")};
            case BODY_URLENCODED:
            case HEADERS:
            case COOKIES:
                return new String[]{RequestManBundle.message("params.name"), RequestManBundle.message("params.value"), RequestManBundle.message("params.type"), RequestManBundle.message("params.desc")};
            default:
                return new String[]{RequestManBundle.message("params.name"), RequestManBundle.message("params.value"), RequestManBundle.message("params.type"), RequestManBundle.message("params.desc")};
        }
    }

    /**
     * 设置列宽
     */
    private void setupColumnWidths() {
        switch (usage) {
            case BODY_FORM_DATA:
                table.getColumnModel().getColumn(0).setPreferredWidth(120); // 参数名
                table.getColumnModel().getColumn(1).setPreferredWidth(200); // 参数值
                table.getColumnModel().getColumn(2).setPreferredWidth(100);  // 类型
                table.getColumnModel().getColumn(3).setPreferredWidth(150); // 说明
                table.getColumnModel().getColumn(4).setPreferredWidth(80);  // 操作
                break;
            case PARAMS:
                table.getColumnModel().getColumn(0).setPreferredWidth(120); // 参数名
                table.getColumnModel().getColumn(1).setPreferredWidth(200); // 参数值
                table.getColumnModel().getColumn(2).setPreferredWidth(100);  // 类型
                table.getColumnModel().getColumn(3).setPreferredWidth(100);  // 位置
                table.getColumnModel().getColumn(4).setPreferredWidth(150); // 说明
                break;
            default:
                table.getColumnModel().getColumn(0).setPreferredWidth(120); // 参数名
                table.getColumnModel().getColumn(1).setPreferredWidth(200); // 参数值
                table.getColumnModel().getColumn(2).setPreferredWidth(100);  // 类型
                table.getColumnModel().getColumn(3).setPreferredWidth(150); // 说明
                break;
        }
    }

    /**
     * 设置渲染器和编辑器
     */
    private void setupRenderersAndEditors() {
        // 为类型列设置下拉框渲染器和编辑器
        table.getColumnModel().getColumn(2).setCellRenderer(new TypeComboBoxRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new TypeComboBoxEditor());

        // 为位置列设置下拉框渲染器和编辑器（仅PARAMS）
        if (usage == ParamUsage.PARAMS) {
            table.getColumnModel().getColumn(3).setCellRenderer(new LocationComboBoxRenderer());
            table.getColumnModel().getColumn(3).setCellEditor(new LocationComboBoxEditor());
        }

        // 为操作列设置按钮渲染器和编辑器（仅form-data）
        if (usage == ParamUsage.BODY_FORM_DATA) {
            table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox(), this));
        }
    }

    /**
     * 初始化数据
     */
    private void initializeData(List<ApiParam> paramList) {
        if (paramList != null) {
            // 使用展开后的扁平参数列表
            List<ApiParam> flattenedParams = ApiParamFlattener.flattenParams(paramList);
            for (ApiParam p : flattenedParams) {
                addParamRow(p);
            }
        }
    }

    /**
     * 添加参数行
     */
    private void addParamRow(ApiParam param) {
        String name = param.getName();
        String value = StringUtils.isBlank(param.getValue()) ? getDefaultValue(param) : param.getValue();
        String type = param.getDataType() != null ? param.getDataType().name().toLowerCase() : "string";
        String description = param.getDescription();

        if (usage == ParamUsage.BODY_FORM_DATA) {
            // 为form-data类型，操作列显示"选择文件"按钮
            String action = RequestManBundle.message("params.chooseFile");
            tableModel.addRow(new Object[]{name, value, type, description, action});
        } else if (usage == ParamUsage.PARAMS) {
            // 为PARAMS类型，位置列显示参数位置
            String location = getLocationFromType(param.getType());
            tableModel.addRow(new Object[]{name, value, type, location, description});
        } else {
            tableModel.addRow(new Object[]{name, value, type, description});
        }
    }

    /**
     * 添加空行
     */
    private void addEmptyRow() {
        if (usage == ParamUsage.BODY_FORM_DATA) {
            tableModel.addRow(new Object[]{"", "", "string", "", RequestManBundle.message("params.chooseFile")});
        } else if (usage == ParamUsage.PARAMS) {
            tableModel.addRow(new Object[]{"", "", "string", "Query", ""});
        } else {
            tableModel.addRow(new Object[]{"", "", "string", ""});
        }
    }

    /**
     * 添加按钮面板（仅form-data需要）
     */
    private void addButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton(RequestManBundle.message("params.add"));
        JButton removeButton = new JButton(RequestManBundle.message("params.remove"));

        addButton.addActionListener(e -> addEmptyRow());
        removeButton.addActionListener(e -> removeSelectedRow());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        add(buttonPanel, BorderLayout.NORTH);
    }

    /**
     * 添加类型变化监听器，实时更新按钮状态
     */
    private void addTypeChangeListener() {
        tableModel.addTableModelListener(e -> {
            // 只监听类型列的变化
            if (e.getColumn() == 2) { // 类型列是第3列（索引2）
                // 强制重绘操作列，更新按钮状态
                table.repaint();
            }
        });
    }

    /**
     * 添加位置变化监听器，实时更新URL
     */
    private void addLocationChangeListener() {
        tableModel.addTableModelListener(e -> {
            // 防重入保护：如果正在处理位置变化，则跳过
            if (handlingLocationChange) {
                return;
            }

            // 监听位置列变化（从Query切换到Path或从Path切换到Query）
            if (e.getColumn() == 3) { // 位置列是第4列（索引3）
                handlingLocationChange = true;
                try {
                    if (onPathParamsChanged != null) {
                        onPathParamsChanged.run();
                    }
                } finally {
                    handlingLocationChange = false;
                }
                return;
            }

            // 监听参数名列（索引0）和参数值列（索引1）的变化
            if (e.getColumn() == 0 || e.getColumn() == 1) {
                // 检查当前行是否为Path参数
                int row = e.getFirstRow();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    Object locationValue = tableModel.getValueAt(row, 3); // 位置列
                    if (locationValue != null && "Path".equals(locationValue.toString())) {
                        // 如果是Path参数，则触发URL更新
                        handlingLocationChange = true;
                        try {
                            if (onPathParamsChanged != null) {
                                onPathParamsChanged.run();
                            }
                        } finally {
                            handlingLocationChange = false;
                        }
                    }
                }
            }
        });
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
        fileChooser.setDialogTitle(RequestManBundle.message("params.chooseFile"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            tableModel.setValueAt(selectedFile.getAbsolutePath(), row, 1);
        }
    }

    /**
     * 根据参数类型生成默认值
     */
    private String getDefaultValue(ApiParam p) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "file":
                return "<" + RequestManBundle.message("params.chooseFile") + ">";
            default:
                return "";
        }
    }

    /**
     * 根据参数的type字段获取位置显示值
     */
    private String getLocationFromType(String type) {
        if (type != null && type.equals("Path")) {
            return "Path";
        }
        return "Query";
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
     * 获取所有参数
     */
    public List<ApiParam> getParams() {
        // 获取数据前，主动结束表格编辑，确保编辑内容写入TableModel，避免数据丢失
        stopTableEditing();

        List<ApiParam> list = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = "";
            String value = "";
            String dataType = "";
            String type = "";
            String desc = "";

            if (usage == ParamUsage.PARAMS) {
                name = String.valueOf(tableModel.getValueAt(i, 0));
                value = String.valueOf(tableModel.getValueAt(i, 1));
                dataType = String.valueOf(tableModel.getValueAt(i, 2));
                type = String.valueOf(tableModel.getValueAt(i, 3));
                desc = String.valueOf(tableModel.getValueAt(i, 4));

            } else {
                name = String.valueOf(tableModel.getValueAt(i, 0));
                value = String.valueOf(tableModel.getValueAt(i, 1));
                dataType = String.valueOf(tableModel.getValueAt(i, 2));
                desc = String.valueOf(tableModel.getValueAt(i, 3));
            }
            if (name != null && !name.trim().isEmpty()) {
                ApiParam param = new ApiParam();
                param.setName(name);
                param.setValue(value);
                param.setDataType(ParamDataType.getParamDataType(dataType));
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
        tableModel.setRowCount(0);
        if (paramList != null) {
            // 使用展开后的扁平参数列表
            List<ApiParam> flattenedParams = ApiParamFlattener.flattenParams(paramList);
            for (ApiParam p : flattenedParams) {
                addParamRow(p);
            }
        }
        addEmptyRow();
    }

    /**
     * 获取表格引用
     */
    public JTable getTable() {
        return table;
    }

    /**
     * 类型下拉框渲染器
     */
    private class TypeComboBoxRenderer extends JComboBox<String> implements javax.swing.table.TableCellRenderer {
        public TypeComboBoxRenderer() {
            super(typeOptions);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                setSelectedItem(value.toString());
            } else {
                setSelectedItem("string");
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * 类型下拉框编辑器
     */
    private class TypeComboBoxEditor extends DefaultCellEditor {
        public TypeComboBoxEditor() {
            super(new JComboBox<>(typeOptions));
        }
    }

    /**
     * 位置下拉框渲染器（仅PARAMS使用）
     */
    private class LocationComboBoxRenderer extends JComboBox<String> implements javax.swing.table.TableCellRenderer {
        private static final String[] LOCATION_OPTIONS = {"Path", "Query"};

        public LocationComboBoxRenderer() {
            super(LOCATION_OPTIONS);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                setSelectedItem(value.toString());
            } else {
                setSelectedItem("Query");
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * 位置下拉框编辑器（仅PARAMS使用）
     */
    private class LocationComboBoxEditor extends DefaultCellEditor {
        private static final String[] LOCATION_OPTIONS = {"Path", "Query"};

        public LocationComboBoxEditor() {
            super(new JComboBox<>(LOCATION_OPTIONS));
        }
    }

    /**
     * 按钮渲染器（仅form-data使用）
     */
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setText("选择文件");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // 获取当前行的类型列值
            String type = "";
            if (row >= 0 && row < table.getRowCount()) {
                Object typeValue = table.getValueAt(row, 2); // 类型列是第3列（索引2）
                if (typeValue != null) {
                    type = typeValue.toString();
                }
            }

            // 根据类型设置按钮状态
            if ("file".equals(type)) {
                setEnabled(true);
                setText("选择文件");
            } else {
                setEnabled(false);
                setText("选择文件");
            }

            // 设置选中状态的背景和前景色
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * 按钮编辑器（仅form-data使用）
     */
    private static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JButton button;
        private ParamsTablePanel parentPanel;

        public ButtonEditor(JCheckBox checkBox, ParamsTablePanel parentPanel) {
            this.parentPanel = parentPanel;
            button = new JButton("选择文件");
            button.setOpaque(true);
            button.setFocusPainted(false); // 避免焦点边框干扰
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // 获取当前行的类型列值
            String type = "";
            if (row >= 0 && row < table.getRowCount()) {
                Object typeValue = table.getValueAt(row, 2); // 类型列是第3列（索引2）
                if (typeValue != null) {
                    type = typeValue.toString();
                }
            }

            // 根据类型设置按钮状态
            if ("file".equals(type)) {
                button.setEnabled(true);
                button.setText("选择文件");

                // 清除之前的事件监听器并添加新的
                for (java.awt.event.ActionListener listener : button.getActionListeners()) {
                    button.removeActionListener(listener);
                }

                // 添加点击事件监听器
                button.addActionListener(e -> {
                    if (parentPanel != null) {
                        parentPanel.selectFile(row);
                    }
                    fireEditingStopped();
                });
            } else {
                button.setEnabled(false);
                button.setText("选择文件");

                // 清除事件监听器
                for (java.awt.event.ActionListener listener : button.getActionListeners()) {
                    button.removeActionListener(listener);
                }
            }

            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "选择文件";
        }
    }
}

