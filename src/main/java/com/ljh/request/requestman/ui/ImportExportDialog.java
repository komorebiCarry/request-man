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
import com.ljh.request.requestman.util.RequestManBundle;

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

        setTitle(isImport ? RequestManBundle.message("impexp.title.import") : RequestManBundle.message("impexp.title.export"));
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

        JButton browseButton = new JButton(RequestManBundle.message("impexp.browse"));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile();
            }
        });

        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(new JBLabel(isImport ? RequestManBundle.message("impexp.select.import") : RequestManBundle.message("impexp.select.export")), BorderLayout.NORTH);

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
        overwriteRadioButton = new JBRadioButton(RequestManBundle.message("impexp.import.overwrite"));
        appendRadioButton = new JBRadioButton(RequestManBundle.message("impexp.import.append"));
        appendRadioButton.setSelected(true); // 默认选择追加模式

        importModeGroup = new ButtonGroup();
        importModeGroup.add(overwriteRadioButton);
        importModeGroup.add(appendRadioButton);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.add(new JBLabel(RequestManBundle.message("impexp.import.mode")));
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
            fileChooser.setDialogTitle(RequestManBundle.message("impexp.dialog.import.title"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
                }

                @Override
                public String getDescription() {
                    return RequestManBundle.message("impexp.dialog.filefilter.json");
                }
            });
        } else {
            fileChooser.setDialogTitle(RequestManBundle.message("impexp.dialog.export.title"));
            fileChooser.setSelectedFile(new File("RequestMan_APIs.json"));
        }

        int result = fileChooser.showDialog(this.getContentPane(), isImport ? RequestManBundle.message("impexp.btn.import") : RequestManBundle.message("impexp.btn.export"));
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @Override
    protected void doOKAction() {
        String filePath = filePathField.getText().trim();
        if (StrUtil.isBlank(filePath)) {
            Messages.showErrorDialog(this.getContentPane(), RequestManBundle.message("impexp.error.noPath"), RequestManBundle.message("common.error"));
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
            Messages.showErrorDialog(this.getContentPane(), RequestManBundle.message("impexp.error.operation", e.getMessage()), RequestManBundle.message("common.error"));
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
            Messages.showWarningDialog(this.getContentPane(), RequestManBundle.message("impexp.import.empty"), RequestManBundle.message("impexp.import.fail"));
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
                    RequestManBundle.message("impexp.import.success.overwrite", importedApis.size()),
                    RequestManBundle.message("impexp.import.success"));
        } else {
            // 追加模式：添加导入的接口到现有列表
            int originalSize = customApiListModel.getSize();
            for (CustomApiInfo api : importedApis) {
                customApiListModel.addElement(api);
            }
            Messages.showInfoMessage(this.getContentPane(),
                    RequestManBundle.message("impexp.import.success.append", importedApis.size(), customApiListModel.getSize()),
                    RequestManBundle.message("impexp.import.success"));
        }
    }

    /**
     * 执行导出操作
     */
    private void performExport(String filePath) throws Exception {
        if (customApiList.isEmpty()) {
            Messages.showWarningDialog(this.getContentPane(), RequestManBundle.message("impexp.export.empty"), RequestManBundle.message("common.warn"));
            return;
        }

        // 直接导出CustomApiInfo列表为JSON
        String jsonContent = JSONUtil.toJsonStr(customApiList);

        // 写入文件
        FileUtil.writeString(jsonContent, filePath, StandardCharsets.UTF_8);

        Messages.showInfoMessage(this.getContentPane(),
                RequestManBundle.message("impexp.export.success", customApiList.size(), filePath), RequestManBundle.message("impexp.export.done"));
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