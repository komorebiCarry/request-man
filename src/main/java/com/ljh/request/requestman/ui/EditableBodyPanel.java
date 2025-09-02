package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.ui.ParamsTablePanel;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import com.ljh.request.requestman.util.JsonExampleGenerator;
import org.apache.commons.lang3.StringUtils;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description 可编辑请求体面板，支持多类型参数自定义增删改查，布局与BodyPanel保持一致。
 * @date 2025/06/19 09:36
 */
public class EditableBodyPanel extends JPanel {
    /**
     * 参数面板统一尺寸
     */
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);
    /**
     * 支持的Body类型
     */
    private static final String[] BODY_TYPES = {"none", "form-data", "x-www-form-urlencoded", "json", "xml", "binary"};
    /**
     * 内容区布局
     */
    private final CardLayout cardLayout = new CardLayout();
    /**
     * 内容区面板
     */
    private final JPanel contentPanel = new JPanel(cardLayout);
    /**
     * 初始化标志位，屏蔽初始化阶段监听器
     */
    private boolean initializing = true;
    /**
     * 类型下拉框
     */
    private JComboBox<String> typeComboBox;
    /**
     * form-data类型面板（可编辑）
     */
    private ParamsTablePanel formDataPanel;
    /**
     * x-www-form-urlencoded类型面板（可编辑）
     */
    private ParamsTablePanel urlencodedPanel;
    /**
     * JSON类型面板
     */
    private JTextArea jsonArea;
    /**
     * XML类型面板
     */
    private JTextArea xmlArea;
    /**
     * 二进制类型面板
     */
    private BinaryBodyPanel binaryPanel;

    public EditableBodyPanel(List<ApiParam> bodyParamList) {
        this(bodyParamList, false);
    }

    /**
     * 构造方法，初始化Body类型切换和内容区。
     *
     * @param bodyParamList 请求体参数列表
     * @param isCustom      是否为自定义模式
     */
    public EditableBodyPanel(List<ApiParam> bodyParamList, boolean isCustom) {
        super(new BorderLayout());

        // 顶部类型切换下拉框
        typeComboBox = new JComboBox<>(BODY_TYPES);
        typeComboBox.setMaximumSize(new Dimension(140, 28));
        JPanel topPanel = new JPanel(new BorderLayout());
        // 左侧：Body 类型选择
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftPanel.add(new JLabel(RequestManBundle.message("body.label") + ": "));
        leftPanel.add(typeComboBox);
        // 右侧：功能按钮（初始为空，根据类型动态添加）
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        // 将功能按钮添加到父面板的右侧面板
        if (rightPanel != null) {
            JButton formatBtn = new JButton(RequestManBundle.message("json.format"));
            rightPanel.add(formatBtn);
                // 格式化功能
                formatBtn.addActionListener((ActionEvent e) -> {
                    String text = jsonArea.getText();
                    try {
                        String pretty = ApiInfoExtractor.formatJson(text);
                        jsonArea.setText(pretty);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, RequestManBundle.message("json.format.error"), RequestManBundle.message("common.error"), JOptionPane.ERROR_MESSAGE);
                    }
                });
        }

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        // 构建内容区
        buildContentPanel(bodyParamList, isCustom);
        add(contentPanel, BorderLayout.CENTER);
        setPreferredSize(PARAM_PANEL_SIZE);

        // 切换内容区监听器
        typeComboBox.addActionListener(e -> {
            if (initializing) {
                return;
            }
            String selected = (String) typeComboBox.getSelectedItem();
            cardLayout.show(contentPanel, selected);
            // 只有 JSON 类型显示功能按钮，其他类型隐藏
            if (rightPanel != null) {
                rightPanel.setVisible("json".equals(selected));
            }
        });

        // 根据body参数类型自动推断默认Body类型
        String defaultType = guessDefaultBodyType(bodyParamList);
        typeComboBox.setSelectedItem(defaultType);
        cardLayout.show(contentPanel, defaultType);
        initializing = false;
    }

    /**
     * 构建内容区，按类型添加不同面板。
     *
     * @param bodyParamList 请求体参数列表
     * @param isCustom      是否为自定义模式
     */
    private void buildContentPanel(List<ApiParam> bodyParamList, boolean isCustom) {
        // none类型
        JPanel nonePanel = new JPanel(new BorderLayout());
        nonePanel.add(new JLabel("无请求体", SwingConstants.CENTER), BorderLayout.CENTER);
        contentPanel.add(nonePanel, "none");

        // form-data类型
        formDataPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.BODY_FORM_DATA, bodyParamList);
        contentPanel.add(formDataPanel, "form-data");

        // x-www-form-urlencoded类型
        urlencodedPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.BODY_URLENCODED, bodyParamList);
        contentPanel.add(urlencodedPanel, "x-www-form-urlencoded");

        // JSON类型
        jsonArea = new JTextArea();
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane jsonScroll = new JScrollPane(jsonArea);
        contentPanel.add(jsonScroll, "json");

        // XML类型
        xmlArea = new JTextArea();
        xmlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane xmlScroll = new JScrollPane(xmlArea);
        contentPanel.add(xmlScroll, "xml");

        // 二进制类型
        binaryPanel = new BinaryBodyPanel(new ArrayList<>());
        contentPanel.add(binaryPanel, "binary");
    }

    /**
     * 根据body参数类型自动推断默认Body类型
     */
    private String guessDefaultBodyType(List<ApiParam> bodyParamList) {
        if (bodyParamList == null || bodyParamList.isEmpty()) {
            return "none";
        }

        // 检查是否有文件类型参数
        for (ApiParam param : bodyParamList) {
            if (param.getDataType() != null && "FILE".equals(param.getDataType().name())) {
                return "form-data";
            }
        }

        // 检查是否有复杂对象参数
        for (ApiParam param : bodyParamList) {
            if (param.getChildren() != null && !param.getChildren().isEmpty()) {
                return "json";
            }
        }

        // 默认使用form-data
        return "form-data";
    }

    /**
     * 获取当前请求体类型
     */
    public String getBodyType() {
        return (String) typeComboBox.getSelectedItem();
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
     * 设置json内容
     */
    public void setJsonBody(String json) {
        if (jsonArea != null) {
            jsonArea.setText(json);
        }
    }

    /**
     * 获取xml内容
     */
    public String getXmlBody() {
        return xmlArea.getText();
    }

    /**
     * 设置xml内容
     */
    public void setXmlBody(String xml) {
        if (xmlArea != null) {
            xmlArea.setText(xml);
        }
    }

//    /**
//     * 获取二进制数据
//     */
//    public byte[] getBinaryData() {
//        return binaryPanel != null ? binaryPanel.getBinaryData() : new byte[0];
//    }

//    /**
//     * 设置二进制数据
//     */
//    public void setBinaryData(byte[] binaryData) {
//        if (binaryPanel != null) {
//            binaryPanel.setBinaryData(binaryData);
//        }
//    }

//    /**
//     * 获取十六进制数据
//     */
//    public String getHexData() {
//        return binaryPanel != null ? binaryPanel.getHexData() : "";
//    }

//    /**
//     * 设置十六进制数据
//     */
//    public void setHexData(String hexData) {
//        if (binaryPanel != null) {
//            binaryPanel.setHexData(hexData);
//        }
//    }

    /**
     * 设置form-data参数
     */
    public void setFormDataParams(List<ApiParam> params) {
        if (formDataPanel != null) {
            formDataPanel.setParams(params);
        }
    }

    /**
     * 设置x-www-form-urlencoded参数
     */
    public void setUrlencodedParams(List<ApiParam> params) {
        if (urlencodedPanel != null) {
            urlencodedPanel.setParams(params);
        }
    }

    /**
     * 刷新参数显示
     */
    public void refreshParams(List<ApiParam> bodyParamList) {
        if (formDataPanel != null) {
            formDataPanel.setParams(bodyParamList);
        }
        if (urlencodedPanel != null) {
            urlencodedPanel.setParams(bodyParamList);
        }
    }

    // --- 兼容性方法，保持与BodyPanel的API一致 ---

    /**
     * 获取类型下拉框（兼容性方法）
     */
    public JComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    /**
     * 获取JSON面板（兼容性方法）
     */
    public JsonBodyPanel getJsonBodyPanel() {
        // 创建一个临时的JsonBodyPanel来保持兼容性
        JsonBodyPanel jsonPanel = new JsonBodyPanel(new ArrayList<>(), true, null);
        // 由于无法直接设置textAreaRef，这里创建一个包装器
        return jsonPanel;
    }

    /**
     * 获取XML面板（兼容性方法）
     */
    public XmlBodyPanel getXmlBodyPanel() {
        // 创建一个临时的XmlBodyPanel来保持兼容性
        XmlBodyPanel xmlPanel = new XmlBodyPanel(new ArrayList<>());
        // 由于无法直接设置textAreaRef，这里创建一个包装器
        return xmlPanel;
    }

    /**
     * 获取JSON文本区域引用（用于自动保存）
     */
    public JTextArea getJsonTextArea() {
        return jsonArea;
    }

    /**
     * 获取XML文本区域引用（用于自动保存）
     */
    public JTextArea getXmlTextArea() {
        return xmlArea;
    }

    /**
     * 获取FormData面板（兼容性方法）
     */
    public ParamsTablePanel getFormDataPanel() {
        return formDataPanel;
    }

    /**
     * 获取Urlencoded面板（兼容性方法）
     */
    public ParamsTablePanel getUrlencodedPanel() {
        return urlencodedPanel;
    }

    /**
     * 获取Body参数（兼容性方法）
     */
    public List<ApiParam> getBodyParams() {
        String bodyType = getBodyType();
        switch (bodyType) {
            case "form-data":
                return getFormDataParams();
            case "x-www-form-urlencoded":
                return getUrlencodedParams();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取JSON文本（兼容性方法）
     */
    public String getJsonBodyText() {
        return getJsonBody();
    }

    /**
     * 获取XML文本（兼容性方法）
     */
    public String getXmlBodyText() {
        return getXmlBody();
    }

    /**
     * 设置Body类型（兼容性方法）
     */
    public void setBodyType(String bodyType) {
        if (typeComboBox != null && bodyType != null) {
            typeComboBox.setSelectedItem(bodyType);
            cardLayout.show(contentPanel, bodyType);
        }
    }

//    /**
//     * 设置二进制数据（兼容性方法）
//     */
//    public void setBinaryBody(byte[] binaryData) {
//        setBinaryData(binaryData);
//    }

    /**
     * 获取binary文件目录
     *
     * @return 二进制数据字节数组
     */
    public String getFilePathFromBinaryText() {
        if (binaryPanel != null) {
            return binaryPanel.getFilePathFromBinaryText();
        }

        return "";
    }

    /**
     * 设置binary文件目录
     *
     * @return 二进制数据字节数组
     */
    public void setFilePathFromBinaryText(String binaryText) {
        if (binaryPanel != null) {
            binaryPanel.setFilePathFromBinaryText(binaryText);
        }
    }
}
