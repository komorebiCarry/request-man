package com.ljh.request.requestman.ui;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.ljh.request.requestman.model.CustomApiInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 导入导出对话框，支持选择覆盖或追加模式
 *
 * @author leijianhui
 * @Description 导入导出对话框，支持选择覆盖或追加模式
 * @date 2025/01/27 10:00
 */
public class ImportExportDialog extends DialogWrapper {

    private final Project project;
    private final boolean isImport;
    private final List<CustomApiInfo> customApiList;
    private final DefaultListModel<CustomApiInfo> customApiListModel;

    private JBTextField filePathField;
    private JBRadioButton overwriteRadioButton;
    private JBRadioButton appendRadioButton;
    private ButtonGroup importModeGroup;

    /**
     * 构造函数
     *
     * @param project            项目对象
     * @param isImport           是否为导入模式
     * @param customApiList      自定义接口列表（导出时使用）
     * @param customApiListModel 自定义接口列表模型（导入时使用）
     */
    public ImportExportDialog(Project project, boolean isImport, List<CustomApiInfo> customApiList, DefaultListModel<CustomApiInfo> customApiListModel) {
        super(project);
        this.project = project;
        this.isImport = isImport;
        this.customApiList = customApiList;
        this.customApiListModel = customApiListModel;

        setTitle(isImport ? "导入接口集合" : "导出接口集合");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        // 文件选择区域
        JPanel filePanel = createFilePanel();
        dialogPanel.add(filePanel, BorderLayout.NORTH);

        // 配置区域
        JPanel configPanel = createConfigPanel();
        dialogPanel.add(configPanel, BorderLayout.CENTER);

        // 设置对话框大小
        dialogPanel.setPreferredSize(new Dimension(500, 300));

        return dialogPanel;
    }

    /**
     * 创建文件选择面板
     */
    private JPanel createFilePanel() {
        filePathField = new JBTextField();
        filePathField.setEditable(false);

        JButton browseButton = new JButton("浏览");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile();
            }
        });

        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(new JBLabel(isImport ? "选择要导入的JSON文件:" : "选择导出文件保存位置:"), BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(filePathField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);
        filePanel.add(inputPanel, BorderLayout.CENTER);

        return filePanel;
    }

    /**
     * 创建配置面板
     */
    private JPanel createConfigPanel() {
        if (isImport) {
            return createImportConfigPanel();
        } else {
            return createExportConfigPanel();
        }
    }

    /**
     * 创建导入配置面板
     */
    private JPanel createImportConfigPanel() {
        // 导入模式选择
        overwriteRadioButton = new JBRadioButton("覆盖现有接口");
        appendRadioButton = new JBRadioButton("追加到现有接口");
        appendRadioButton.setSelected(true); // 默认选择追加模式

        importModeGroup = new ButtonGroup();
        importModeGroup.add(overwriteRadioButton);
        importModeGroup.add(appendRadioButton);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.add(new JBLabel("导入模式:"));
        modePanel.add(overwriteRadioButton);
        modePanel.add(appendRadioButton);

        return modePanel;
    }

    /**
     * 创建导出配置面板
     */
    private JPanel createExportConfigPanel() {
        // 导出时不需要额外配置，返回空面板
        return new JPanel();
    }

    /**
     * 浏览文件
     */
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();

        if (isImport) {
            fileChooser.setDialogTitle("选择要导入的JSON文件");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
                }

                @Override
                public String getDescription() {
                    return "JSON文件 (*.json)";
                }
            });
        } else {
            fileChooser.setDialogTitle("选择导出文件保存位置");
            fileChooser.setSelectedFile(new File("RequestMan_APIs.json"));
        }

        int result = fileChooser.showDialog(this.getContentPane(), isImport ? "导入" : "导出");
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @Override
    protected void doOKAction() {
        String filePath = filePathField.getText().trim();
        if (StrUtil.isBlank(filePath)) {
            Messages.showErrorDialog(this.getContentPane(), "请选择文件路径", "错误");
            return;
        }

        try {
            if (isImport) {
                performImport(filePath);
            } else {
                performExport(filePath);
            }
            super.doOKAction();
        } catch (Exception e) {
            Messages.showErrorDialog(this.getContentPane(), "操作失败: " + e.getMessage(), "错误");
        }
    }

    /**
     * 执行导入操作
     */
    private void performImport(String filePath) throws Exception {
        // 读取JSON文件
        String jsonContent = FileUtil.readString(filePath, StandardCharsets.UTF_8);

        // 解析为CustomApiInfo列表
        List<CustomApiInfo> importedApis = JSONUtil.toList(jsonContent, CustomApiInfo.class);

        if (importedApis.isEmpty()) {
            Messages.showWarningDialog(this.getContentPane(), "导入的JSON文件中没有找到有效的接口数据", "导入失败");
            return;
        }

        // 根据模式处理导入
        if (overwriteRadioButton.isSelected()) {
            // 覆盖模式：清空现有列表，添加导入的接口
            customApiListModel.clear();
            for (CustomApiInfo api : importedApis) {
                customApiListModel.addElement(api);
            }
            Messages.showInfoMessage(this.getContentPane(),
                    String.format("成功导入 %d 个接口，覆盖了原有接口", importedApis.size()),
                    "导入成功");
        } else {
            // 追加模式：添加导入的接口到现有列表
            int originalSize = customApiListModel.getSize();
            for (CustomApiInfo api : importedApis) {
                customApiListModel.addElement(api);
            }
            Messages.showInfoMessage(this.getContentPane(),
                    String.format("成功导入 %d 个接口，现有接口总数: %d",
                            importedApis.size(), customApiListModel.getSize()),
                    "导入成功");
        }
    }

    /**
     * 执行导出操作
     */
    private void performExport(String filePath) throws Exception {
        if (customApiList.isEmpty()) {
            Messages.showWarningDialog(this.getContentPane(), "没有接口可以导出", "警告");
            return;
        }

        // 直接导出CustomApiInfo列表为JSON
        String jsonContent = JSONUtil.toJsonStr(customApiList);

        // 写入文件
        FileUtil.writeString(jsonContent, filePath, StandardCharsets.UTF_8);

        Messages.showInfoMessage(this.getContentPane(),
                String.format("成功导出 %d 个接口到文件: %s", customApiList.size(), filePath), "导出成功");
    }

    /**
     * 获取导入模式
     *
     * @return true为覆盖模式，false为追加模式
     */
    public boolean isOverwriteMode() {
        return overwriteRadioButton.isSelected();
    }
} 