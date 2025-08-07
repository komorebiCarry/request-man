package com.ljh.request.requestman.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.util.ProjectSettingsManager;
import com.ljh.request.requestman.util.ProjectUtils;
import com.ljh.request.requestman.util.PerformanceMonitor;
import com.ljh.request.requestman.search.ApiSearchPopup;
import com.ljh.request.requestman.ui.EnvironmentManagerPanel;
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

    // 当前项目对象
    private Project currentProject;

    // 默认tab
    private int defaultTabIndex = 0;


    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "RequestMan 设置";
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
        // 基础设置面板
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        // 前置URL配置区域（已移至环境管理中）
        // JPanel preUrlPanel = new JPanel(new BorderLayout(8, 8));
        // JLabel preUrlLabel = new JLabel("前置URL:");
        // preUrlField = new JTextField();
        // preUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        // preUrlPanel.add(preUrlLabel, BorderLayout.WEST);
        // preUrlPanel.add(preUrlField, BorderLayout.CENTER);
        // preUrlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        // mainPanel.add(preUrlPanel);
        // 缓存目录配置区域
        JPanel cacheDirPanel = new JPanel(new BorderLayout(8, 8));
        JLabel cacheDirLabel = new JLabel("缓存目录:");
        cacheDirField = new JTextField();
        cacheDirField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        selectDirButton = new JButton("选择目录");
        selectDirButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                cacheDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        cacheDirPanel.add(cacheDirLabel, BorderLayout.WEST);
        cacheDirPanel.add(cacheDirField, BorderLayout.CENTER);
        cacheDirPanel.add(selectDirButton, BorderLayout.EAST);
        cacheDirPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(cacheDirPanel);

        // 全局认证信息配置（按项目缓存）
        JPanel authPanel = new JPanel(new BorderLayout(8, 8));
        JLabel authLabel = new JLabel("全局认证信息:");
        globalAuthField = new JTextField();
        globalAuthField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        authPanel.add(authLabel, BorderLayout.WEST);
        authPanel.add(globalAuthField, BorderLayout.CENTER);
        authPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(authPanel);
        // 新增：全局变量Tab - 传递正确的项目对象
        variablePanel = new VariablePanel(currentProject);

        // 新增：环境管理Tab
        environmentManagerPanel = new EnvironmentManagerPanel(currentProject);

        tabbedPane.addTab("基础设置", mainPanel);
        tabbedPane.addTab("环境管理", environmentManagerPanel);
        tabbedPane.addTab("全局变量", variablePanel);
        // 新增：接口搜索Tab
        apiSearchPanel = new JPanel();
        apiSearchPanel.setLayout(new BoxLayout(apiSearchPanel, BoxLayout.Y_AXIS));
        // 搜索模式单选框
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.setBorder(BorderFactory.createTitledBorder("接口搜索模式"));
        instantSearchRadio = new JRadioButton("即时搜索");
        initSearchRadio = new JRadioButton("项目启动初始化搜索");
        popupInitSearchRadio = new JRadioButton("弹窗初始化搜索");
        searchModeGroup = new ButtonGroup();
        searchModeGroup.add(instantSearchRadio);
        searchModeGroup.add(initSearchRadio);
        searchModeGroup.add(popupInitSearchRadio);
        modePanel.add(instantSearchRadio);
        modePanel.add(initSearchRadio);
        modePanel.add(popupInitSearchRadio);
        apiSearchPanel.add(modePanel);
        // 三方包勾选
        JPanel libsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        includeLibsCheckBox = new JCheckBox("扫描三方包");
        libsPanel.add(includeLibsCheckBox);

        // 添加性能提示
        JLabel performanceTipLabel = new JLabel("(大项目建议关闭以提高性能)");
        performanceTipLabel.setForeground(Color.GRAY);
        performanceTipLabel.setFont(performanceTipLabel.getFont().deriveFont(Font.ITALIC, performanceTipLabel.getFont().getSize() - 1));
        libsPanel.add(performanceTipLabel);

        apiSearchPanel.add(libsPanel);

        // 字体大小设置
        JPanel fontSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fontSizePanel.setBorder(BorderFactory.createTitledBorder("字体设置"));
        fontSizePanel.add(new JLabel("搜索框字体大小:"));
        // 获取默认字体大小
        int defaultFontSize = getDefaultFontSize();
        searchFontSizeSpinner = new JSpinner(new SpinnerNumberModel(defaultFontSize, 12, 32, 1));
        fontSizePanel.add(searchFontSizeSpinner);
        JLabel fontSizeTipLabel = new JLabel("(影响搜索框和结果列表的字体大小，需要重新打开搜索弹窗生效)");
        fontSizeTipLabel.setForeground(Color.GRAY);
        fontSizeTipLabel.setFont(fontSizeTipLabel.getFont().deriveFont(Font.ITALIC, fontSizeTipLabel.getFont().getSize() - 1));
        fontSizePanel.add(fontSizeTipLabel);

        apiSearchPanel.add(fontSizePanel);
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
        tabbedPane.addTab("接口搜索", apiSearchPanel);

        // 新增：性能优化Tab
        JPanel performancePanel = new JPanel();
        performancePanel.setLayout(new BoxLayout(performancePanel, BoxLayout.Y_AXIS));

        // 性能监控设置
        JPanel monitoringPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monitoringPanel.setBorder(BorderFactory.createTitledBorder("性能监控"));
        enablePerformanceMonitoringCheckBox = new JCheckBox("启用性能监控");
        monitoringPanel.add(enablePerformanceMonitoringCheckBox);

        monitoringPanel.add(Box.createHorizontalStrut(20));

        JButton viewPerformanceReportButton = new JButton("查看性能报告");
        viewPerformanceReportButton.addActionListener(e -> showPerformanceReport());
        monitoringPanel.add(viewPerformanceReportButton);

        performancePanel.add(monitoringPanel);

        // 移除自动扫描设置，避免与接口搜索设置冲突

        // 移除缓存设置，避免影响用户搜索体验

        // 扫描超时设置
        JPanel timeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeoutPanel.setBorder(BorderFactory.createTitledBorder("扫描超时"));
        timeoutPanel.add(new JLabel("扫描超时时间(秒):"));
        scanTimeoutSpinner = new JSpinner(new SpinnerNumberModel(60, 30, 180, 10));
        timeoutPanel.add(scanTimeoutSpinner);
        performancePanel.add(timeoutPanel);

        tabbedPane.addTab("性能优化", performancePanel);
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

        String curMode = initSearchRadio != null && initSearchRadio.isSelected() ? "init" : "instant";
        boolean curLibs = includeLibsCheckBox != null && includeLibsCheckBox.isSelected();
        boolean curPerformanceMonitoring = enablePerformanceMonitoringCheckBox != null && enablePerformanceMonitoringCheckBox.isSelected();
        int curScanTimeout = scanTimeoutSpinner != null ? (Integer) scanTimeoutSpinner.getValue() : 30;
        int curFontSize = searchFontSizeSpinner != null ? (Integer) searchFontSizeSpinner.getValue() : getDefaultFontSize();

        // 检查全局变量是否有未保存的修改
        boolean variableChanged = variablePanel != null && variablePanel.hasUnsavedChanges();

        return !Objects.equals(savedGlobalAuth, globalAuthField.getText()) ||
                !Objects.equals(savedCacheDir, cacheDirField.getText()) ||
                !Objects.equals(savedSearchMode, curMode) ||
                savedIncludeLibs != curLibs ||
                savedPerformanceMonitoring != curPerformanceMonitoring ||
                savedScanTimeout != curScanTimeout ||
                savedFontSize != curFontSize ||
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

        // 刷新字体缓存，使设置立即生效
        ApiSearchPopup.refreshFont();

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
            JDialog dialog = new JDialog((java.awt.Frame) null, "RequestMan 性能报告", true);
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
            JButton closeButton = new JButton("关闭");
            JButton clearButton = new JButton("清除统计数据");

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
                    "获取性能报告失败: " + e.getMessage(),
                    "错误",
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
}
