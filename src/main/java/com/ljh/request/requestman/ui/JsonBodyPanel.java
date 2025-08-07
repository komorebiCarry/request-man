package com.ljh.request.requestman.ui;

import cn.hutool.json.JSONUtil;
import com.ljh.request.requestman.enums.ContentType;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.JsonExampleGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leijianhui
 * @Description JSON请求体参数面板，支持JSON文本编辑与格式化。
 * @date 2025/06/17 19:48
 */
public class JsonBodyPanel extends JPanel {
    /**
     * json 编辑区文本域，便于持久化操作
     */
    private JTextArea textAreaRef;

    public JsonBodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());
        // 顶部Tab和右上角ContentType选择+功能按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        JTabbedPane tabPane = new JTabbedPane();
        // 参数值Tab：原有json编辑器
        JPanel valuePanel = new JPanel(new BorderLayout());
        JTextArea textArea = null;
        JScrollPane scrollPane = null;
        if (bodyParamList != null && !bodyParamList.isEmpty()) {
            textArea = new JTextArea();
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            scrollPane = new JScrollPane(textArea);
            valuePanel.add(scrollPane, BorderLayout.CENTER);
            textAreaRef = textArea;
        } else {
            valuePanel.add(new JLabel("该请求没有Body", JLabel.CENTER), BorderLayout.CENTER);
            textAreaRef = null;
        }
        tabPane.addTab("参数值", valuePanel);
        // 数据结构Tab：结构树表格
        JsonBodyStructurePanel structurePanel = new JsonBodyStructurePanel(bodyParamList);
        tabPane.addTab("数据结构", structurePanel);
        topPanel.add(tabPane, BorderLayout.WEST);
        JComboBox<ContentType> contentTypeBox = new JComboBox<>(ContentType.values());
        contentTypeBox.setSelectedItem(ContentType.APPLICATION_JSON);
        // 右上角功能按钮
        JButton formatBtn = new JButton("格式化");
        JButton autoGenBtn = new JButton("自动生成");
        // 右上角功能按钮和ContentType选择器
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.add(formatBtn);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(autoGenBtn);
        rightPanel.add(Box.createHorizontalStrut(8));
        // ContentType下拉框只显示label，鼠标悬停显示完整value
        contentTypeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContentType) {
                    ContentType ct = (ContentType) value;
                    label.setText(ct.getLabel());
                    label.setToolTipText(ct.getValue());
                }
                return label;
            }
        });
        contentTypeBox.setMaximumSize(new Dimension(140, 28)); // 限制最大宽度
        rightPanel.add(contentTypeBox);
        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        // 内容区：Tab切换显示
        add(tabPane, BorderLayout.CENTER);
        // 仅在有body参数时，绑定格式化/自动生成按钮逻辑
        if (bodyParamList != null && !bodyParamList.isEmpty() && textArea != null) {
            final JTextArea finalTextArea = textArea;
            StringBuilder json = new StringBuilder();
            if (bodyParamList.size() == 1 && bodyParamList.get(0).getChildren() != null && !bodyParamList.get(0).getChildren().isEmpty()) {
                ApiParam apiParam = bodyParamList.get(0);
                boolean isArray = false;
                int index = 0;
                if (ParamDataType.ARRAY.equals(apiParam.getDataType())) {
                    isArray = true;
                    json.append("[");
                    json.append("\n");
                    index = 2;
                }
                json.append(JsonExampleGenerator.genJsonWithComment(bodyParamList.get(0).getChildren(), index));
                if (isArray) {
                    json.append("\n");
                    json.append("]");
                }
            } else {
                json.append(JsonExampleGenerator.genJsonWithComment(bodyParamList, 0));
            }
            finalTextArea.setText(json.toString());
            // 格式化功能
            formatBtn.addActionListener((ActionEvent e) -> {
                String text = finalTextArea.getText();
                try {
                    String pretty = formatJson(text);
                    finalTextArea.setText(pretty);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "JSON格式错误，无法格式化！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            });
            // 自动生成功能
            autoGenBtn.addActionListener((ActionEvent e) -> {
                String json2;
                if (bodyParamList.size() == 1 && bodyParamList.get(0).getChildren() != null && !bodyParamList.get(0).getChildren().isEmpty()) {
                    json2 = JsonExampleGenerator.generateRandomJson(bodyParamList.get(0).getChildren());
                } else {
                    json2 = JsonExampleGenerator.generateRandomJson(bodyParamList);
                }
                finalTextArea.setText(json2);
            });
        }
    }

    /**
     * 使用Hutool标准格式化JSON字符串，格式化后去除冒号和逗号后的所有空格，生成极致紧凑的json
     */
    private String formatJson(String json) {
        try {
            // 去除所有换行和多余空白
            String compact = json.replaceAll("\\s*\\n\\s*", "");
            // 去除冒号和逗号后的所有空格
            compact = compact.replaceAll(":\\s+", ":").replaceAll(",\\s+", ",");
            return JSONUtil.formatJsonStr(compact);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 根据参数列表生成简单JSON示例
     */
    private String genJsonExample(List<ApiParam> params) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (ApiParam p : params) {
            String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
            Object value = "";
            switch (type) {
                case "integer":
                    value = 0;
                    break;
                case "boolean":
                    value = false;
                    break;
                case "number":
                    value = 0.0;
                    break;
                case "array":
                    value = new Object[0];
                    break;
                case "file":
                    value = "<file>";
                    break;
                default:
                    value = "";
            }
            map.put(p.getName(), value);
        }
        // 简单转json字符串
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("  \"").append(entry.getKey()).append("\": ");
            Object v = entry.getValue();
            if (v instanceof String) sb.append('"').append(v).append('"');
            else if (v instanceof Object[] arr && arr.length == 0) sb.append("[]");
            else sb.append(v);
            if (i < map.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取json编辑区内容
     *
     * @return json文本
     */
    public String getJsonText() {
        return textAreaRef != null ? textAreaRef.getText() : "";
    }

    /**
     * 设置json编辑区内容
     *
     * @param text json文本
     */
    public void setJsonText(String text) {
        if (textAreaRef != null) {
            textAreaRef.setText(text != null ? text : "");
        }
    }
} 