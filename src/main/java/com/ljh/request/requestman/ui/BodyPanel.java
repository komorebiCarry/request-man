package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * @author leijianhui
 * @Description BodyPanel 请求体参数面板，顶部为请求体类型切换按钮，下方为内容区。
 * @date 2025/06/19 09:36
 */
public class BodyPanel extends JPanel {
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
     * form-data类型面板
     */
    private FormDataBodyPanel formDataPanel;
    /**
     * x-www-form-urlencoded类型面板
     */
    private XWwwFormUrlencodedBodyPanel urlencodedPanel;

    /**
     * 构造方法，初始化Body类型切换和内容区。
     *
     * @param bodyParamList 请求体参数列表
     */
    public BodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());
        // 顶部类型切换下拉框
        typeComboBox = new JComboBox<>(BODY_TYPES);
        typeComboBox.setMaximumSize(new Dimension(150, 28));
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.X_AXIS));
        typePanel.add(new JLabel("Body类型: "));
        typePanel.add(typeComboBox);
        typePanel.add(Box.createHorizontalGlue());
        add(typePanel, BorderLayout.NORTH);
        // 构建内容区
        buildContentPanel(bodyParamList);
        add(contentPanel, BorderLayout.CENTER);
        setPreferredSize(PARAM_PANEL_SIZE);
        // 切换内容区监听器
        typeComboBox.addActionListener(e -> {
            if (initializing) {
                return;
            }
            String selected = (String) typeComboBox.getSelectedItem();
            cardLayout.show(contentPanel, selected);
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
     */
    private void buildContentPanel(List<ApiParam> bodyParamList) {
        // none类型
        JPanel nonePanel = new JPanel(new BorderLayout());
        JLabel noneLabel = new JLabel("该请求没有Body", JLabel.CENTER);
        nonePanel.add(noneLabel, BorderLayout.CENTER);
        contentPanel.add(nonePanel, "none");

        // form-data类型
        formDataPanel = new FormDataBodyPanel(bodyParamList);
        contentPanel.add(formDataPanel, "form-data");

        // x-www-form-urlencoded类型
        urlencodedPanel = new XWwwFormUrlencodedBodyPanel(bodyParamList);
        contentPanel.add(urlencodedPanel, "x-www-form-urlencoded");

        // json类型
        JsonBodyPanel jsonPanel = new JsonBodyPanel(bodyParamList);
        contentPanel.add(jsonPanel, "json");

        // xml类型
        XmlBodyPanel xmlPanel = new XmlBodyPanel(bodyParamList);
        contentPanel.add(xmlPanel, "xml");

        // binary类型
        BinaryBodyPanel binaryPanel = new BinaryBodyPanel(bodyParamList);
        contentPanel.add(binaryPanel, "binary");
    }

    /**
     * 根据body参数类型自动推断默认Body类型。
     * 优先考虑Spring注解的consumes属性，其次根据参数类型特征推断。
     *
     * @param bodyParamList 请求体参数列表
     * @return 默认Body类型
     */
    private String guessDefaultBodyType(List<ApiParam> bodyParamList) {
        if (bodyParamList == null || bodyParamList.isEmpty()) {
            return "none";
        }

        // 1. 优先检查是否有明确的Content-Type注解信息
        for (ApiParam param : bodyParamList) {
            String contentType = param.getContentType();
            if (contentType != null && !contentType.trim().isEmpty()) {
                return mapContentTypeToBodyType(contentType);
            }
        }

        // 2. 检查是否有文件类型参数
        boolean hasFile = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            return dataTypeName.equals("file") ||
                    rawType.contains("multipartfile") ||
                    rawType.contains("file") ||
                    rawType.contains("inputstream") ||
                    rawType.contains("byte[]");
        });

        if (hasFile) {
            return "form-data";
        }

        // 3. 检查是否有二进制数据
        boolean hasBinary = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            return dataTypeName.equals("binary") ||
                    rawType.contains("byte[]") ||
                    rawType.contains("bytebuffer") ||
                    rawType.contains("inputstream") ||
                    rawType.contains("outputstream") ||
                    rawType.contains("bufferedimage");
            // 注意：移除了 rawType.contains("multipartfile")，因为MultipartFile应该优先识别为文件类型
        });

        if (hasBinary) {
            return "binary";
        }

        // 4. 检查是否有XML相关类型
        boolean hasXml = bodyParamList.stream().anyMatch(p -> {
            if (p.getDataType() == null) {
                return false;
            }
            String dataTypeName = p.getDataType().name().toLowerCase();
            String rawType = p.getRawType() != null ? p.getRawType().toLowerCase() : "";
            String description = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
            return dataTypeName.equals("xml") ||
                    rawType.contains("document") ||
                    rawType.contains("element") ||
                    rawType.contains("node") ||
                    description.contains("xml") ||
                    description.contains("soap");
        });

        if (hasXml) {
            return "xml";
        }

        // 7. 分析参数类型特征
        boolean hasComplexObject = false;
        boolean hasBasicType = false;
        boolean hasArray = false;
        boolean hasString = false;

        for (ApiParam param : bodyParamList) {
            if (param.getDataType() == null) {
                continue;
            }

            String dataTypeName = param.getDataType().name().toLowerCase();
            String rawType = param.getRawType() != null ? param.getRawType().toLowerCase() : "";

            // 检查是否是字符串类型
            if (dataTypeName.equals("string") || rawType.equals("java.lang.string")) {
                hasString = true;
            }
            // 检查是否是基本类型
            else if (isBasicType(dataTypeName, rawType)) {
                hasBasicType = true;
            }
            // 检查是否是数组
            else if (dataTypeName.equals("array") || rawType.contains("[]") ||
                    rawType.contains("list") || rawType.contains("set")) {
                hasArray = true;
            }
            // 检查是否是复杂对象
            else if (isComplexObject(dataTypeName, rawType)) {
                hasComplexObject = true;
            }
        }

        // 8. 根据参数特征推断Body类型
        if (hasComplexObject) {
            // 复杂对象通常使用JSON
            return "json";
        } else if (hasArray && !hasBasicType) {
            // 对象数组通常使用JSON
            return "json";
        } else if (hasBasicType && !hasComplexObject && !hasArray) {
            // 只有基本类型参数，可以使用form-urlencoded
            return "x-www-form-urlencoded";
        } else if (hasBasicType && hasArray) {
            // 基本类型+数组，使用JSON更合适
            return "json";
        } else if (hasString && bodyParamList.size() == 1) {
            // 单个字符串参数，可能是JSON字符串
            return "json";
        }

        // 9. 默认使用JSON（最通用的选择）
        return "json";
    }

    /**
     * 将Content-Type映射到Body类型
     *
     * @param contentType Content-Type字符串
     * @return 对应的Body类型
     */
    private String mapContentTypeToBodyType(String contentType) {
        if (contentType == null) {
            return "json";
        }

        String lowerContentType = contentType.toLowerCase();

        // JSON相关
        if (lowerContentType.contains("application/json") ||
                lowerContentType.contains("text/json")) {
            return "json";
        }

        // XML相关
        if (lowerContentType.contains("application/xml") ||
                lowerContentType.contains("text/xml") ||
                lowerContentType.contains("application/soap+xml")) {
            return "xml";
        }

        // 表单相关
        if (lowerContentType.contains("application/x-www-form-urlencoded")) {
            return "x-www-form-urlencoded";
        }
        if (lowerContentType.contains("multipart/form-data")) {
            return "form-data";
        }

        // 文本相关 - 根据内容类型分配到合适的类型
        if (lowerContentType.contains("text/plain")) {
            return "json"; // 纯文本通常作为JSON字符串处理
        }
        if (lowerContentType.contains("text/html")) {
            return "xml"; // HTML作为XML处理
        }
        if (lowerContentType.contains("text/css")) {
            return "json"; // CSS作为JSON字符串处理
        }
        if (lowerContentType.contains("text/javascript")) {
            return "json"; // JavaScript作为JSON字符串处理
        }

        // 二进制相关
        if (lowerContentType.contains("application/octet-stream") ||
                lowerContentType.contains("application/pdf") ||
                lowerContentType.contains("image/") ||
                lowerContentType.contains("audio/") ||
                lowerContentType.contains("video/") ||
                lowerContentType.contains("application/zip") ||
                lowerContentType.contains("application/rar") ||
                lowerContentType.contains("application/excel") ||
                lowerContentType.contains("application/msword") ||
                lowerContentType.contains("application/protobuf")) {
            return "binary";
        }

        // 其他特殊类型
        if (lowerContentType.contains("application/yaml") ||
                lowerContentType.contains("text/yaml")) {
            return "json"; // YAML作为JSON处理
        }

        if (lowerContentType.contains("application/protobuf") ||
                lowerContentType.contains("application/x-protobuf")) {
            return "binary"; // Protocol Buffers作为二进制处理
        }

        // 默认返回JSON（最通用的选择）
        return "json";
    }

    /**
     * 判断是否是基本类型
     *
     * @param dataTypeName 数据类型名称
     * @param rawType      原始类型
     * @return 是否是基本类型
     */
    private boolean isBasicType(String dataTypeName, String rawType) {
        return dataTypeName.equals("string") ||
                dataTypeName.equals("integer") ||
                dataTypeName.equals("number") ||
                dataTypeName.equals("boolean") ||
                rawType.equals("java.lang.string") ||
                rawType.equals("java.lang.integer") ||
                rawType.equals("java.lang.long") ||
                rawType.equals("java.lang.double") ||
                rawType.equals("java.lang.float") ||
                rawType.equals("java.lang.boolean") ||
                rawType.equals("int") ||
                rawType.equals("long") ||
                rawType.equals("double") ||
                rawType.equals("float") ||
                rawType.equals("boolean");
    }

    /**
     * 判断是否是复杂对象
     *
     * @param dataTypeName 数据类型名称
     * @param rawType      原始类型
     * @return 是否是复杂对象
     */
    private boolean isComplexObject(String dataTypeName, String rawType) {
        // 排除基本类型和数组
        if (isBasicType(dataTypeName, rawType) ||
                dataTypeName.equals("array") ||
                rawType.contains("[]") ||
                rawType.contains("list") ||
                rawType.contains("set")) {
            return false;
        }

        // 检查是否是Java内置类型
        if (rawType.startsWith("java.lang.") ||
                rawType.startsWith("java.util.") ||
                rawType.startsWith("java.time.") ||
                rawType.startsWith("java.math.")) {
            return false;
        }

        // 其他都认为是复杂对象
        return true;
    }

    /**
     * 获取 json 类型 body 的文本内容（仅支持 json 类型）
     *
     * @return json 文本
     */
    public String getJsonBodyText() {
        Component comp = contentPanel.getComponent(3); // "json" 类型在 buildContentPanel 顺序第4个
        if (comp instanceof JsonBodyPanel jsonPanel) {
            return jsonPanel.getJsonText();
        }
        return "";
    }

    /**
     * 设置 json 类型 body 的文本内容（仅支持 json 类型）
     *
     * @param text json 文本
     */
    public void setJsonBodyText(String text) {
        Component comp = contentPanel.getComponent(3); // "json" 类型在 buildContentPanel 顺序第4个
        if (comp instanceof JsonBodyPanel jsonPanel) {
            jsonPanel.setJsonText(text);
        }
    }

    /**
     * 获取当前Body类型
     */
    public String getBodyType() {
        return typeComboBox.getSelectedItem().toString();
    }

    /**
     * 获取当前Body参数（form-data或urlencoded类型返回参数列表，其它类型返回空列表）
     */
    public List<ApiParam> getBodyParams() {
        String type = getBodyType();
        if ("form-data".equals(type) && formDataPanel != null) {
            return formDataPanel.getParams();
        } else if ("x-www-form-urlencoded".equals(type) && urlencodedPanel != null) {
            return urlencodedPanel.getParams();
        }
        return new ArrayList<>();
    }

    /**
     * 获取当前Body内容（根据类型返回相应的内容）
     *
     * @return Body内容字符串
     */
    public String getBodyContent() {
        String type = getBodyType();
        switch (type) {
            case "json":
                return getJsonBodyText();
            case "xml":
                return getXmlBodyText();
            case "binary":
                return getBinaryBodyContent();
            default:
                return "";
        }
    }

    /**
     * 获取 xml 类型 body 的文本内容
     *
     * @return xml 文本
     */
    public String getXmlBodyText() {
        Component comp = contentPanel.getComponent(4); // "xml" 类型在 buildContentPanel 顺序第5个
        if (comp instanceof XmlBodyPanel xmlPanel) {
            return xmlPanel.getXmlText();
        }
        return "";
    }

    /**
     * 设置 xml 类型 body 的文本内容
     *
     * @param text xml 文本
     */
    public void setXmlBodyText(String text) {
        Component comp = contentPanel.getComponent(4); // "xml" 类型在 buildContentPanel 顺序第5个
        if (comp instanceof XmlBodyPanel xmlPanel) {
            xmlPanel.setXmlText(text);
        }
    }

    /**
     * 获取 binary 类型 body 的内容
     *
     * @return binary 内容字符串（文件路径或十六进制数据）
     */
    public String getBinaryBodyContent() {
        Component comp = contentPanel.getComponent(5); // "binary" 类型在 buildContentPanel 顺序第6个
        if (comp instanceof BinaryBodyPanel binaryPanel) {
            if (binaryPanel.getSelectedFile() != null) {
                return binaryPanel.getSelectedFile().getAbsolutePath();
            } else {
                return binaryPanel.getHexData();
            }
        }
        return "";
    }

    /**
     * 获取二进制数据
     *
     * @return 二进制数据字节数组
     */
    public byte[] getBinaryData() {
        Component comp = contentPanel.getComponent(5); // "binary" 类型在 buildContentPanel 顺序第6个
        if (comp instanceof BinaryBodyPanel binaryPanel) {
            return binaryPanel.getBinaryData();
        }
        return new byte[0];
    }
} 