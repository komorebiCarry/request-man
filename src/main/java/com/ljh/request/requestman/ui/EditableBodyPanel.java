package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 可编辑请求体面板，支持多类型参数自定义增删改查。
 * @date 2025/06/19 09:36
 */
public class EditableBodyPanel extends JPanel {
    private static final String[] BODY_TYPES = {"none", "form-data", "x-www-form-urlencoded", "json", "xml", "binary"};
    private JComboBox<String> typeBox;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private EditableParamsPanel formDataPanel;
    private EditableParamsPanel urlencodedPanel;
    private JTextArea jsonArea;
    private JTextArea xmlArea;
    private BinaryBodyPanel binaryPanel;

    public EditableBodyPanel(List<ApiParam> bodyParams) {
        super(new BorderLayout());
        // 顶部类型切换
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeBox = new JComboBox<>(BODY_TYPES);
        typeBox.setPreferredSize(new Dimension(160, 28));
        topPanel.add(new JLabel("请求体类型:"));
        topPanel.add(typeBox);
        add(topPanel, BorderLayout.NORTH);
        // 内容区
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        // form-data
        formDataPanel = new EditableParamsPanel(bodyParams);
        // 为formDataPanel的表格添加右键自动选中逻辑
        addRightClickSelectRow(formDataPanel);
        contentPanel.add(formDataPanel, "form-data");
        // x-www-form-urlencoded
        urlencodedPanel = new EditableParamsPanel(bodyParams);
        // 为urlencodedPanel的表格添加右键自动选中逻辑
        addRightClickSelectRow(urlencodedPanel);
        contentPanel.add(urlencodedPanel, "x-www-form-urlencoded");
        // json
        jsonArea = new JTextArea();
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane jsonScroll = new JScrollPane(jsonArea);
        contentPanel.add(jsonScroll, "json");
        // xml
        xmlArea = new JTextArea();
        xmlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane xmlScroll = new JScrollPane(xmlArea);
        contentPanel.add(xmlScroll, "xml");
        // binary
        binaryPanel = new BinaryBodyPanel(new ArrayList<>());
        contentPanel.add(binaryPanel, "binary");
        // none类型空面板（必须最后add，避免被其它类型覆盖）
        JPanel nonePanel = new JPanel();
        contentPanel.add(nonePanel, "none");
        add(contentPanel, BorderLayout.CENTER);
        // 类型切换事件
        typeBox.addActionListener(e -> {
            String type = (String) typeBox.getSelectedItem();
            cardLayout.show(contentPanel, type);
        });
        // 默认显示none
        cardLayout.show(contentPanel, "none");
    }

    /**
     * 获取当前请求体类型
     */
    public String getBodyType() {
        return (String) typeBox.getSelectedItem();
    }

    /**
     * 获取form-data参数
     */
    public List<ApiParam> getFormDataParams() {
        return formDataPanel.getParams();
    }

    /**
     * 获取x-www-form-urlencoded参数
     */
    public List<ApiParam> getUrlencodedParams() {
        return urlencodedPanel.getParams();
    }

    /**
     * 获取json内容
     */
    public String getJsonBody() {
        return jsonArea.getText();
    }

    /**
     * 获取xml内容
     */
    public String getXmlBody() {
        return xmlArea.getText();
    }

    /**
     * 获取binary内容
     */
    public byte[] getBinaryBody() {
        return binaryPanel.getBinaryData();
    }

    /**
     * 设置json内容
     */
    public void setJsonBody(String json) {
        jsonArea.setText(json != null ? json : "");
        typeBox.setSelectedItem("json");
        cardLayout.show(contentPanel, "json");
    }

    /**
     * 设置form-data参数并切换显示
     */
    public void setFormDataParams(List<ApiParam> params) {
        contentPanel.remove(formDataPanel);
        formDataPanel = new EditableParamsPanel(params);
        contentPanel.add(formDataPanel, "form-data");
        cardLayout.show(contentPanel, "form-data");
        typeBox.setSelectedItem("form-data");
    }

    /**
     * 设置x-www-form-urlencoded参数并切换显示
     */
    public void setUrlencodedParams(List<ApiParam> params) {
        contentPanel.remove(urlencodedPanel);
        urlencodedPanel = new EditableParamsPanel(params);
        contentPanel.add(urlencodedPanel, "x-www-form-urlencoded");
        cardLayout.show(contentPanel, "x-www-form-urlencoded");
        typeBox.setSelectedItem("x-www-form-urlencoded");
    }

    /**
     * 设置xml内容并切换到xml类型
     */
    public void setXmlBody(String xml) {
        xmlArea.setText(xml != null ? xml : "");
        typeBox.setSelectedItem("xml");
        cardLayout.show(contentPanel, "xml");
    }

    /**
     * 设置binary内容并切换到binary类型
     */
    public void setBinaryBody(byte[] binaryData) {
        binaryPanel.setBinaryData(binaryData);
        typeBox.setSelectedItem("binary");
        cardLayout.show(contentPanel, "binary");
    }

    /**
     * 设置当前请求体类型
     */
    public void setBodyType(String type) {
        if (type == null) {
            type = "none";
        }
        typeBox.setSelectedItem(type);
        cardLayout.show(contentPanel, type);
    }

    /**
     * 获取form-data参数面板
     */
    public EditableParamsPanel getFormDataPanel() {
        return formDataPanel;
    }

    /**
     * 获取x-www-form-urlencoded参数面板
     */
    public EditableParamsPanel getUrlencodedPanel() {
        return urlencodedPanel;
    }

    /**
     * 给EditableParamsPanel的表格添加右键点击自动选中行的监听器
     */
    private void addRightClickSelectRow(EditableParamsPanel panel) {
        try {
            java.lang.reflect.Field tableField = panel.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object tableObj = tableField.get(panel);
            if (tableObj instanceof JTable table) {
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
            }
        } catch (Exception ignored) {
        }
    }
} 