package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * @author leijianhui
 * @Description 二进制请求体面板，支持文件选择和二进制数据输入。
 * @date 2025/06/19 09:36
 */
public class BinaryBodyPanel extends JPanel {
    /**
     * 文件路径显示
     */
    private final JTextField filePathField;
    /**
     * 选择文件按钮
     */
    private final JButton selectFileButton;
    /**
     * 清空按钮
     */
    private final JButton clearButton;
    /**
     * 十六进制文本区域
     */
    private final JTextArea hexTextArea;
    /**
     * 滚动面板
     */
    private final JScrollPane scrollPane;
    /**
     * 当前选择的文件
     */
    private File selectedFile;

    /**
     * 构造方法，初始化二进制编辑面板。
     *
     * @param bodyParamList 请求体参数列表
     */
    public BinaryBodyPanel(List<ApiParam> bodyParamList) {
        super(new BorderLayout());

        // 创建文件选择面板
        JPanel filePanel = new JPanel(new BorderLayout());
        filePathField = new JTextField();
        filePathField.setEditable(false);
        filePathField.setToolTipText("选择的文件路径");

        selectFileButton = new JButton("选择文件");
        clearButton = new JButton("清空");

        // 添加按钮事件
        selectFileButton.addActionListener(e -> selectFile());
        clearButton.addActionListener(e -> clearBinary());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(selectFileButton);
        buttonPanel.add(clearButton);

        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(buttonPanel, BorderLayout.EAST);

        // 创建十六进制文本区域
        hexTextArea = new JTextArea();
        hexTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hexTextArea.setLineWrap(false);
        hexTextArea.setWrapStyleWord(false);
        hexTextArea.setToolTipText("十六进制数据（可选，用于直接输入二进制数据）");

        scrollPane = new JScrollPane(hexTextArea);
        scrollPane.setPreferredSize(new Dimension(600, 120));

        // 创建标签面板
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.add(new JLabel("文件选择:"), BorderLayout.WEST);
        labelPanel.add(new JLabel("十六进制数据:"), BorderLayout.CENTER);

        // 布局
        add(labelPanel, BorderLayout.NORTH);
        add(filePanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // 如果有文件类型参数，显示提示
        if (bodyParamList != null && !bodyParamList.isEmpty()) {
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
                filePathField.setText("检测到文件类型参数，请选择文件");
            }
        }
    }

    /**
     * 选择文件
     */
    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择二进制文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());

            // 可选：显示文件大小
            long fileSize = selectedFile.length();
            String sizeText = formatFileSize(fileSize);
            filePathField.setToolTipText("文件大小: " + sizeText);
        }
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 清空二进制数据
     */
    private void clearBinary() {
        selectedFile = null;
        filePathField.setText("");
        filePathField.setToolTipText("选择的文件路径");
        hexTextArea.setText("");
    }

    /**
     * 获取选择的文件
     *
     * @return 选择的文件，如果没有选择则返回null
     */
    public File getSelectedFile() {
        return selectedFile;
    }

    /**
     * 获取十六进制数据
     *
     * @return 十六进制字符串
     */
    public String getHexData() {
        return hexTextArea.getText().trim();
    }

    /**
     * 设置十六进制数据
     *
     * @param hexData 十六进制字符串
     */
    public void setHexData(String hexData) {
        hexTextArea.setText(hexData != null ? hexData : "");
    }

    /**
     * 检查是否有数据
     *
     * @return 是否有文件或十六进制数据
     */
    public boolean hasData() {
        return selectedFile != null || !getHexData().isEmpty();
    }

    /**
     * 获取二进制数据描述
     *
     * @return 数据描述字符串
     */
    public String getDataDescription() {
        if (selectedFile != null) {
            return "文件: " + selectedFile.getName() + " (" + formatFileSize(selectedFile.length()) + ")";
        } else if (!getHexData().isEmpty()) {
            return "十六进制数据: " + getHexData().length() + " 字符";
        } else {
            return "无数据";
        }
    }

    /**
     * 获取二进制数据
     *
     * @return 二进制数据字节数组
     */
    public byte[] getBinaryData() {
        if (selectedFile != null) {
            try {
                return java.nio.file.Files.readAllBytes(selectedFile.toPath());
            } catch (Exception e) {
                return new byte[0];
            }
        } else if (!getHexData().isEmpty()) {
            return hexStringToByteArray(getHexData());
        }
        return new byte[0];
    }

    /**
     * 设置二进制数据
     *
     * @param binaryData 二进制数据字节数组
     */
    public void setBinaryData(byte[] binaryData) {
        if (binaryData != null && binaryData.length > 0) {
            // 将字节数组转换为十六进制字符串
            String hexString = byteArrayToHexString(binaryData);
            setHexData(hexString);
        } else {
            clearBinary();
        }
    }

    /**
     * 将十六进制字符串转换为字节数组
     *
     * @param hexString 十六进制字符串
     * @return 字节数组
     */
    private byte[] hexStringToByteArray(String hexString) {
        try {
            // 移除空格和换行符
            hexString = hexString.replaceAll("\\s+", "");
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                        + Character.digit(hexString.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString().trim();
    }
} 