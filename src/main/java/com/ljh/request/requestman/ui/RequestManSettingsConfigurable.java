package com.ljh.request.requestman.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.search.FontManager;
import com.ljh.request.requestman.util.ProjectSettingsManager;
import com.ljh.request.requestman.util.ProjectUtils;
import com.ljh.request.requestman.util.PerformanceMonitor;
import com.ljh.request.requestman.search.ApiSearchPopup;
import com.ljh.request.requestman.ui.EnvironmentManagerPanel;
import com.ljh.request.requestman.util.LanguageManager;
import com.ljh.request.requestman.util.RequestManBundle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author leijianhui
 * @Description 插件设置页配置类，支持项目级别的设置和全局变量管理。RequestMan 插件设置页，支持前置URL配置。
 * @date 2025/06/19 09:36
 */
public class RequestManSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JTextField preUrlField;
    private JTextField cacheDirField;
    private JButton selectDirButton;
    // 新增：Tab面板
    private JTabbedPane tabbedPane;
    private VariablePanel variablePanel;
    private JTextField globalAuthField;

    /**
     * 环境管理面板
     */
    private EnvironmentManagerPanel environmentManagerPanel;
    private JRadioButton instantSearchRadio;
    private JRadioButton initSearchRadio;
    private JRadioButton popupInitSearchRadio;
    private ButtonGroup searchModeGroup;
    private JCheckBox includeLibsCheckBox;
    private JPanel apiSearchPanel;
    // 性能优化设置
    private JCheckBox enablePerformanceMonitoringCheckBox;
    // 移除自动扫描设置，避免与接口搜索设置冲突
    // 移除缓存大小设置，避免影响用户搜索体验
    private JSpinner scanTimeoutSpinner;
    // 接口搜索字体大小设置
    private JSpinner searchFontSizeSpinner;
    // 静默保存设置
    private JCheckBox autoSaveCheckBox;

    // 语言设置
    private JComboBox<String> languageComboBox;

    // 当前项目对象
    private Project currentProject;

    // 默认tab
    private int defaultTabIndex = 0;


    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return RequestManBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        // 获取当前项目
        currentProject = ProjectUtils.getCurrentProject();

        // 保证每次打开设置页都加载项目变量
        if (currentProject != null) {
            // 项目变量现在是动态加载的，不需要手动加载
        }

        // 主Tab面板
        tabbedPane = new JTabbedPane();
        
        // 基础设置面板 - 使用更紧凑的布局
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 基础设置表单（紧凑对齐）
        JPanel basicForm = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 语言设置
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel languageLabel = new JLabel(RequestManBundle.message("language.title") + ":");
        languageLabel.setPreferredSize(new Dimension(100, 25));
        basicForm.add(languageLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        languageComboBox = new JComboBox<>(new String[]{
                RequestManBundle.message("language.english"),
                RequestManBundle.message("language.chinese")
        });
        languageComboBox.setPreferredSize(new Dimension(150, 25));
        String langCode = LanguageManager.getLanguageCode();
        languageComboBox.setSelectedIndex("zh_CN".equals(langCode) ? 1 : 0);
        basicForm.add(languageComboBox, gbc);

        // 缓存目录
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel cacheDirLabel = new JLabel(RequestManBundle.message("settings.cacheDir") + ":");
        cacheDirLabel.setPreferredSize(new Dimension(100, 25));
        basicForm.add(cacheDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cacheDirField = new JTextField();
        cacheDirField.setPreferredSize(new Dimension(200, 25));
        basicForm.add(cacheDirField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        selectDirButton = new JButton(RequestManBundle.message("settings.chooseDir"));
        selectDirButton.setPreferredSize(new Dimension(80, 25));
        selectDirButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                cacheDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        basicForm.add(selectDirButton, gbc);

        // 全局认证信息
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel authLabel = new JLabel(RequestManBundle.message("settings.globalAuth") + ":");
        authLabel.setPreferredSize(new Dimension(100, 25));
        basicForm.add(authLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        globalAuthField = new JTextField();
        globalAuthField.setPreferredSize(new Dimension(200, 25));
        basicForm.add(globalAuthField, gbc);
        gbc.gridwidth = 1;

        mainPanel.add(basicForm, BorderLayout.NORTH);
        // 新增：全局变量Tab - 传递正确的项目对象
        variablePanel = new VariablePanel(currentProject);

        // 新增：环境管理Tab
        environmentManagerPanel = new EnvironmentManagerPanel(currentProject);

        tabbedPane.addTab(RequestManBundle.message("settings.tab.basic"), mainPanel);
        tabbedPane.addTab(RequestManBundle.message("settings.tab.environment"), environmentManagerPanel);
        tabbedPane.addTab(RequestManBundle.message("settings.tab.variables"), variablePanel);
        // 新增：接口搜索Tab - 使用更紧凑的布局
        apiSearchPanel = new JPanel(new BorderLayout());
        apiSearchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建垂直布局容器
        JPanel searchContentPanel = new JPanel();
        searchContentPanel.setLayout(new BoxLayout(searchContentPanel, BoxLayout.Y_AXIS));
        
        // 搜索模式单选框
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        modePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.search.mode.title")
        ));
        instantSearchRadio = new JRadioButton(RequestManBundle.message("settings.search.mode.instant"));
        initSearchRadio = new JRadioButton(RequestManBundle.message("settings.search.mode.init"));
        popupInitSearchRadio = new JRadioButton(RequestManBundle.message("settings.search.mode.popup_init"));
        searchModeGroup = new ButtonGroup();
        searchModeGroup.add(instantSearchRadio);
        searchModeGroup.add(initSearchRadio);
        searchModeGroup.add(popupInitSearchRadio);
        modePanel.add(instantSearchRadio);
        modePanel.add(initSearchRadio);
        modePanel.add(popupInitSearchRadio);
        searchContentPanel.add(modePanel);
        
        // 三方包勾选
        JPanel libsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        libsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.search.libs.title")
        ));
        includeLibsCheckBox = new JCheckBox(RequestManBundle.message("settings.search.includeLibs"));
        libsPanel.add(includeLibsCheckBox);

        // 添加性能提示
        JLabel performanceTipLabel = new JLabel(RequestManBundle.message("settings.search.performance.tip"));
        performanceTipLabel.setForeground(Color.GRAY);
        performanceTipLabel.setFont(performanceTipLabel.getFont().deriveFont(Font.ITALIC, performanceTipLabel.getFont().getSize() - 1));
        libsPanel.add(performanceTipLabel);

        searchContentPanel.add(libsPanel);

        // 字体大小设置
        JPanel fontSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        fontSizePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.font.title")
        ));
        fontSizePanel.add(new JLabel(RequestManBundle.message("settings.font.size")));
        // 获取默认字体大小
        int defaultFontSize = getDefaultFontSize();
        searchFontSizeSpinner = new JSpinner(new SpinnerNumberModel(defaultFontSize, 12, 32, 1));
        fontSizePanel.add(searchFontSizeSpinner);
        JLabel fontSizeTipLabel = new JLabel(RequestManBundle.message("settings.font.tip"));
        fontSizeTipLabel.setForeground(Color.GRAY);
        fontSizeTipLabel.setFont(fontSizeTipLabel.getFont().deriveFont(Font.ITALIC, fontSizeTipLabel.getFont().getSize() - 1));
        fontSizePanel.add(fontSizeTipLabel);

        searchContentPanel.add(fontSizePanel);
        
        apiSearchPanel.add(searchContentPanel, BorderLayout.NORTH);
        // 加载已保存配置
        String searchMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        if ("init".equals(searchMode)) {
            initSearchRadio.setSelected(true);
        } else if ("popup_init".equals(searchMode)) {
            popupInitSearchRadio.setSelected(true);
        } else {
            instantSearchRadio.setSelected(true);
        }
        Boolean includeLibsObj = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);
        // 只要配置不存在或为false都不勾选
        includeLibsCheckBox.setSelected(includeLibsObj != null && includeLibsObj);

        // 加载字体大小配置
        int savedFontSize = getIntValue("requestman.searchFontSize", getDefaultFontSize());
        searchFontSizeSpinner.setValue(savedFontSize);
        
        // 加入Tab
        tabbedPane.addTab(RequestManBundle.message("settings.tab.search"), apiSearchPanel);

        // 新增：性能优化Tab - 使用更紧凑的布局
        JPanel performancePanel = new JPanel(new BorderLayout());
        performancePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建垂直布局容器
        JPanel performanceContentPanel = new JPanel();
        performanceContentPanel.setLayout(new BoxLayout(performanceContentPanel, BoxLayout.Y_AXIS));

        // 性能监控设置
        JPanel monitoringPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        monitoringPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.performance.title")
        ));
        enablePerformanceMonitoringCheckBox = new JCheckBox(RequestManBundle.message("settings.performance.enable"));
        monitoringPanel.add(enablePerformanceMonitoringCheckBox);

        monitoringPanel.add(Box.createHorizontalStrut(20));

        JButton viewPerformanceReportButton = new JButton(RequestManBundle.message("settings.performance.view"));
        viewPerformanceReportButton.addActionListener(e -> showPerformanceReport());
        monitoringPanel.add(viewPerformanceReportButton);

        performanceContentPanel.add(monitoringPanel);

        // 移除自动扫描设置，避免与接口搜索设置冲突

        // 移除缓存设置，避免影响用户搜索体验

        // 扫描超时设置
        JPanel timeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        timeoutPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.timeout.title")
        ));
        timeoutPanel.add(new JLabel(RequestManBundle.message("settings.timeout.seconds")));
        scanTimeoutSpinner = new JSpinner(new SpinnerNumberModel(60, 30, 180, 10));
        timeoutPanel.add(scanTimeoutSpinner);
        performanceContentPanel.add(timeoutPanel);

        // 静默保存设置
        JPanel autoSavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        autoSavePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            RequestManBundle.message("settings.autosave.title")
        ));
        autoSaveCheckBox = new JCheckBox(RequestManBundle.message("settings.autosave.enable"));
        autoSaveCheckBox.setToolTipText(RequestManBundle.message("settings.autosave.tip"));
        autoSavePanel.add(autoSaveCheckBox);
        JLabel autoSaveTipLabel = new JLabel(RequestManBundle.message("settings.autosave.tip"));
        autoSaveTipLabel.setForeground(Color.GRAY);
        autoSaveTipLabel.setFont(autoSaveTipLabel.getFont().deriveFont(Font.ITALIC, autoSaveTipLabel.getFont().getSize() - 1));
        autoSavePanel.add(autoSaveTipLabel);
        performanceContentPanel.add(autoSavePanel);
        
        performancePanel.add(performanceContentPanel, BorderLayout.NORTH);
        
        // 加载静默保存配置
        boolean savedAutoSave = PropertiesComponent.getInstance().getBoolean("requestman.autoSave", false);
        autoSaveCheckBox.setSelected(savedAutoSave);

        tabbedPane.addTab(RequestManBundle.message("settings.tab.performance"), performancePanel);
        // 组件创建后，再切换 tab
        SwingUtilities.invokeLater(() -> {
            if (tabbedPane != null && defaultTabIndex >= 0 && defaultTabIndex < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(defaultTabIndex);
            }
        });
        return tabbedPane;
    }

    @Override
    public boolean isModified() {
        // 检查项目级别的设置
        String savedGlobalAuth = "";
        if (currentProject != null) {
            savedGlobalAuth = ProjectSettingsManager.getProjectGlobalAuth(currentProject);
        }

        String savedCacheDir = PropertiesComponent.getInstance().getValue("requestman.cacheDir", Paths.get(System.getProperty("user.home"), ".requestman_cache").toString() + File.separator);
        String savedSearchMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        boolean savedIncludeLibs = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);
        boolean savedPerformanceMonitoring = PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
        int savedScanTimeout = getIntValue("requestman.scanTimeout", 60);
        int savedFontSize = getIntValue("requestman.searchFontSize", getDefaultFontSize());
        boolean savedAutoSave = PropertiesComponent.getInstance().getBoolean("requestman.autoSave", false);
        String savedLanguage = LanguageManager.getLanguageCode();

        String curMode = initSearchRadio != null && initSearchRadio.isSelected() ? "init" : "instant";
        boolean curLibs = includeLibsCheckBox != null && includeLibsCheckBox.isSelected();
        boolean curPerformanceMonitoring = enablePerformanceMonitoringCheckBox != null && enablePerformanceMonitoringCheckBox.isSelected();
        int curScanTimeout = scanTimeoutSpinner != null ? (Integer) scanTimeoutSpinner.getValue() : 30;
        int curFontSize = searchFontSizeSpinner != null ? (Integer) searchFontSizeSpinner.getValue() : getDefaultFontSize();
        boolean curAutoSave = autoSaveCheckBox != null && autoSaveCheckBox.isSelected();
        String curLanguage = getSelectedLanguageCode();

        // 检查全局变量是否有未保存的修改
        boolean variableChanged = variablePanel != null && variablePanel.hasUnsavedChanges();

        return !Objects.equals(savedGlobalAuth, globalAuthField.getText()) ||
                !Objects.equals(savedCacheDir, cacheDirField.getText()) ||
                !Objects.equals(savedSearchMode, curMode) ||
                savedIncludeLibs != curLibs ||
                savedPerformanceMonitoring != curPerformanceMonitoring ||
                savedScanTimeout != curScanTimeout ||
                savedFontSize != curFontSize ||
                savedAutoSave != curAutoSave ||
                !Objects.equals(savedLanguage, curLanguage) ||
                variableChanged;
    }

    @Override
    public void apply() {
        // 获取当前设置值
        String mode;
        if (initSearchRadio != null && initSearchRadio.isSelected()) {
            mode = "init";
        } else if (popupInitSearchRadio != null && popupInitSearchRadio.isSelected()) {
            mode = "popup_init";
        } else {
            mode = "instant";
        }
        boolean libs = includeLibsCheckBox != null && includeLibsCheckBox.isSelected();

        // 获取保存的设置值进行比较
        String savedSearchMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        boolean savedIncludeLibs = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);

        // 检查搜索模式或扫描三方包设置是否发生变更
        boolean searchSettingsChanged = !Objects.equals(savedSearchMode, mode) || savedIncludeLibs != libs;

        // 保存项目级别的设置
        if (currentProject != null) {
            ProjectSettingsManager.setProjectGlobalAuth(currentProject, globalAuthField.getText());
        }

        // 缓存目录仍然是全局设置，因为它是系统级别的
        PropertiesComponent.getInstance().setValue("requestman.cacheDir", cacheDirField.getText());

        // 保存搜索相关设置
        PropertiesComponent.getInstance().setValue("requestman.searchMode", mode);
        PropertiesComponent.getInstance().setValue("requestman.includeLibs", libs);

        // 保存性能优化设置
        boolean performanceMonitoring = enablePerformanceMonitoringCheckBox != null && enablePerformanceMonitoringCheckBox.isSelected();
        int scanTimeout = scanTimeoutSpinner != null ? (Integer) scanTimeoutSpinner.getValue() : 30;

        PropertiesComponent.getInstance().setValue("requestman.performanceMonitoring", performanceMonitoring);
        PropertiesComponent.getInstance().setValue("requestman.scanTimeout", String.valueOf(scanTimeout));

        // 保存字体大小设置
        int fontSize = searchFontSizeSpinner != null ? (Integer) searchFontSizeSpinner.getValue() : getDefaultFontSize();
        PropertiesComponent.getInstance().setValue("requestman.searchFontSize", String.valueOf(fontSize));

        // 保存静默保存设置
        boolean autoSave = autoSaveCheckBox != null && autoSaveCheckBox.isSelected();
        PropertiesComponent.getInstance().setValue("requestman.autoSave", autoSave);

        // 保存语言并触发刷新
        String languageCode = getSelectedLanguageCode();
        LanguageManager.setLanguage(languageCode);

        // 通知RequestManPanel更新自动保存设置
        if (currentProject != null) {
            RequestManPanel requestManPanel = RequestManPanel.findRequestManPanel(currentProject);
            if (requestManPanel != null) {
                requestManPanel.updateAutoSaveSetting();
            }
        }

        // 刷新字体缓存，使设置立即生效
        FontManager.refreshFont();

        // 自动保存全局变量的修改
        if (variablePanel != null) {
            variablePanel.saveAllOnApply();
        }

        // 直接刷新环境选择器
        if (currentProject != null) {
            // 检查环境是否有变动
            boolean environmentChanged = checkEnvironmentChanged();
            if (environmentChanged) {
                // 查找并刷新RequestManPanel的环境选择器
                RequestManPanel requestManPanel = RequestManPanel.findRequestManPanel(currentProject);
                if (requestManPanel != null) {
                    requestManPanel.refreshEnvironmentSelector();
                }
            }
        }

        // 只有当搜索模式或扫描三方包设置发生变更时，才重新扫描并缓存
        if (searchSettingsChanged && StringUtils.equalsAny(mode, "init", "popup_init")) {
            // 使用当前项目进行缓存操作
            Project targetProject = currentProject != null ? currentProject : ProjectUtils.getCurrentProject();
            if (targetProject != null) {
                ApiSearchPopup.cacheApisOnSettingSaved(targetProject, libs);
            }
        }
    }

    @Override
    public void reset() {
        // 加载项目级别的设置
        if (currentProject != null) {
            globalAuthField.setText(ProjectSettingsManager.getProjectGlobalAuth(currentProject));
        } else {
            globalAuthField.setText("");
        }

        // 缓存目录仍然是全局设置
        cacheDirField.setText(PropertiesComponent.getInstance().getValue("requestman.cacheDir", Paths.get(System.getProperty("user.home"), ".requestman_cache").toString() + File.separator));

        String searchMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        if ("init".equals(searchMode)) {
            initSearchRadio.setSelected(true);
        } else if ("popup_init".equals(searchMode)) {
            popupInitSearchRadio.setSelected(true);
        } else {
            instantSearchRadio.setSelected(true);
        }
        Boolean includeLibsObj = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);
        includeLibsCheckBox.setSelected(includeLibsObj != null && includeLibsObj);

        // 加载性能优化设置
        boolean performanceMonitoring = PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
        // 移除自动扫描相关配置
        // 移除缓存大小相关配置
        int scanTimeout = getIntValue("requestman.scanTimeout", 60);

        if (enablePerformanceMonitoringCheckBox != null) {
            enablePerformanceMonitoringCheckBox.setSelected(performanceMonitoring);
        }
        // 移除自动扫描相关配置
        // 移除缓存大小相关配置
        if (scanTimeoutSpinner != null) {
            scanTimeoutSpinner.setValue(scanTimeout);
        }

        // 加载字体大小设置
        int fontSize = getIntValue("requestman.searchFontSize", getDefaultFontSize());
        if (searchFontSizeSpinner != null) {
            searchFontSizeSpinner.setValue(fontSize);
        }

        // 初始化环境配置（如果不存在）
        if (currentProject != null) {
            String savedEnvironment = PropertiesComponent.getInstance().getValue("requestman.environmentConfig", "");
            if (savedEnvironment.isEmpty()) {
                String currentEnvironment = EnvironmentManagerPanel.getEnvironmentConfig(currentProject);
                PropertiesComponent.getInstance().setValue("requestman.environmentConfig", currentEnvironment);
            }
        }

        // 语言选择
        String langCode = LanguageManager.getLanguageCode();
        if (languageComboBox != null) {
            languageComboBox.setSelectedIndex("zh_CN".equals(langCode) ? 1 : 0);
        }
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        cacheDirField = null;
        selectDirButton = null;
        tabbedPane = null;
        variablePanel = null;
        environmentManagerPanel = null;
        globalAuthField = null;
        instantSearchRadio = null;
        initSearchRadio = null;
        popupInitSearchRadio = null;
        searchModeGroup = null;
        includeLibsCheckBox = null;
        apiSearchPanel = null;
    }

    /**
     * 切换到环境管理tab
     */
    public void switchToEnvironmentTab(int tabIndex) {
        this.defaultTabIndex = tabIndex;
        if (tabbedPane != null) {
            // 环境管理tab的索引是1（基础设置是0，环境管理是1，全局变量是2，接口搜索是3，性能优化是4）
            tabbedPane.setSelectedIndex(tabIndex);
        }
    }

    /**
     * 打开设置页面并切换到指定tab
     *
     * @param project  项目对象
     * @param tabIndex 要切换到的tab索引
     */
    public static void showSettingsDialog(Project project, int tabIndex) {
        // 打开设置页面
        com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(project, RequestManSettingsConfigurable.class, configurable -> {
            configurable.switchToEnvironmentTab(tabIndex);
        });
    }


    /**
     * 获取默认字体大小（IDEA字体大小加8）
     */
    private int getDefaultFontSize() {
        // 优先使用IDEA的编辑器字体大小
        Font editorFont = UIManager.getFont("EditorPane.font");
        if (editorFont != null) {
            return editorFont.getSize() + 8;
        }

        // 如果没有编辑器字体，使用标签字体大小
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.getSize() + 8;
        }

        // 最后使用默认值
        return 20;
    }

    /**
     * 安全获取整数值，如果解析失败则返回默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 整数值
     */
    private int getIntValue(String key, int defaultValue) {
        try {
            String value = PropertiesComponent.getInstance().getValue(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 显示性能报告对话框
     */
    private void showPerformanceReport() {
        try {
            // 获取性能报告
            String report = PerformanceMonitor.getPerformanceReport();

            // 创建对话框
            JDialog dialog = new JDialog((java.awt.Frame) null, RequestManBundle.message("settings.performance.report.title"), true);
            dialog.setLayout(new BorderLayout(10, 10));

            // 创建文本区域显示报告
            JTextArea textArea = new JTextArea(report);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            // 添加滚动条
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));

            // 创建按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton closeButton = new JButton(RequestManBundle.message("settings.performance.report.close"));
            JButton clearButton = new JButton(RequestManBundle.message("settings.performance.report.clear"));

            closeButton.addActionListener(e -> dialog.dispose());
            clearButton.addActionListener(e -> {
                PerformanceMonitor.clearStats();
                textArea.setText(PerformanceMonitor.getPerformanceReport());
            });

            buttonPanel.add(closeButton);
            buttonPanel.add(clearButton);

            // 组装对话框
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // 设置对话框属性
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to get performance report: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 检查环境是否有变动
     */
    private boolean checkEnvironmentChanged() {
        if (currentProject == null) {
            return false;
        }

        // 获取当前环境配置
        String currentEnvironment = EnvironmentManagerPanel.getEnvironmentConfig(currentProject);
        // 获取保存的环境配置
        String savedEnvironment = PropertiesComponent.getInstance().getValue("requestman.environmentConfig", "");

        boolean changed = !Objects.equals(currentEnvironment, savedEnvironment);

        // 如果有变动，保存新的配置
        if (changed) {
            PropertiesComponent.getInstance().setValue("requestman.environmentConfig", currentEnvironment);
        }

        return changed;
    }

    /**
     * 获取下拉选中的语言代码
     */
    private String getSelectedLanguageCode() {
        if (languageComboBox == null) {
            return LanguageManager.getLanguageCode();
        }
        int idx = languageComboBox.getSelectedIndex();
        return idx == 1 ? "zh_CN" : "en";
    }
}
