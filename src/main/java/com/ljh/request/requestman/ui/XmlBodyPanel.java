package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author leijianhui
 * @Description XML请求体面板，支持XML内容编辑和格式化。
 * @date 2025/06/19 09:36
 */
public class XmlBodyPanel extends JPanel {
    /**
     * XML文本区域
     */
    private final JTextArea xmlTextArea;
    /**
     * 滚动面板
     */
    private final JScrollPane scrollPane;
    /**
     * 格式化按钮
     */
    private final JButton formatButton;
    /**
     * 清空按钮
     */
    private final JButton clearButton;

    /**
     * 构造方法，初始化XML编辑面板。
     *
     * @param bodyParamList 请求体参数列表
     */
    public XmlBodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());

        // 创建XML文本区域
        xmlTextArea = new JTextArea();
        xmlTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        xmlTextArea.setLineWrap(false);
        xmlTextArea.setWrapStyleWord(false);

        // 创建滚动面板
        scrollPane = new JScrollPane(xmlTextArea);
        scrollPane.setPreferredSize(new Dimension(600, 120));

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatButton = new JButton("格式化");
        clearButton = new JButton("清空");

        // 添加按钮事件
        formatButton.addActionListener(e -> formatXml());
        clearButton.addActionListener(e -> clearXml());

        buttonPanel.add(formatButton);
        buttonPanel.add(clearButton);

        // 布局
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 如果有参数，生成示例XML
        if (bodyParamList != null && !bodyParamList.isEmpty()) {
            generateSampleXml(bodyParamList);
        }
    }

    /**
     * 格式化XML内容
     */
    private void formatXml() {
        String xml = xmlTextArea.getText().trim();
        if (xml.isEmpty()) {
            return;
        }

        try {
            // 简单的XML格式化（这里可以集成更复杂的XML解析库）
            String formatted = simpleXmlFormat(xml);
            xmlTextArea.setText(formatted);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "XML格式化失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 简单的XML格式化
     *
     * @param xml 原始XML字符串
     * @return 格式化后的XML字符串
     */
    private String simpleXmlFormat(String xml) {
        // 移除多余的空白字符
        xml = xml.replaceAll(">\\s+<", "><");
        xml = xml.replaceAll("\\s+", " ");

        // 简单的缩进处理
        StringBuilder formatted = new StringBuilder();
        int indent = 0;
        boolean inTag = false;

        for (int i = 0; i < xml.length(); i++) {
            char c = xml.charAt(i);

            if (c == '<') {
                if (i + 1 < xml.length() && xml.charAt(i + 1) == '/') {
                    // 结束标签
                    indent--;
                    formatted.append("\n").append("  ".repeat(Math.max(0, indent)));
                } else {
                    // 开始标签
                    formatted.append("\n").append("  ".repeat(Math.max(0, indent)));
                    indent++;
                }
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            }

            formatted.append(c);
        }

        return formatted.toString().trim();
    }

    /**
     * 清空XML内容
     */
    private void clearXml() {
        xmlTextArea.setText("");
    }

    /**
     * 根据参数生成示例XML
     *
     * @param bodyParamList 请求体参数列表
     */
    private void generateSampleXml(List<ApiParam> bodyParamList) {
        if (bodyParamList.isEmpty()) {
            return;
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<root>\n");

        for (ApiParam param : bodyParamList) {
            String paramName = param.getName() != null ? param.getName() : "param";
            String paramType = param.getDataType() != null ? param.getDataType().name().toLowerCase() : "string";
            String defaultValue = getDefaultValue(paramType);

            xml.append("  <").append(paramName).append(">");
            xml.append(defaultValue);
            xml.append("</").append(paramName).append(">\n");
        }

        xml.append("</root>");
        xmlTextArea.setText(xml.toString());
    }

    /**
     * 根据参数类型获取默认值
     *
     * @param paramType 参数类型
     * @return 默认值
     */
    private String getDefaultValue(String paramType) {
        switch (paramType.toLowerCase()) {
            case "integer":
            case "number":
                return "0";
            case "boolean":
                return "false";
            case "array":
                return "[]";
            case "file":
                return "&lt;file&gt;";
            default:
                return "";
        }
    }

    /**
     * 获取XML内容
     *
     * @return XML字符串
     */
    public String getXmlText() {
        return xmlTextArea.getText();
    }

    /**
     * 设置XML内容
     *
     * @param xml XML字符串
     */
    public void setXmlText(String xml) {
        xmlTextArea.setText(xml != null ? xml : "");
    }

    /**
     * 获取XML文本区域的引用
     *
     * @return JTextArea实例
     */
    public JTextArea getTextAreaRef() {
        return xmlTextArea;
    }
} 