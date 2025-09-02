package com.ljh.request.requestman.ui;

import cn.hutool.json.JSONUtil;

import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import com.ljh.request.requestman.util.JsonExampleGenerator;
import com.ljh.request.requestman.util.RequestManBundle;

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

    public JsonBodyPanel(List<ApiParam> bodyParamList, boolean isCustom, JPanel rightPanel) {
        super(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Tab 面板
        JTabbedPane tabPane = new JTabbedPane();
        
        // 参数值Tab：原有json编辑器
        JPanel valuePanel = new JPanel(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);
        valuePanel.add(scrollPane, BorderLayout.CENTER);
        textAreaRef = textArea;

        tabPane.addTab(RequestManBundle.message("json.tab.value"), valuePanel);
        if (!isCustom) {
            // 数据结构Tab：结构树表格
            JsonBodyStructurePanel structurePanel = new JsonBodyStructurePanel(bodyParamList);
            tabPane.addTab(RequestManBundle.message("json.tab.structure"), structurePanel);
        }
        
        // 组装主面板
        mainPanel.add(tabPane, BorderLayout.CENTER);
        
        add(mainPanel);

        // 将功能按钮添加到父面板的右侧面板
        if (rightPanel != null) {
            JButton formatBtn = new JButton(RequestManBundle.message("json.format"));
            JButton autoGenBtn = new JButton(RequestManBundle.message("json.autogen"));
            rightPanel.add(formatBtn);
            rightPanel.add(autoGenBtn);

            // 仅在有body参数时，绑定格式化/自动生成按钮逻辑
            if (bodyParamList != null && !bodyParamList.isEmpty() && textArea != null) {
                final JTextArea finalTextArea = textArea;
                finalTextArea.setText(ApiInfoExtractor.getApiInfoBodyJson(bodyParamList));
                // 格式化功能
                formatBtn.addActionListener((ActionEvent e) -> {
                    String text = finalTextArea.getText();
                    try {
                        String pretty = ApiInfoExtractor.formatJson(text);
                        finalTextArea.setText(pretty);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, RequestManBundle.message("json.format.error"), RequestManBundle.message("common.error"), JOptionPane.ERROR_MESSAGE);
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

    public JTextArea getTextAreaRef() {
        return textAreaRef;
    }
} 