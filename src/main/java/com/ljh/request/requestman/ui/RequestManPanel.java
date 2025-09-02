package com.ljh.request.requestman.ui;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.f.Z.K.S;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import com.ljh.request.requestman.enums.ContentType;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.search.ApiSearchPopup;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;
import com.ljh.request.requestman.util.*;
import com.ljh.request.requestman.util.RequestManBundle;
import com.ljh.request.requestman.ui.ImportExportDialog;
import com.ljh.request.requestman.ui.builder.RequestViewBuilders;
import com.ljh.request.requestman.ui.builder.TopPanelBuilder;
import com.ljh.request.requestman.ui.builder.RequestPanelsBuilder;
import com.ljh.request.requestman.ui.builder.CustomEditPanelsBuilder;
import com.ljh.request.requestman.util.TableEditingManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.DropMode;
import javax.swing.TransferHandler;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * RequestMan主面板，负责集成和展示接口列表、参数编辑、请求发送、响应展示等核心功能。
 * 该类为插件的主界面入口，支持自动扫描与自定义接口两种模式切换。
 *
 * @author leijianhui
 * @Description RequestMan主面板，集成接口列表、参数编辑、请求发送、响应展示等功能。
 * @date 2025/06/19 09:36
 */
public class RequestManPanel extends JPanel {
    /**
     * 当前项目对象
     */
    private final Project project;
    /**
     * 接口下拉选择模型
     */
    private final DefaultComboBoxModel<ApiInfo> apiComboBoxModel = new DefaultComboBoxModel<>();
    /**
     * 接口下拉选择控件
     */
    private final JComboBox<ApiInfo> apiComboBox = new JComboBox<>(apiComboBoxModel);
    /**
     * 详情面板
     */
    private final JPanel detailPanel = new JPanel(new BorderLayout());
    /**
     * 刷新接口按钮
     */
    private final JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
    /**
     * 统一参数Tab内容高度
     */
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 120);

    /**
     * 拖拽起始索引
     */
    private int dragIndex = -1;
    /**
     * 列表变更持久化防抖
     */
    private Timer persistDebounce;
    /**
     * 拖拽插入位置索引（用于渲染分割线）
     */
    private int dropLineIndex = -1;
    /**
     * 是否为插入模式（JList DropLocation.isInsert）
     */
    private boolean dropInsert = true;

    /**
     * Headers 参数面板，便于持久化操作
     */
    private HeadersPanel headersPanel;
    /**
     * Cookies 参数面板，便于持久化操作
     */
    private CookiesPanel cookiesPanel;
    /**
     * Auth 参数面板，便于持久化操作
     */
    private AuthPanel authPanel;
    /**
     * PostOp 参数面板，便于持久化操作
     */
    private PostOpPanel postOpPanel;
    /**
     * PreOpPanel 参数面板，便于持久化操作
     */
    private PreOpPanel preOpPanel;
    /**
     * 模式切换按钮（单一按钮，图标随模式切换）
     */
    private JButton modeSwitchBtn;
    /**
     * 当前模式，true为自定义接口模式，false为自动扫描模式
     */
    private boolean customMode = false;
    /**
     * 自定义接口列表模型和控件
     */
    private DefaultListModel<CustomApiInfo> customApiListModel;
    private JList<CustomApiInfo> customApiList;
    /**
     * 自定义接口编辑面板
     */
    private JPanel customEditPanel;
    private JTextField customNameField;
    private JTextField customUrlField;
    private JComboBox<String> customMethodBox;
    /**
     * 自定义接口请求体编辑面板（可编辑多类型）
     */
    private EditableBodyPanel customBodyPanel;
    private JButton saveCustomBtn;
    private JButton deleteCustomBtn;
    private CustomApiInfo editingApi;
    /**
     * 自定义接口主分栏面板
     */
    private JSplitPane customPanel;
    /**
     * 自定义接口参数编辑面板
     */
    private ParamsTablePanel customParamsPanel;
    /**
     * 自定义接口前置操作编辑面板
     */
    private PreOpPanel customPreOpPanel;
    /**
     * 自定义接口后置操作编辑面板
     */
    private PostOpPanel customPostOpPanel;
    /**
     * 主界面参数面板（自动扫描模式专用）
     */
    private ParamsTablePanel paramsPanel;
    /**
     * 主界面Body参数面板（自动扫描模式专用）
     */
    private BodyPanel bodyPanel;
    /**
     * 响应折叠面板（主界面和自定义接口共用）
     */
    private ResponseCollapsePanel responsePanel;
    /**
     * 自定义接口Tab顶部发送按钮
     */
    private JPanel customEditPanelSendBtnPanel;
    /**
     * 线程池已移至RequestSenderManager中统一管理
     * 此处不再需要重复定义
     */

    /**
     * 统计执行器，用于定期更新插件线程数量统计
     */
    private static final ScheduledExecutorService STATS_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RequestMan-Stats");
        t.setDaemon(true);
        return t;
    });

    static {
        // 定期更新插件线程数量统计
        STATS_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                // 使用ReadAction确保线程安全
                com.intellij.openapi.application.ReadAction.run(() -> {
                    try {
                        // 统计RequestMan相关的线程数量
                        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
                        while (rootGroup.getParent() != null) {
                            rootGroup = rootGroup.getParent();
                        }

                        int requestManThreads = 0;
                        Thread[] threads = new Thread[rootGroup.activeCount()];
                        rootGroup.enumerate(threads);

                        for (Thread thread : threads) {
                            if (thread != null && thread.getName().contains("RequestMan")) {
                                requestManThreads++;
                            }
                        }

                        PerformanceMonitor.updatePluginThreadCount(requestManThreads);

                    } catch (Exception e) {
                        // 静默处理异常
                    }
                });

            } catch (Exception e) {
                // 静默处理异常
            }
        }, 5, 30, TimeUnit.SECONDS); // 5秒后开始，每30秒更新一次
    }

    /**
     * 静态初始化块，添加JVM关闭时的清理
     * 注意：请求发送线程池已在RequestSenderManager中管理
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 关闭统计执行器
                STATS_EXECUTOR.shutdown();
                if (!STATS_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    STATS_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                STATS_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "RequestMan-ShutdownHook"));
    }

    /**
     * 自定义接口认证面板
     */
    private AuthPanel customAuthPanel;

    /**
     * 自定义接口cookies面板
     */
    private HeadersPanel customHeadersPanel;
    /**
     * 自定义接口cookies面板
     */
    private CookiesPanel customCookiesPanel;
    /**
     * 顶部接口搜索按钮
     */
    private JButton apiSearchButton;

    /**
     * 环境选择器
     */
    private EnvironmentSelector environmentSelector;

    /**
     * 自动保存管理器
     */
    private AutoSaveManager autoSaveManager;

    /**
     * 接口名称标签（用于显示未保存标识）
     */
    private JLabel customNameLabel;
    /**
     * 自定义接口名称星号标签（显示未保存标识，位于输入框右侧）
     */
    private JLabel customNameStarLabel;
    /**
     * 自定义接口名称标签基础与星号文本
     */
    private static final String CUSTOM_NAME_LABEL_TEXT = RequestManBundle.message("custom.name.label") + ":";
    private static final String CUSTOM_NAME_LABEL_TEXT_WITH_STAR_HTML = "<html>" + RequestManBundle.message("custom.name.label") + ": <font color='red'>*</font></html>";

    /**
     * 当前编辑的扫描接口信息
     */
    private ApiInfo currentScanningApi;

    /**
     * 扫描模式保存按钮
     */
    private JButton scanSaveButton;

    /**
     * 刷新标志，防止刷新过程中的重复调用
     */
    private boolean isRefreshing = false;

    /**
     * 当前选中的Tab索引，用于精确停止表格编辑
     */
    private int currentTabIndex = 0;


    // 静态实例管理，用于通知刷新
    private static final Map<Project, RequestManPanel> instances = new HashMap<>();

    /**
     * 扫描态基线缓存：key=StorageUtil.safeFileName(StorageUtil.buildApiKey(apiInfo, project))
     * 仅保存首次展示时的深拷贝，用于“还原”。
     */
    private final Map<String, ApiInfo> baselineByKey = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 显式清理基线缓存（项目关闭或刷新列表时调用）。
     */
    public void clearBaselines() {
        baselineByKey.clear();
    }

    /**
     * 是否处于批量初始化状态，防止监听器误触发
     */
    private boolean isInitializing = false;

    /**
     * Tab索引常量，用于精确停止表格编辑
     */
    private static final class TabIndex {
        public static final int PARAMS = 0;      // 参数
        public static final int BODY = 1;        // 请求体
        public static final int HEADERS = 2;     // 请求头
        public static final int COOKIES = 3;     // Cookie
        public static final int AUTH = 4;        // 认证
        public static final int PRE_OP = 5;      // 前置操作
        public static final int POST_OP = 6;     // 后置操作
    }

    /**
     * RequestManPanel构造函数
     */
    public RequestManPanel(Project project) {
        super(new BorderLayout());
        // 确保全局变量池初始化
        VariableManager.loadAll();
        this.project = project;
        setLayout(new BorderLayout());
        // 设置默认首选宽度和高度，优化插件初始显示宽度
        setPreferredSize(new Dimension(800, 600));

        // 初始化自动保存管理器
        autoSaveManager = new AutoSaveManager(project);
        // 初始化列表变更持久化防抖
        persistDebounce = new Timer(300, e -> CustomApiStorage.persistCustomApiList(project, customApiListModel));
        persistDebounce.setRepeats(false);
        autoSaveManager.setSaveCallback(this::autoSaveCustomApi);
        autoSaveManager.setScanSaveCallback(this::autoSaveScanningApi);
        autoSaveManager.setUiUpdateCallback(this::updateUIState);
        autoSaveManager.setLocalCacheSupplier(() -> {
            if (currentScanningApi != null) {
                return ApiCacheStorage.loadCustomEdit(currentScanningApi, project);
            }
            return null;
        });
        autoSaveManager.setUrlUpdateCallback(() -> {
            if (customMode && customParamsPanel != null) {
                updateUrlFromPathParams();
            }
        });

        // 顶部区域：模式选择 + 按钮 + 接口下拉框
        JPanel topPanel = buildTopPanel();
        // 详情区
        updateDetailPanelTitle();
        // 响应折叠面板
        responsePanel = new ResponseCollapsePanel(RequestManBundle.message("main.response.title"));
        responsePanel.setResponseText(RequestManBundle.message("main.response.placeholder"));
        // 主布局：顶部为topPanel，下方为详情区+响应区
        add(topPanel, BorderLayout.NORTH);
        add(detailPanel, BorderLayout.CENTER);
        add(responsePanel, BorderLayout.SOUTH);

        // 初始化按钮状态
        refreshButton.setToolTipText(RequestManBundle.message("main.refresh.tooltip"));

        // 初始化加载接口
        refreshApiList();

        // 添加到静态实例管理
        instances.put(project, this);
    }

    /**
     * 构建顶部面板（模式选择+刷新/新增按钮+接口下拉框），并绑定事件。
     *
     * @return 顶部面板
     */
    private JPanel buildTopPanel() {
        // 构建并初始化控件（保持原有行为与事件）
        JButton searchBtn = new JButton(AllIcons.Actions.Find);
        searchBtn.setToolTipText(RequestManBundle.message("main.search.tooltip"));
        searchBtn.setPreferredSize(new Dimension(36, 36));
        searchBtn.setMaximumSize(new Dimension(36, 36));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(true);
        searchBtn.addActionListener(e -> new ApiSearchPopup(project).show());
        this.apiSearchButton = searchBtn;

        JButton modeBtn = new JButton(RequestManBundle.message("main.mode.switch"));
        modeBtn.setFocusPainted(false);
        modeBtn.setBorderPainted(true);
        modeBtn.setPreferredSize(new Dimension(36, 36));
        modeBtn.setMaximumSize(new Dimension(36, 36));
        this.modeSwitchBtn = modeBtn;
        updateModeSwitchBtn();
        modeBtn.addActionListener(e -> {
            if (autoSaveManager != null) {
                autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
            } else {
                stopTableEditingForTabIndex(currentTabIndex);
            }
            if (!customMode) {
                refreshButton.setIcon(AllIcons.General.Add);
                refreshButton.setToolTipText(RequestManBundle.message("main.add.tooltip"));
                switchToCustomMode();
            } else {
                refreshButton.setIcon(AllIcons.Actions.Refresh);
                refreshButton.setToolTipText(RequestManBundle.message("main.refresh.tooltip"));
                switchToScanMode();
            }
            updateModeSwitchBtn();
        });

        if (refreshButton.getActionListeners().length == 0) {
            refreshButton.addActionListener(e -> {
                if (customMode) {
                    if (autoSaveManager != null) {
                        autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
                    } else {
                        stopTableEditingForTabIndex(currentTabIndex);
                    }
                    if (!checkUnsavedChanges()) {
                        return;
                    }
                    customApiList.clearSelection();
                    showCustomApiDetail(null);
                } else {
                    refreshApiList();
                }
            });
        }
        if (apiComboBox.getActionListeners().length == 0) {
            apiComboBox.addActionListener(e -> {
                if (isRefreshing) {
                    return;
                }
                if (!checkUnsavedChanges()) {
                    if (currentScanningApi != null) {
                        for (int i = 0; i < apiComboBox.getItemCount(); i++) {
                            ApiInfo api = apiComboBox.getItemAt(i);
                            if (api.equals(currentScanningApi)) {
                                apiComboBox.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    return;
                }
                ApiInfo selected = (ApiInfo) apiComboBox.getSelectedItem();
                showApiDetail(selected);
            });
        }

        JButton locateButton = new JButton(AllIcons.General.Locate);
        locateButton.setToolTipText(RequestManBundle.message("main.locate.tooltip"));
        locateButton.setPreferredSize(new Dimension(32, 32));
        locateButton.setMaximumSize(new Dimension(32, 32));
        locateButton.setFocusPainted(false);
        locateButton.setBorderPainted(true);
        locateButton.addActionListener(e -> {
            Editor editor1 = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor1 == null) return;
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor1.getDocument());
            if (psiFile == null) return;
            int offset = editor1.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (method == null) return;
            for (int i = 0; i < apiComboBox.getItemCount(); i++) {
                ApiInfo api = apiComboBox.getItemAt(i);
                if (method.getName().equals(api.getMethodName()) && methodParamTypesMatch(method, api.getParamTypes())) {
                    apiComboBox.setSelectedIndex(i);
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, RequestManBundle.message("main.locate.not.found"), RequestManBundle.message("main.tip"), JOptionPane.INFORMATION_MESSAGE);
        });

        if (environmentSelector == null) {
            environmentSelector = new EnvironmentSelector(project);
        }

        JButton performanceButton = null;
        boolean performanceMonitoringEnabled = PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
        if (performanceMonitoringEnabled) {
            performanceButton = new JButton("📊");
            performanceButton.setToolTipText(RequestManBundle.message("main.performance.tooltip"));
            performanceButton.setPreferredSize(new Dimension(36, 36));
            performanceButton.setMaximumSize(new Dimension(36, 36));
            performanceButton.setFocusPainted(false);
            performanceButton.setBorderPainted(true);
            performanceButton.addActionListener(e -> showPerformanceReport());
        }

        TopPanelBuilder.TopPanelContext ctx = new TopPanelBuilder.TopPanelContext();
        ctx.apiSearchButton = searchBtn;
        ctx.modeSwitchButton = modeBtn;
        ctx.refreshOrAddButton = refreshButton;
        ctx.apiComboBox = apiComboBox;
        ctx.locateButton = locateButton;
        ctx.environmentSelector = environmentSelector;
        ctx.performanceButton = performanceButton;

        return TopPanelBuilder.buildTopPanel(ctx);
    }

    /**
     * 根据当前模式切换按钮图标和提示。
     */
    private void updateModeSwitchBtn() {
        if (modeSwitchBtn == null) {
            return;
        }
        if (customMode) {
            modeSwitchBtn.setText("\u270E");
            modeSwitchBtn.setToolTipText(RequestManBundle.message("main.mode.to.scan"));
        } else {
            modeSwitchBtn.setText("\uD83D\uDCE1");
            modeSwitchBtn.setToolTipText(RequestManBundle.message("main.mode.to.custom"));
        }
    }

    /**
     * 刷新接口下拉框。
     * 1. 检查是否有未保存的更改
     * 2. 调用ApiInfoExtractor获取接口信息（后台线程）
     * 3. 填充下拉框模型（UI线程）
     * 4. 处理异常和空数据
     */
    private void refreshApiList() {
        baselineByKey.clear();
        // 刷新前，仅停止当前选中Tab的表格编辑，并以"立即更新"模式确保未保存状态同步
        if (autoSaveManager != null) {
            autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
        } else {
            stopTableEditingForTabIndex(currentTabIndex);
        }

        // 检查是否有未保存的更改，如果有则提示用户
        if (!checkUnsavedChanges()) {
            return; // 用户取消，停止刷新流程
        }
        // 设置刷新标志，防止ActionListener触发
        isRefreshing = true;
        apiComboBoxModel.removeAllElements();
        isRefreshing = false;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<ApiInfo> apis;
            try {
                apis = ApiInfoExtractor.extractApiInfos(project);
            } catch (ProcessCanceledException pce) {
                throw pce;
            } catch (Exception ex) {
                apis = null;
                ApplicationManager.getApplication().invokeLater(() -> {
                    apiComboBoxModel.addElement(new ApiInfo("加载接口时发生错误: " + ex.getMessage(), "", "", "", ""));
                    if (apiComboBoxModel.getSize() > 0) {
                        apiComboBox.setSelectedIndex(0);
                    }
                });
                return;
            }
            List<ApiInfo> finalApis = apis;
            ApplicationManager.getApplication().invokeLater(() -> {
                // 设置刷新标志，防止ActionListener触发
                isRefreshing = true;
                if (finalApis == null || finalApis.isEmpty()) {
                    apiComboBoxModel.addElement(new ApiInfo(RequestManBundle.message("main.no.api.detected"), "", "", "", ""));
                } else {
                    for (ApiInfo api : finalApis) {
                        apiComboBoxModel.addElement(api);
                    }
                }
                isRefreshing = false;
                if (apiComboBoxModel.getSize() > 0) {
                    apiComboBox.setSelectedIndex(0);
                }
            });
        });
    }

    /**
     * 展示接口详情（主Tab和参数Tab）。
     *
     * @param apiInfo 选中的接口信息
     */
    private void showApiDetail(ApiInfo apiInfo) {
        // 在加载缓存前，缓存基线（仅首次）
        try {
            String key = StorageUtil.safeFileName(StorageUtil.buildApiKey(apiInfo, project));
            baselineByKey.putIfAbsent(key, deepCopyApiInfo(apiInfo));
        } catch (Exception ignore) {
        }

        // 检查是否有未保存的更改，并且不是同一个接口
        if (!checkUnsavedChanges()) {
            return;
        }
        // 加载本地缓存
        apiInfo = ApiCacheStorage.loadCustomEdit(apiInfo, project);
        // 延迟清理旧的JsonBodyStructurePanel，避免过早清理导致的问题
        // 先构建新的面板，再清理旧的，确保平滑切换
        JLayeredPane layeredPane = new JLayeredPane();
        // 使用绝对布局，精确控制按钮层位置
        layeredPane.setLayout(null);

        JTabbedPane mainTab = buildMainTab(apiInfo);
        layeredPane.add(mainTab, JLayeredPane.DEFAULT_LAYER);
        // 创建还原按钮面板
        JPanel restoreButtonPanel = createRestoreButtonPanel(mainTab);
        layeredPane.add(restoreButtonPanel, JLayeredPane.PALETTE_LAYER);
        Runnable placeButton = () -> updateRestoreButtonBounds(mainTab, restoreButtonPanel, layeredPane);
        // 统一的尺寸监听，减少监听器数量，避免不必要的回调
        java.awt.event.ComponentAdapter resizeListener = new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                mainTab.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                SwingUtilities.invokeLater(placeButton);
            }
        };
        layeredPane.addComponentListener(resizeListener);
        // 首次放置
        SwingUtilities.invokeLater(() -> {
            mainTab.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            placeButton.run();
        });
        // Tab变化时也更新
        mainTab.addChangeListener(e -> SwingUtilities.invokeLater(placeButton));
        // 在替换面板之前清理旧的
        cleanupOldStructurePanels();

        detailPanel.removeAll();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText(RequestManBundle.message("main.response.placeholder"));
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        if (apiInfo == null) {
            detailPanel.add(new JLabel(RequestManBundle.message("main.no.api.selected")), BorderLayout.CENTER);
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }

        // 设置当前扫描接口
        currentScanningApi = apiInfo;
        if (autoSaveManager != null) {
            autoSaveManager.setCurrentScanningApi(apiInfo);
        }
        // 回显headers
        if (headersPanel != null && apiInfo.getHeaders() != null) {
            isInitializing = true;
            headersPanel.setHeadersData(apiInfo.getHeaders());
            isInitializing = false;
        }

        detailPanel.add(layeredPane, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();

        // 设置自动保存监听器
        setupScanningAutoSaveListeners();
        // 展示接口详情后，刷新保存按钮状态
        updateSaveButtonState();
    }

    /**
     * 清理旧的JsonBodyStructurePanel，避免内存泄漏
     */
    private void cleanupOldStructurePanels() {
        // 使用更高效的方式清理，避免不必要的递归遍历
        if (detailPanel != null) {
            cleanupStructurePanelsEfficiently(detailPanel);
        }
    }

    /**
     * 高效清理面板中的JsonBodyStructurePanel，避免性能问题
     */
    private void cleanupStructurePanelsEfficiently(Container container) {
        if (container == null) return;

        // 使用WeakHashMap避免强引用，让GC自动清理
        Map<Container, Boolean> visited = new WeakHashMap<>();
        cleanupStructurePanelsWithWeakTracking(container, visited);

        // 主动触发GC清理
        System.gc();
    }

    /**
     * 使用WeakHashMap的清理方法，避免内存泄漏
     */
    private void cleanupStructurePanelsWithWeakTracking(Container container, Map<Container, Boolean> visited) {
        if (container == null || visited.containsKey(container)) {
            return; // 防止循环引用
        }

        // 标记当前容器已访问
        visited.put(container, Boolean.TRUE);

        try {
            // 优先检查当前容器，避免不必要的递归
            if (container instanceof JPanel) {
                JPanel panel = (JPanel) container;
                Object structurePanel = panel.getClientProperty("structurePanel");
                if (structurePanel instanceof JsonBodyStructurePanel) {
                    try {
                        ((JsonBodyStructurePanel) structurePanel).cleanup();
                    } catch (Exception e) {
                        // 使用日志框架记录异常
                        LogUtil.error("清理JsonBodyStructurePanel时发生异常: " + e.getMessage());
                    }
                    // 移除引用
                    panel.putClientProperty("structurePanel", null);
                }
            }

            // 只对必要的容器类型进行递归，减少遍历开销
            if (container instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) container;
                int tabCount = tabbedPane.getTabCount();
                for (int i = 0; i < tabCount; i++) {
                    Component tabComponent = tabbedPane.getComponentAt(i);
                    if (tabComponent instanceof Container && !visited.containsKey(tabComponent)) {
                        cleanupStructurePanelsWithWeakTracking((Container) tabComponent, visited);
                    }
                }
            } else if (container instanceof JSplitPane) {
                // 只处理JSplitPane的左右面板
                JSplitPane splitPane = (JSplitPane) container;
                if (splitPane.getLeftComponent() instanceof Container && !visited.containsKey(splitPane.getLeftComponent())) {
                    cleanupStructurePanelsWithWeakTracking((Container) splitPane.getLeftComponent(), visited);
                }
                if (splitPane.getRightComponent() instanceof Container && !visited.containsKey(splitPane.getRightComponent())) {
                    cleanupStructurePanelsWithWeakTracking((Container) splitPane.getRightComponent(), visited);
                }
            } else {
                // 对于其他容器，只检查直接子组件，避免深度递归
                Component[] components = container.getComponents();
                for (Component component : components) {
                    if (component instanceof Container && !visited.containsKey(component)) {
                        // 限制递归深度，避免性能问题
                        if (visited.size() < 100) { // 设置合理的递归深度限制
                            cleanupStructurePanelsWithWeakTracking((Container) component, visited);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // 记录异常但不中断清理过程
            LogUtil.error("清理组件时发生异常: " + e.getMessage());
        }
    }

    /**
     * 构建主Tab面板，包括请求、响应定义、接口说明、预览文档。
     *
     * @param apiInfo 接口信息
     * @return 主Tab面板
     */
    private JTabbedPane buildMainTab(ApiInfo apiInfo) {
        JTabbedPane mainTab = new JTabbedPane();
        mainTab.addTab(RequestManBundle.message("tab.request"), buildRequestPanel(apiInfo));
        mainTab.addTab(RequestManBundle.message("tab.responseDef"), buildResponsePanel(apiInfo));
        mainTab.addTab(RequestManBundle.message("tab.doc"), buildDocPanel(apiInfo));
        mainTab.addTab(RequestManBundle.message("tab.preview"), buildPreviewPanel(apiInfo));
//
//        // 在Tab右侧添加还原按钮
//        addRestoreButtonToMainTab(mainTab);

        return mainTab;
    }


    /**
     * 创建还原按钮面板
     *
     * @return 还原按钮面板
     */
    private JPanel createRestoreButtonPanel(JTabbedPane mainTab) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)) {
            @Override
            public boolean contains(int x, int y) {
                // 只在按钮区域内返回 true，其余地方让事件透传给 mainTab
                for (Component comp : getComponents()) {
                    Point p = SwingUtilities.convertPoint(this, x, y, comp);
                    if (comp.contains(p)) {
                        return true; // 在按钮区域，拦截
                    }
                }
                return false; // 其他区域透传
            }
        };

        // 叠加容器由绝对布局放置，本层仅需透明
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(null);

        // 创建还原按钮
        JButton restoreButton = new JButton(
                com.intellij.openapi.util.IconLoader.getIcon("/icons/rollback.svg", RequestManPanel.class)
        );
        restoreButton.setToolTipText(RequestManBundle.message("restore.tooltip"));
        restoreButton.setPreferredSize(new Dimension(30, 30));
        restoreButton.setFocusPainted(false);
        restoreButton.setBorderPainted(true);
        restoreButton.setDefaultCapable(false);              // 禁止成为默认按钮

        restoreButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                // 把焦点给 tab（或任意父容器）
                if (mainTab.isShowing()) {
                    mainTab.requestFocusInWindow();
                }
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            });
            restoreScanningApiToOriginal();
        });

        buttonPanel.add(restoreButton);
        return buttonPanel;
    }

    /**
     * 将覆盖在顶部的按钮面板放置到 Tab 标题行的右上角。
     * 绝对定位，避免 OverlayLayout 的居中行为。
     */
    private void updateRestoreButtonBounds(JTabbedPane mainTab, JPanel buttonPanel, JLayeredPane layeredPane) {
        if (mainTab == null || buttonPanel == null || layeredPane == null) {
            return;
        }

        // 期望的按钮尺寸
        Dimension pref = buttonPanel.getPreferredSize();
        if (pref == null) {
            pref = new Dimension(32, 24);
        }
        int buttonWidth = Math.max(24, pref.width);
        int buttonHeight = Math.max(20, pref.height);

        // 读取第一个 Tab 的区域，获取标题栏的 y 和高度
        Rectangle firstTabBounds = null;
        try {
            if (mainTab.getTabCount() > 0) {
                firstTabBounds = mainTab.getUI().getTabBounds(mainTab, 0);
            }
        } catch (Exception ignored) {
        }

        int headerY = 0;
        int headerH;
        if (firstTabBounds != null) {
            headerY = firstTabBounds.y;
            headerH = firstTabBounds.height;
        } else {
            headerH = Math.max(24, mainTab.getFontMetrics(mainTab.getFont()).getHeight() + 12);
        }

        // 右侧 12px 内边距
        int rightPadding = 12;
        int x = Math.max(0, layeredPane.getWidth() - buttonWidth - rightPadding);

        // 垂直居中到标题栏
        int y = headerY + Math.max(0, (headerH - buttonHeight) / 2);

        buttonPanel.setBounds(x, y, buttonWidth, headerH);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    /**
     * 深拷贝 ApiInfo（仅数据结构复制，不含任何解析/扫描行为）。
     */
    private static ApiInfo deepCopyApiInfo(ApiInfo src) {
        if (src == null) {
            return null;
        }
        java.util.List<ApiParam> params = cloneParams(src.getParams());
        java.util.List<ApiParam> bodyParams = cloneParams(src.getBodyParams());
        java.util.List<String> paramTypes = src.getParamTypes() == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(src.getParamTypes());
        java.util.List<ApiParam> responseParams = cloneParams(src.getResponseParams());

        ApiInfo copy = new ApiInfo(
                src.getName(), src.getMethodName(), src.getUrl(), src.getHttpMethod(),
                params, bodyParams, paramTypes, src.getDescription(), responseParams, src.getClassName()
        );
        // 直拷字段/列表
        copy.setHeaders(src.getHeaders() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(src.getHeaders()));
        copy.setCookieItems(src.getCookieItems() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(src.getCookieItems()));
        copy.setAuthMode(src.getAuthMode());
        copy.setAuthValue(src.getAuthValue());
        copy.setPostOps(src.getPostOps() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(src.getPostOps()));
        copy.setBodyType(src.getBodyType());
        copy.setBody(src.getBody());
        return copy;
    }

    private static java.util.List<ApiParam> cloneParams(java.util.List<ApiParam> list) {
        if (list == null || list.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        java.util.List<ApiParam> out = new java.util.ArrayList<>(list.size());
        for (ApiParam p : list) {
            ApiParam c = new ApiParam();
            c.setName(p.getName());
            c.setType(p.getType());
            c.setDescription(p.getDescription());
            c.setDataType(p.getDataType());
            c.setRawType(p.getRawType());
            c.setRawCanonicalType(p.getRawCanonicalType());
            c.setValue(p.getValue());
            c.setContentType(p.getContentType());
            c.setRecursive(p.isRecursive());
            c.setChildren(cloneParams(p.getChildren()));
            out.add(c);
        }
        return out;
    }

    /**
     * 还原扫描接口到原始状态
     */
    private void restoreScanningApiToOriginal() {
        if (currentScanningApi == null) {
            return;
        }

        // 显示确认对话框
        int result = JOptionPane.showConfirmDialog(
                this,
                RequestManBundle.message("restore.confirm.message"),
                RequestManBundle.message("restore.confirm.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // 停止所有表格编辑
            if (autoSaveManager != null) {
                autoSaveManager.runWithImmediateTableUpdate(() -> stopAllTableEditing());
            } else {
                stopAllTableEditing();
            }

            // 清除本地缓存
            ApiCacheStorage.clearCustomEdit(currentScanningApi, project);

            // 基于首次扫描基线还原
            String key = StorageUtil.safeFileName(StorageUtil.buildApiKey(currentScanningApi, project));
            ApiInfo baseline = baselineByKey.get(key);
            ApiInfo originalApi;
            if (baseline != null) {
                originalApi = deepCopyApiInfo(baseline);
            } else {
                // 兜底：使用当前对象的拷贝
                originalApi = new ApiInfo(currentScanningApi);
            }
            // 重新构建界面已在上面处理

            // 标记为已保存状态
            if (autoSaveManager != null) {
                autoSaveManager.markAsSaved();
                autoSaveManager.setCurrentScanningApi(originalApi); // 关键：让后续编辑仍绑定到当前扫描接口
            }
            showApiDetail(originalApi);
            // 更新UI状态
            updateUIState();

            // 显示成功提示
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        RequestManBundle.message("restore.done.message"),
                        RequestManBundle.message("restore.done.title"),
                        JOptionPane.INFORMATION_MESSAGE
                );
            });

        } catch (Exception ex) {
            LogUtil.error("还原接口数据时发生错误: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
                    this,
                    RequestManBundle.message("restore.fail.message") + ex.getMessage(),
                    RequestManBundle.message("restore.fail.title"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 构建请求Tab，包括请求行和参数Tab。
     *
     * @param apiInfo 接口信息
     * @return 请求Tab面板
     */
    private JPanel buildRequestPanel(ApiInfo apiInfo) {
        JPanel requestPanel = new JPanel(new BorderLayout());
        JPanel requestLine = buildRequestLine(apiInfo);
        JTabbedPane paramTab = buildParamTab(apiInfo);
        requestPanel.add(requestLine, BorderLayout.NORTH);
        requestPanel.add(paramTab, BorderLayout.CENTER);
        return requestPanel;
    }

    /**
     * 构建请求行（请求类型、URL、发送、保存按钮）。
     *
     * @param apiInfo 接口信息
     * @return 请求行面板
     */
    private JPanel buildRequestLine(ApiInfo apiInfo) {
        JLabel methodLabel = new JLabel(apiInfo.getHttpMethod());
        methodLabel.setForeground(new java.awt.Color(220, 53, 69));
        JTextField urlField = new JTextField(apiInfo.getUrl());
        urlField.setEditable(false);
        urlField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        JPanel splitSendPanel = buildSplitSendButton(
                btn -> doSendScanRequest(btn, apiInfo),
                btn -> doSendAndDownloadScan(btn, apiInfo)
        );
        scanSaveButton = new JButton(RequestManBundle.message("common.save"));
        scanSaveButton.addActionListener(e -> {
            // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
            stopAllTableEditing();
            ApiCacheStorage.saveCustomEdit(apiInfo, project, this, autoSaveManager);
            autoSaveManager.markAsSaved();
            updateUIState();
        });

        RequestPanelsBuilder.RequestLineComponents c = new RequestPanelsBuilder.RequestLineComponents();
        c.methodLabel = methodLabel;
        c.urlField = urlField;
        c.splitSendPanel = splitSendPanel;
        c.scanSaveButton = scanSaveButton;
        return RequestPanelsBuilder.assembleRequestLine(c);
    }

    /**
     * 构建参数Tab，包括Params、Body、Headers、Cookies、Auth、前置操作、后置操作。
     *
     * @param apiInfo 接口信息
     * @return 参数Tab
     */
    private JTabbedPane buildParamTab(ApiInfo apiInfo) {
        // Params 持久化支持
        paramsPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.PARAMS, apiInfo.getParams(), true);
        // Body 持久化支持
        bodyPanel = new BodyPanel(apiInfo);
        // Headers 持久化支持
        headersPanel = new HeadersPanel();
        headersPanel.setHeadersData(apiInfo.getHeaders());
        // Cookies 持久化支持
        cookiesPanel = new CookiesPanel();
        cookiesPanel.setCookiesData(apiInfo.getCookieItems());
        // Auth 持久化支持
        authPanel = new AuthPanel();
        authPanel.setAuthMode(apiInfo.getAuthMode());
        authPanel.setAuthValue(apiInfo.getAuthValue());
        // preOp 持久化支持
        preOpPanel = new PreOpPanel();
        // PostOp 持久化支持
        postOpPanel = new PostOpPanel();
        postOpPanel.setPostOpData(apiInfo.getPostOps());
        RequestPanelsBuilder.ParamTabsComponents pc = new RequestPanelsBuilder.ParamTabsComponents();
        pc.paramsPanel = paramsPanel;
        pc.bodyPanel = bodyPanel;
        pc.headersPanel = headersPanel;
        pc.cookiesPanel = cookiesPanel;
        pc.authPanel = authPanel;
        pc.preOpPanel = preOpPanel;
        pc.postOpPanel = postOpPanel;
        JTabbedPane paramTab = RequestPanelsBuilder.assembleParamTabs(pc);

        // 设置响应面板引用，用于JSONPath提取器
        postOpPanel.setResponsePanel(responsePanel);
        // 设置当前接口信息，用于获取响应定义
        postOpPanel.setCurrentApiInfo(apiInfo);
        paramTab.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        // 为paramTab添加ChangeListener，在切换tab之前先停止所有表格编辑
        paramTab.addChangeListener(e -> {
            // 获取切换前的Tab索引
            int previousTabIndex = currentTabIndex;
            // 获取切换后的Tab索引
            int newTabIndex = paramTab.getSelectedIndex();

            // 更新当前Tab索引
            currentTabIndex = newTabIndex;

            // 只停止离开的Tab中的表格编辑
            stopTableEditingForTabIndex(previousTabIndex);

            LogUtil.debug("扫描模式Tab切换: " + previousTabIndex + " -> " + newTabIndex);
        });
        return paramTab;
    }

    /**
     * 构建响应定义Tab，直接显示响应结构树。
     *
     * @param apiInfo 当前接口信息
     * @return 响应定义面板
     */
    private JPanel buildResponsePanel(ApiInfo apiInfo) {
        return RequestViewBuilders.buildResponseDefinitionPanel(apiInfo);
    }

    /**
     * 构建接口说明Tab。
     *
     * @param apiInfo 接口信息
     * @return 接口说明面板
     */
    private JPanel buildDocPanel(ApiInfo apiInfo) {
        return RequestViewBuilders.buildDocPanel(apiInfo);
    }


    /**
     * 构建预览文档Tab，集成PreviewDocPanel。
     *
     * @param apiInfo 接口信息
     * @return 预览文档面板
     */
    private JPanel buildPreviewPanel(ApiInfo apiInfo) {
        return RequestViewBuilders.buildPreviewPanel(apiInfo);
    }


    /**
     * 根据Path参数更新URL
     */
    private void updateUrlFromPathParams() {
        if (customParamsPanel == null || customUrlField == null) {
            return;
        }

        List<ApiParam> params = customParamsPanel.getParams();
        if (params.isEmpty()) {
            return;
        }

        // 获取基础URL（不包含Path参数的部分）
        String baseUrl = getBaseUrlWithoutPathParams();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseUrl);

        // 重新拼接所有Path参数
        for (ApiParam param : params) {
            if (!"Path".equals(param.getType())) {
                continue;
            }
            if (StringUtils.isAnyBlank(param.getName(), param.getValue())) {
                continue;
            }
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                urlBuilder.append("/{").append(param.getName()).append("}");
            }
        }

        // 更新URL字段
        customUrlField.setText(urlBuilder.toString());
    }

    /**
     * 获取不包含Path参数的基础URL
     */
    private String getBaseUrlWithoutPathParams() {
        String currentUrl = customUrlField.getText();
        if (currentUrl == null || currentUrl.isEmpty()) {
            return "";
        }

        // 找到第一个Path参数的位置，截取前面的部分作为基础URL
        int pathStartIndex = currentUrl.indexOf("/{");
        if (pathStartIndex > 0) {
            return currentUrl.substring(0, pathStartIndex);
        }

        return currentUrl;
    }


    // 重写toString，保证下拉框显示友好
    static {
        javax.swing.UIManager.put("ComboBox.rendererUseListColors", Boolean.TRUE);
    }

    /**
     * 切换到自定义接口模式，彻底清空并重建布局，避免页面错乱，每次都新建customPanel
     */
    private void switchToCustomMode() {
        // 检查是否有未保存的更改
        if (!checkUnsavedChanges()) {
            return; // 用户取消，不切换模式
        }

        // 设置模式
        customMode = true;
        if (autoSaveManager != null) {
            autoSaveManager.setCurrentEditingApi(null);
        }

        // 彻底移除所有子组件，防止残留
        this.removeAll();
        JPanel topPanel = buildTopPanel();
        // 每次都新建，避免引用丢失
        customPanel = buildCustomPanel();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText(RequestManBundle.message("main.response.placeholder"));
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        this.add(topPanel, BorderLayout.NORTH);
        this.add(customPanel, BorderLayout.CENTER);
        this.add(responsePanel, BorderLayout.SOUTH);
        this.revalidate();
        this.repaint();
        loadCustomApiList();
    }

    /**
     * 切换到自动扫描模式，彻底清空并重建布局，避免页面错乱
     */
    private void switchToScanMode() {
        // 检查是否有未保存的更改
        if (!checkUnsavedChanges()) {
            return; // 用户取消，不切换模式
        }

        // 设置模式
        customMode = false;
        if (autoSaveManager != null) {
            autoSaveManager.setCurrentScanningApi(null);
        }

        // 彻底移除所有子组件，防止残留
        this.removeAll();
        JPanel topPanel = buildTopPanel();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText(RequestManBundle.message("main.response.placeholder"));
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        this.add(topPanel, BorderLayout.NORTH);
        this.add(detailPanel, BorderLayout.CENTER);
        this.add(responsePanel, BorderLayout.SOUTH);
        this.revalidate();
        this.repaint();
        refreshApiList();
    }

    /**
     * 构建自定义接口分栏面板
     *
     * @return JSplitPane
     */
    private JSplitPane buildCustomPanel() {
        // 主分栏
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150); // 设置分隔线初始位置，更窄
        splitPane.setResizeWeight(0.0); // 左侧固定宽度
        splitPane.setMinimumSize(new Dimension(600, 400));
        // 左侧列表
        customApiListModel = new DefaultListModel<>();
        customApiList = new JList<>(customApiListModel);
        customApiList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 启用拖拽功能
        customApiList.setDragEnabled(true);
        customApiList.setDropMode(DropMode.INSERT);

        // 设置拖拽提示
        customApiList.setToolTipText(RequestManBundle.message("custom.drag.tip"));

        // 分割线渲染：记录插入位置并设置自定义渲染器
        dropLineIndex = -1;
        dropInsert = true;
        customApiList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                int top = 0, bottom = 0;
                // 顶部分割线：插入到当前单元格前
                if (dropLineIndex >= 0 && dropInsert && index == dropLineIndex) {
                    top = 2;
                }
                // 末尾插入：在最后一项下方画线
                if (dropLineIndex == customApiListModel.getSize() && index == customApiListModel.getSize() - 1) {
                    bottom = 2;
                }
                if (top > 0 || bottom > 0) {
                    c.setBorder(BorderFactory.createMatteBorder(top, 0, bottom, 0, new Color(0, 120, 215)));
                } else {
                    c.setBorder(null);
                }
                return c;
            }
        });

        // 添加拖拽监听器
        setupDragAndDropSupport();

        customApiList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                CustomApiInfo selected = customApiList.getSelectedValue();
                // 检查是否有未保存的更改
                if (!checkUnsavedChanges()) {
                    // 用户取消，恢复之前的选择
                    customApiList.setSelectedValue(editingApi, true);
                    return;
                }
                showCustomApiDetail(selected);
            }
        });

        // 添加右键菜单
        CustomApiContextMenuManager.setupCustomApiListContextMenu(this, project);

        JScrollPane listScroll = new JScrollPane(customApiList);
        listScroll.setMinimumSize(new Dimension(180, 300));
        listScroll.setPreferredSize(new Dimension(220, 400));


        splitPane.setLeftComponent(listScroll);
        // 右侧编辑面板
        customEditPanel = new JPanel();
        customEditPanel.setLayout(new BoxLayout(customEditPanel, BoxLayout.Y_AXIS));
        customNameField = new JTextField();
        customNameField.setMaximumSize(new Dimension(400, 28));
        customUrlField = new JTextField();
        customUrlField.setMaximumSize(new Dimension(400, 28));
        customMethodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"});
        customMethodBox.setMaximumSize(new Dimension(400, 28));
        customParamsPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.PARAMS, new ArrayList<>(), false, this::updateUrlFromPathParams);
        customPostOpPanel = new PostOpPanel();
        customBodyPanel = new EditableBodyPanel(new ArrayList<>(), true);
        saveCustomBtn = new JButton(RequestManBundle.message("common.save"));
        deleteCustomBtn = new JButton(RequestManBundle.message("common.delete"));
        // 组装右侧
        customEditPanel.removeAll();
        customEditPanel.setLayout(new BoxLayout(customEditPanel, BoxLayout.Y_AXIS));
        // 顶部一行（仅UI拼装）
        JLabel nameLabel = new JLabel(RequestManBundle.message("custom.name.label") + ":");
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        customNameField = new JTextField();
        customNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customNameField.setPreferredSize(new Dimension(0, 28));
        customNameField.setAlignmentY(Component.CENTER_ALIGNMENT);
        customNameStarLabel = new JLabel(" ");
        customNameStarLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        Dimension starSize = new Dimension(12, 28);
        customNameStarLabel.setPreferredSize(starSize);
        customNameStarLabel.setMaximumSize(starSize);
        customNameStarLabel.setMinimumSize(starSize);
        CustomEditPanelsBuilder.TopRowComponents trc = new CustomEditPanelsBuilder.TopRowComponents();
        trc.nameLabel = nameLabel;
        trc.nameField = customNameField;
        trc.nameStarLabel = null; // 初始创建面板此处无需星号
        trc.extraComponent = customParamsPanel;
        customEditPanel.add(CustomEditPanelsBuilder.assembleTopRow(trc));
        // URL/METHOD（仅UI拼装）
        JLabel urlLabel = new JLabel("URL:");
        customUrlField = new JTextField();
        JLabel methodLabel = new JLabel("HTTP方法:");
        customMethodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"});
        CustomEditPanelsBuilder.assembleUrlAndMethod(customEditPanel, urlLabel, customUrlField, methodLabel, customMethodBox);
        // Tab区
        JTabbedPane tabbedPane = buildCustomEditTabs();
        tabbedPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        customEditPanel.add(tabbedPane);
        // 按钮区（仅UI拼装）
        customEditPanel.add(CustomEditPanelsBuilder.assembleButtonRow(saveCustomBtn, deleteCustomBtn));
        splitPane.setRightComponent(customEditPanel);
        // 事件绑定 - 使用标志位避免重复注册
        if (saveCustomBtn.getActionListeners().length == 0) {
            saveCustomBtn.addActionListener(e -> saveOrUpdateCustomApi());
        }
        if (deleteCustomBtn.getActionListeners().length == 0) {
            deleteCustomBtn.addActionListener(e -> CustomApiContextMenuManager.deleteSelectedCustomApi(this, project));
        }
        return splitPane;
    }

    /**
     * 显示自定义接口详情到右侧编辑面板
     */
    public void showCustomApiDetail(CustomApiInfo api) {
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText(RequestManBundle.message("main.response.placeholder"));
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        customEditPanel.removeAll();
        customEditPanel.setLayout(new BoxLayout(customEditPanel, BoxLayout.Y_AXIS));
        // 顶部一行（仅UI拼装）
        customNameLabel = new JLabel(CUSTOM_NAME_LABEL_TEXT);
        customNameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        customNameField = new JTextField();
        customNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customNameField.setPreferredSize(new Dimension(0, 28));
        customNameField.setAlignmentY(Component.CENTER_ALIGNMENT);
        JPanel splitSendPanel = buildSplitSendButton(
                btn -> doSendCustomRequest(btn),
                btn -> doSendAndDownloadCustom(btn)
        );
        splitSendPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        CustomEditPanelsBuilder.TopRowComponents trc = new CustomEditPanelsBuilder.TopRowComponents();
        trc.nameLabel = customNameLabel;
        trc.nameField = customNameField;
        trc.nameStarLabel = customNameStarLabel;
        trc.extraComponent = splitSendPanel;
        customEditPanel.add(CustomEditPanelsBuilder.assembleTopRow(trc));
        // URL区
        JLabel urlLabel = new JLabel("URL:");
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        customEditPanel.add(urlLabel);
        customUrlField = new JTextField();
        customUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        customEditPanel.add(customUrlField);
        // HTTP方法区
        JLabel methodLabel = new JLabel("HTTP方法:");
        methodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        customEditPanel.add(methodLabel);
        customMethodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"});
        customMethodBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customMethodBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        customEditPanel.add(customMethodBox);
        // Tab区
        JTabbedPane tabbedPane = buildCustomEditTabs();
        tabbedPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        customEditPanel.add(tabbedPane);
        // 按钮区
        JPanel btnPanel = new JPanel();
        saveCustomBtn = new JButton(RequestManBundle.message("common.save"));
        deleteCustomBtn = new JButton(RequestManBundle.message("common.delete"));
        btnPanel.add(saveCustomBtn);
        btnPanel.add(deleteCustomBtn);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        customEditPanel.add(btnPanel);
        // 回显数据
        if (api == null) {
            editingApi = null;
            // 尝试从缓存恢复数据
            Map<String, Object> cache = CustomApiStorage.loadCustomParamsFromCache(project);
            if (cache.containsKey("customParams")) {
                List<ApiParam> cachedParams = new ArrayList<>();
                Object paramsObj = cache.get("customParams");
                if (paramsObj instanceof List<?> list) {
                    for (Object obj : list) {
                        if (obj instanceof ApiParam) {
                            cachedParams.add((ApiParam) obj);
                        } else if (obj instanceof Map map) {
                            String name = (String) map.get("name");
                            String value = (String) map.get("value");
                            String type = (String) map.get("type");
                            String description = (String) map.get("description");
                            ApiParam param = new ApiParam();
                            param.setName(name);
                            param.setValue(value);
                            param.setType(type);
                            param.setDescription(description);
                            cachedParams.add(param);
                        }
                    }
                }
                if (!cachedParams.isEmpty()) {
                    customParamsPanel.setParams(cachedParams);
                }
            }
        } else {
            isInitializing = true;
            customNameField.setText(api.getName());
            customUrlField.setText(api.getUrl());
            customMethodBox.setSelectedItem(api.getHttpMethod());
            isInitializing = false;
            if (customParamsPanel != null) {
                isInitializing = true;
                customParamsPanel.setParams(api.getParams() != null ? api.getParams() : new ArrayList<>());
                isInitializing = false;
            }
            if (customPostOpPanel != null && api.getPostOps() != null) {
                isInitializing = true;
                customPostOpPanel.setPostOpData(api.getPostOps());
                isInitializing = false;
            }
            if (customBodyPanel != null) {
                String bodyType = api.getBodyType() != null ? api.getBodyType() : "none";
                isInitializing = true;
                customBodyPanel.setBodyType(bodyType);
                if ("json".equals(bodyType)) {
                    customBodyPanel.setJsonBody(api.getBody());
                } else if ("form-data".equals(bodyType)) {
                    customBodyPanel.setFormDataParams(api.getBodyParams());
                } else if ("x-www-form-urlencoded".equals(bodyType)) {
                    customBodyPanel.setUrlencodedParams(api.getBodyParams());
                } else if ("xml".equals(bodyType)) {
                    customBodyPanel.setXmlBody(api.getBody());
                } else if ("binary".equals(bodyType)) {
                    customBodyPanel.setFilePathFromBinaryText(api.getBody());
                }
                isInitializing = false;
            }
            // 恢复headers
            if (customHeadersPanel != null) {
                isInitializing = true;
                customHeadersPanel.setHeadersData(api.getHeaders() != null ? api.getHeaders() : new ArrayList<>());
                isInitializing = false;
            }
            // 恢复cookie
            if (customCookiesPanel != null) {
                isInitializing = true;
                customCookiesPanel.setCookiesData(api.getCookieItems() != null ? api.getCookieItems() : new ArrayList<>());
                isInitializing = false;
            }
            // 恢复认证信息
            if (customAuthPanel != null) {
                isInitializing = true;
                customAuthPanel.setAuthMode(api.getAuthMode());
                customAuthPanel.setAuthValue(api.getAuthValue());
                isInitializing = false;
            }
            editingApi = api;
        }
        // 重新绑定按钮事件 - 使用标志位避免重复注册
        if (saveCustomBtn.getActionListeners().length == 0) {
            saveCustomBtn.addActionListener(e -> saveOrUpdateCustomApi());
        }
        if (deleteCustomBtn.getActionListeners().length == 0) {
            deleteCustomBtn.addActionListener(e -> CustomApiContextMenuManager.deleteSelectedCustomApi(this, project));
        }

        // 设置自动保存管理器的当前编辑接口
        autoSaveManager.setCurrentEditingApi(editingApi);

        // 添加自动保存监听器
        setupAutoSaveListeners();

        // 初始化状态
        updateSaveButtonState();
        updateApiNameDisplay();

        customEditPanel.revalidate();
        customEditPanel.repaint();
    }

    /**
     * 自动保存自定义接口（静默保存）
     */
    private void autoSaveCustomApi(CustomApiInfo api) {
        if (api == null) {
            return;
        }

        try {
            // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
            stopAllTableEditing();

            // 获取当前编辑的内容
            String name = customNameField.getText().trim();
            String url = customUrlField.getText().trim();
            String method = (String) customMethodBox.getSelectedItem();

            if (name.isEmpty() || url.isEmpty() || method == null || method.isEmpty()) {
                return; // 基本信息不完整，不进行自动保存
            }

            // 获取请求体内容
            String body = "";
            String bodyType = customBodyPanel != null ? customBodyPanel.getBodyType() : "none";
            List<ApiParam> bodyParams = new ArrayList<>();
            if (customBodyPanel != null) {
                if ("none".equals(bodyType)) {
                    body = "";
                } else if ("json".equals(bodyType)) {
                    body = customBodyPanel.getJsonBodyText();
                } else if ("form-data".equals(bodyType)) {
                    bodyParams = customBodyPanel.getBodyParams();
                } else if ("x-www-form-urlencoded".equals(bodyType)) {
                    bodyParams = customBodyPanel.getBodyParams();
                } else if ("xml".equals(bodyType)) {
                    body = customBodyPanel.getXmlBodyText();
                } else if ("binary".equals(bodyType)) {
                    body = customBodyPanel.getFilePathFromBinaryText();
                }
            }

            List<ApiParam> params = customParamsPanel != null ? customParamsPanel.getParams() : new ArrayList<>();
            params = params.stream()
                    .filter(p -> p.getName() != null && !p.getName().trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            List<PostOpItem> postOps = customPostOpPanel != null ? customPostOpPanel.getPostOpData() : new ArrayList<>();

            // 更新接口信息
            api.setName(name);
            api.setUrl(url);
            api.setHttpMethod(method);
            api.setParams(params);
            api.setPostOps(postOps);
            api.setBody(body);
            api.setBodyType(bodyType);
            api.setBodyParams(bodyParams);

            // 保存认证信息
            if (customAuthPanel != null) {
                api.setAuthMode(customAuthPanel.getAuthMode());
                api.setAuthValue(customAuthPanel.getAuthValue());
            }

            // 持久化保存
            CustomApiStorage.persistCustomApiList(project, customApiListModel);

            // 标记已保存
            autoSaveManager.markAsSaved();
            updateSaveButtonState();
            updateApiNameDisplay();

        } catch (Exception e) {
            // 自动保存失败时不显示错误提示，避免干扰用户
            LogUtil.error("自动保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 自动保存扫描接口
     */
    private void autoSaveScanningApi(ApiInfo api) {
        if (api == null) {
            return;
        }

        try {
            // 停止所有表格编辑
            stopTableEditingForTabIndex(currentTabIndex);

            // 保存到本地缓存
            ApiCacheStorage.saveCustomEdit(api, project, this, autoSaveManager);

            // 标记为已保存
            autoSaveManager.markAsSaved();
            updateUIState();

            if (headersPanel != null) {
                api.setHeaders(headersPanel.getHeadersData());
            }
            if (authPanel != null) {
                api.setAuthMode(authPanel.getAuthMode());
                api.setAuthValue(authPanel.getAuthValue());
            }
            if (postOpPanel != null) {
                api.setPostOps(postOpPanel.getPostOpData());
            }

        } catch (Exception e) {
            // 自动保存失败时不显示错误提示，避免干扰用户
            LogUtil.error("自动保存扫描接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止所有表格编辑
     */
    private void stopAllTableEditing() {
        TableEditingManager.RequestContext ctx = new TableEditingManager.RequestContext(this);
        TableEditingManager.stopAll(ctx);
    }

    /**
     * 停止指定Tab索引中的表格编辑（精确停止）
     */
    private void stopTableEditingForTabIndex(int tabIndex) {
        TableEditingManager.RequestContext ctx = new TableEditingManager.RequestContext(this);
        TableEditingManager.stopByTabIndex(ctx, tabIndex);
    }

    /**
     * 更新保存按钮状态
     */
    private void updateSaveButtonState() {
        if (saveCustomBtn != null) {
            boolean hasChanges = autoSaveManager.hasUnsavedChanges();
            saveCustomBtn.setEnabled(hasChanges);
        }
        if (scanSaveButton != null) {
            boolean hasChanges = autoSaveManager.hasUnsavedChanges();
            scanSaveButton.setEnabled(hasChanges);
        }
    }

    /**
     * 更新UI状态
     */
    private void updateUIState() {
        updateSaveButtonState();
        updateApiNameDisplay();

        // 检查param字段是否发生变化，如果有Path参数则更新URL
        if (customMode && customParamsPanel != null) {
            // 获取当前参数列表
            List<ApiParam> currentParams = customParamsPanel.getParams();

            // 检查是否有有效的Path参数
            boolean hasPathParams = currentParams.stream()
                    .anyMatch(param -> "Path".equals(param.getType()) &&
                            param.getName() != null && !param.getName().trim().isEmpty() &&
                            param.getValue() != null && !param.getValue().trim().isEmpty());

            // 如果有Path参数，则更新URL
            if (hasPathParams) {
                updateUrlFromPathParams();
            }
        }
    }

    /**
     * 更新接口名称显示（包括*号）
     */
    private void updateApiNameDisplay() {
        // 自定义模式下，仅更新自定义接口名称的星号显示
        if (customMode) {
            if (customNameStarLabel != null) {
                if (autoSaveManager != null && autoSaveManager.hasUnsavedChanges()) {
                    customNameStarLabel.setText("<html><font color='red'>*</font></html>");
                } else {
                    customNameStarLabel.setText(" ");
                }
            }
            return;
        }

        // 扫描模式下，更新详情面板标题星号
        updateDetailPanelTitle();
    }

    /**
     * 根据未保存状态更新详情面板标题星号
     */
    private void updateDetailPanelTitle() {
        if (detailPanel == null) {
            return;
        }
        String baseTitle = RequestManBundle.message("main.api.details");
        boolean showStar = autoSaveManager != null && autoSaveManager.hasUnsavedChanges();
        String title = showStar ? "<html>" + baseTitle + " <font color='red'>*</font></html>" : baseTitle;
        detailPanel.setBorder(BorderFactory.createTitledBorder(title));
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * 设置自动保存监听器
     */
    private void setupAutoSaveListeners() {
        if (autoSaveManager == null) {
            return;
        }

        // 为文本字段添加监听器
        if (customNameField != null) {
            autoSaveManager.addTextChangeListener(customNameField, "name");
        }
        if (customUrlField != null) {
            autoSaveManager.addTextChangeListener(customUrlField, "url");
        }

        // 为下拉框添加监听器
        if (customMethodBox != null) {
            autoSaveManager.addSelectionChangeListener(customMethodBox, "method");
        }

        // 为表格添加监听器
        if (customParamsPanel != null) {
            JTable customParamsPanelTable = SwingUtils.getTable(customParamsPanel);
            if (customParamsPanelTable != null) {
                autoSaveManager.addTableChangeListener(customParamsPanelTable, "param");

                // 注意：param_location_changed字段变化会通过AutoSaveManager的uiUpdateCallback机制触发
                // 在updateUIState方法中会检查是否需要更新URL
            }
        }
        if (customPostOpPanel != null) {
            JTable customPostOpPanelTable = SwingUtils.getTable(customPostOpPanel);
            if (customPostOpPanelTable != null) {
                autoSaveManager.addTableChangeListener(customPostOpPanelTable, "postOp");
            }
        }

        // 为请求体面板添加监听器
        if (customBodyPanel != null) {
            // JSON文本区域
            JTextArea jsonArea = getJsonAreaFromBodyPanel(customBodyPanel);
            if (jsonArea != null) {
                autoSaveManager.addTextChangeListener(jsonArea, "body");
            }

            // XML文本区域
            JTextArea xmlArea = getXmlAreaFromBodyPanel(customBodyPanel);
            if (xmlArea != null) {
                autoSaveManager.addTextChangeListener(xmlArea, "body");
            }

            // form-data表格
            JTable formTable = SwingUtils.getTable(customBodyPanel.getFormDataPanel());
            if (formTable != null) {
                autoSaveManager.addTableChangeListener(formTable, "bodyParams");
            }

            // urlencoded表格
            JTable urlTable = SwingUtils.getTable(customBodyPanel.getUrlencodedPanel());
            if (urlTable != null) {
                autoSaveManager.addTableChangeListener(urlTable, "bodyParams");
            }

            // binary 文件路径文本框
            Object binaryObj = SwingUtils.getObject(customBodyPanel, "binaryPanel");
            if (binaryObj instanceof BinaryBodyPanel) {
                BinaryBodyPanel customBinaryPanel = (BinaryBodyPanel) binaryObj;
                JTextField filePathField = customBinaryPanel.getFilePathField();
                if (filePathField != null) {
                    autoSaveManager.addTextChangeListener(filePathField, "body");
                }
            }
        }
    }

    /**
     * 设置扫描模式自动保存监听器
     */
    private void setupScanningAutoSaveListeners() {
        if (autoSaveManager == null) {
            return;
        }
        // 为参数面板添加监听器
        if (paramsPanel != null) {
            JTable paramsPanelTable = SwingUtils.getTable(paramsPanel);
            if (paramsPanelTable != null) {
                autoSaveManager.addTableChangeListener(paramsPanelTable, "param");
            }
        }
        // 为请求体面板添加监听器
        if (bodyPanel != null) {
            // 类型下拉框
            JComboBox<String> typeComboBox = bodyPanel.getTypeComboBox();
            if (typeComboBox != null && typeComboBox.getSelectedItem() != null) {
                autoSaveManager.addSelectionChangeListener(typeComboBox, "bodyType");
            }
            // JSON文本区域
            JsonBodyPanel jsonPanel = bodyPanel.getJsonBodyPanel();
            if (jsonPanel != null && jsonPanel.getTextAreaRef() != null) {
                autoSaveManager.addTextChangeListener(jsonPanel.getTextAreaRef(), "body");
            }
            // XML文本区域
            XmlBodyPanel xmlPanel = bodyPanel.getXmlBodyPanel();
            if (xmlPanel != null && xmlPanel.getTextAreaRef() != null) {
                autoSaveManager.addTextChangeListener(xmlPanel.getTextAreaRef(), "body");
            }
            // form-data表格
            JTable formTable = getTableFromPanel(bodyPanel, "formDataPanel");
            if (formTable != null) {
                autoSaveManager.addTableChangeListener(formTable, "bodyParams");
            }
            // urlencoded表格
            JTable urlTable = getTableFromPanel(bodyPanel, "urlencodedPanel");
            if (urlTable != null) {
                autoSaveManager.addTableChangeListener(urlTable, "bodyParams");
            }
            // binary 文件路径文本框
            BinaryBodyPanel scanBinaryPanel = bodyPanel.getBinaryPanel();
            if (scanBinaryPanel != null && scanBinaryPanel.getFilePathField() != null) {
                autoSaveManager.addTextChangeListener(scanBinaryPanel.getFilePathField(), "body");
            }
        }
        // 为头部面板添加监听器
        if (headersPanel != null) {
            if (headersPanel.getTable() != null) {
                autoSaveManager.addTableChangeListener(headersPanel.getTable(), "headers");
            }
        }
        // 为Cookie面板添加监听器
        if (cookiesPanel != null) {
            if (cookiesPanel.getTable() != null) {
                autoSaveManager.addTableChangeListener(cookiesPanel.getTable(), "cookie");
            }
        }
        // 为认证面板添加监听器
        if (authPanel != null) {
            JComboBox<?> authModeCombo = getAuthModeComboBox(authPanel);
            if (authModeCombo != null) {
                autoSaveManager.addSelectionChangeListener(authModeCombo, "authMode");
            }
            JTextField authValueField = getAuthValueField(authPanel);
            if (authValueField != null) {
                autoSaveManager.addTextChangeListener(authValueField, "authValue");
            }
        }
        // 为后置操作面板添加监听器
        if (postOpPanel != null) {
            JTable postOpPanelTable = SwingUtils.getTable(postOpPanel);
            if (postOpPanelTable != null) {
                autoSaveManager.addTableChangeListener(postOpPanelTable, "postOp");
            }
        }
    }

    /**
     * 检查是否有未保存的更改并提示用户
     */
    public boolean checkUnsavedChanges() {
        if (autoSaveManager != null && autoSaveManager.hasUnsavedChanges()) {
            // 如果启用自动保存，直接保存，不弹窗
            if (autoSaveManager.isAutoSaveEnabled()) {
                // 直接走保存逻辑
                if (customMode) {
                    saveOrUpdateCustomApi();
                } else if (currentScanningApi != null) {
                    // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
                    stopAllTableEditing();
                    ApiCacheStorage.saveCustomEdit(currentScanningApi, project, this, autoSaveManager);
                    // 标记为已保存
                    autoSaveManager.markAsSaved();
                    updateUIState();
                }
                return true;
            }

            // 如果未启用自动保存，弹窗询问用户
            int result = JOptionPane.showConfirmDialog(
                    this,
                    RequestManBundle.message("unsaved.confirm.message"),
                    RequestManBundle.message("main.tip"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                // 用户选择保存
                if (customMode) {
                    saveOrUpdateCustomApi();
                } else if (currentScanningApi != null) {
                    // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
                    stopAllTableEditing();
                    ApiCacheStorage.saveCustomEdit(currentScanningApi, project, this, autoSaveManager);
                    // 标记为已保存
                    autoSaveManager.markAsSaved();
                    updateUIState();
                }
                return true;
            } else if (result == JOptionPane.NO_OPTION) {
                // 用户选择不保存，直接离开
                autoSaveManager.markAsSaved(); // 清除未保存状态
                updateDetailPanelTitle();
                return true;
            } else {
                // 用户选择取消，不离开
                return false;
            }
        }
        return true; // 没有未保存的更改，可以离开
    }

    /**
     * 保存或更新自定义接口
     */
    private void saveOrUpdateCustomApi() {
        // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        stopAllTableEditing();
        String name = customNameField.getText().trim();
        String url = customUrlField.getText().trim();
        String method = (String) customMethodBox.getSelectedItem();
        // 保存时根据当前类型获取内容
        String body = "";
        String bodyType = customBodyPanel != null ? customBodyPanel.getBodyType() : "none";
        List<ApiParam> bodyParams = new ArrayList<>();
        if (customBodyPanel != null) {
            if ("none".equals(bodyType)) {
                body = "";
            } else if ("json".equals(bodyType)) {
                body = customBodyPanel.getJsonBodyText();
            } else if ("form-data".equals(bodyType)) {
                bodyParams = customBodyPanel.getBodyParams();
            } else if ("x-www-form-urlencoded".equals(bodyType)) {
                bodyParams = customBodyPanel.getBodyParams();
            } else if ("xml".equals(bodyType)) {
                body = customBodyPanel.getXmlBodyText();
            } else if ("binary".equals(bodyType)) {
                body = customBodyPanel.getFilePathFromBinaryText();
            }
        }
        List<ApiParam> params = customParamsPanel != null ? customParamsPanel.getParams() : new ArrayList<>();
        // 过滤掉参数名为空的行，防止空行被保存
        params = params.stream()
                .filter(p -> p.getName() != null && !p.getName().trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
        java.util.List<PostOpItem> postOps = customPostOpPanel != null ? customPostOpPanel.getPostOpData() : new java.util.ArrayList<>();
        if (name.isEmpty() || url.isEmpty() || method == null || method.isEmpty()) {
            JOptionPane.showMessageDialog(this, RequestManBundle.message("custom.required"), RequestManBundle.message("main.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (editingApi == null) {
            // 新增
            CustomApiInfo api = new CustomApiInfo(name, url, method, params, body, null, postOps);
            api.setBodyType(bodyType);
            api.setBodyParams(bodyParams);
            // 保存认证信息
            if (customAuthPanel != null) {
                api.setAuthMode(customAuthPanel.getAuthMode());
                api.setAuthValue(customAuthPanel.getAuthValue());
            }
            // 保存headers
            if (customHeadersPanel != null) {
                api.setHeaders(customHeadersPanel.getHeadersData());
            }
            // 保存cookice
            if (customCookiesPanel != null) {
                api.setCookieItems(customCookiesPanel.getCookiesData());
            }
            customApiListModel.addElement(api);
        } else {
            // 编辑
            editingApi.setName(name);
            editingApi.setUrl(url);
            editingApi.setHttpMethod(method);
            editingApi.setParams(params);
            editingApi.setPostOps(postOps);
            editingApi.setBody(body);
            editingApi.setBodyType(bodyType);
            editingApi.setBodyParams(bodyParams);
            // 保存认证信息
            if (customAuthPanel != null) {
                editingApi.setAuthMode(customAuthPanel.getAuthMode());
                editingApi.setAuthValue(customAuthPanel.getAuthValue());
            }
            // 保存headers
            if (customHeadersPanel != null) {
                editingApi.setHeaders(customHeadersPanel.getHeadersData());
            }
            // 保存cookice
            if (customCookiesPanel != null) {
                editingApi.setCookieItems(customCookiesPanel.getCookiesData());
            }
            customApiList.repaint();
        }
        CustomApiStorage.persistCustomApiList(project, customApiListModel);

        // 保存自定义参数到缓存
        CustomApiStorage.saveCustomParamsToCache(customParamsPanel, project);

        // 标记已保存并更新状态
        autoSaveManager.markAsSaved();
        updateSaveButtonState();
        updateApiNameDisplay();
        if (!autoSaveManager.isAutoSaveEnabled()) {
            JOptionPane.showMessageDialog(this, RequestManBundle.message("common.save.success"), RequestManBundle.message("main.tip"), JOptionPane.INFORMATION_MESSAGE);
        }
    }


    /**
     * 加载自定义接口列表
     */
    private void loadCustomApiList() {
        customApiListModel.clear();
        for (CustomApiInfo api : CustomApiStorage.loadCustomApis(project)) {
            customApiListModel.addElement(api);
        }
        if (!customApiListModel.isEmpty()) {
            customApiList.setSelectedIndex(0); // 自动触发回显
        } else {
            showCustomApiDetail(null);
        }
    }

    // 响应格式化相关方法已移至DefaultResponseHandler中
    // 此处不再需要重复实现

    // --- 保证showCustomApiDetail能正常调用Tab构建方法 ---
    private JTabbedPane buildCustomEditTabs() {
        // 创建各面板
        customParamsPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.PARAMS, new ArrayList<>(), false, this::updateUrlFromPathParams);
        customBodyPanel = new EditableBodyPanel(new ArrayList<>(), true);
        customHeadersPanel = new HeadersPanel();
        customCookiesPanel = new CookiesPanel();
        customAuthPanel = new AuthPanel();
        customPreOpPanel = new PreOpPanel();
        customPostOpPanel = new PostOpPanel();

        // 仅UI拼装交由Builder，业务与监听仍在本方法中
        CustomEditPanelsBuilder.CustomTabsComponents ctc = new CustomEditPanelsBuilder.CustomTabsComponents(this);
        JTabbedPane tabbedPane = CustomEditPanelsBuilder.assembleCustomTabs(ctc);
        // 为Cookie面板添加监听器
        if (customCookiesPanel != null) {
            if (customCookiesPanel.getTable() != null) {
                autoSaveManager.addTableChangeListener(customCookiesPanel.getTable(), "cookie");
            }
        }
        // 为认证面板添加监听器
        if (customAuthPanel != null) {
            JComboBox<?> authModeCombo = getAuthModeComboBox(customAuthPanel);
            if (authModeCombo != null) {
                autoSaveManager.addSelectionChangeListener(authModeCombo, "authMode");
            }
            JTextField authValueField = getAuthValueField(customAuthPanel);
            if (authValueField != null) {
                autoSaveManager.addTextChangeListener(authValueField, "authValue");
            }
        }
        // 为请求体面板添加监听器
        if (customBodyPanel != null) {
            // 类型下拉框
            JComboBox<String> typeComboBox = customBodyPanel.getTypeComboBox();
            if (typeComboBox != null && typeComboBox.getSelectedItem() != null) {
                autoSaveManager.addSelectionChangeListener(typeComboBox, "bodyType");
            }

            // 对于EditableBodyPanel，直接获取文本区域
            if (customBodyPanel instanceof EditableBodyPanel) {
                EditableBodyPanel editablePanel = (EditableBodyPanel) customBodyPanel;
                // JSON文本区域
                JTextArea jsonArea = editablePanel.getJsonTextArea();
                if (jsonArea != null) {
                    autoSaveManager.addTextChangeListener(jsonArea, "body");
                }
                // XML文本区域
                JTextArea xmlArea = editablePanel.getXmlTextArea();
                if (xmlArea != null) {
                    autoSaveManager.addTextChangeListener(xmlArea, "body");
                }
            } else {
                // 对于BodyPanel，使用原有的兼容性方法
                JsonBodyPanel jsonPanel = customBodyPanel.getJsonBodyPanel();
                if (jsonPanel != null && jsonPanel.getTextAreaRef() != null) {
                    autoSaveManager.addTextChangeListener(jsonPanel.getTextAreaRef(), "body");
                }
                XmlBodyPanel xmlPanel = customBodyPanel.getXmlBodyPanel();
                if (xmlPanel != null && xmlPanel.getTextAreaRef() != null) {
                    autoSaveManager.addTextChangeListener(xmlPanel.getTextAreaRef(), "body");
                }
            }

            // form-data表格
            JTable formTable = getTableFromPanel(customBodyPanel, "formDataPanel");
            if (formTable != null) {
                autoSaveManager.addTableChangeListener(formTable, "bodyParams");
            }
            // urlencoded表格
            JTable urlTable = getTableFromPanel(customBodyPanel, "urlencodedPanel");
            if (urlTable != null) {
                autoSaveManager.addTableChangeListener(urlTable, "bodyParams");
            }
        }
        // 为前置操作面板添加监听器
        if (customPreOpPanel != null) {
            JTable preOpPanelTable = SwingUtils.getTable(customPreOpPanel);
            if (preOpPanelTable != null) {
                autoSaveManager.addTableChangeListener(preOpPanelTable, "preOp");
            }
        }
        // 为头部面板添加监听器
        if (customHeadersPanel != null) {
            if (customHeadersPanel.getTable() != null) {
                autoSaveManager.addTableChangeListener(customHeadersPanel.getTable(), "headers");
            }
        }
        // 设置响应面板引用，用于JSONPath提取器
        customPostOpPanel.setResponsePanel(responsePanel);
        // 自定义模式没有接口信息，使用默认响应定义

        // 为tabbedPane添加ChangeListener，在切换tab之前先停止所有表格编辑
        tabbedPane.addChangeListener(e -> {
            // 获取切换前的Tab索引
            int previousTabIndex = currentTabIndex;
            // 获取切换后的Tab索引
            int newTabIndex = tabbedPane.getSelectedIndex();

            // 更新当前Tab索引
            currentTabIndex = newTabIndex;

            // 只停止离开的Tab中的表格编辑
            stopTableEditingForTabIndex(previousTabIndex);

            LogUtil.debug("自定义模式Tab切换: " + previousTabIndex + " -> " + newTabIndex);
        });

        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        return tabbedPane;
    }

    // --- 分体式发送按钮工具方法，返回包含主按钮和下拉按钮的JPanel ---
    private JPanel buildSplitSendButton
    (java.util.function.Consumer<JButton> sendAction, java.util.function.Consumer<JButton> sendAndDownloadAction) {
        JButton sendBtn = new JButton(RequestManBundle.message("main.send"));
        JButton arrowBtn = new JButton(RequestManBundle.message("main.dropdown.arrow"));
        int arc = 18;
        int btnHeight = 36;
        int btnWidth = 64;
        int arrowWidth = 36;
        // 主按钮样式
        sendBtn.setPreferredSize(new Dimension(btnWidth, btnHeight));
        sendBtn.setMinimumSize(new Dimension(btnWidth, btnHeight));
        sendBtn.setMaximumSize(new Dimension(btnWidth, btnHeight));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 0, UIManager.getColor("Button.shadow")),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        sendBtn.setFont(sendBtn.getFont().deriveFont(Font.BOLD, 15f));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 圆角
        sendBtn.setContentAreaFilled(true);
        sendBtn.setOpaque(false);
        sendBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                sendBtn.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                sendBtn.repaint();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                sendBtn.repaint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                sendBtn.repaint();
            }
        });
        // 下拉按钮样式
        arrowBtn.setPreferredSize(new Dimension(arrowWidth, btnHeight));
        arrowBtn.setMinimumSize(new Dimension(arrowWidth, btnHeight));
        arrowBtn.setMaximumSize(new Dimension(arrowWidth, btnHeight));
        arrowBtn.setFocusPainted(false);
        arrowBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 1, UIManager.getColor("Button.shadow")),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        arrowBtn.setFont(sendBtn.getFont().deriveFont(Font.BOLD, 15f));
        arrowBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        arrowBtn.setContentAreaFilled(true);
        arrowBtn.setOpaque(false);
        arrowBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                arrowBtn.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                arrowBtn.repaint();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                arrowBtn.repaint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                arrowBtn.repaint();
            }
        });
        // 圆角绘制
        sendBtn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth() + arc, c.getHeight(), arc, arc);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        arrowBtn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                // 只右侧圆角
                g2.fillRoundRect(-arc, 0, c.getWidth() + arc, c.getHeight(), arc, arc);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        // 下拉菜单
        JPopupMenu menu = new JPopupMenu();
        JMenuItem downloadItem = new JMenuItem(RequestManBundle.message("main.send.and.download"));
        downloadItem.setFont(sendBtn.getFont());
        menu.add(downloadItem);
        // 事件绑定，传递按钮本身
        sendBtn.addActionListener(e -> sendAction.accept(sendBtn));
        arrowBtn.addActionListener(e -> menu.show(arrowBtn, 0, arrowBtn.getHeight()));
        downloadItem.addActionListener(e -> sendAndDownloadAction.accept(sendBtn));
        // panel无间隙
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(Box.createRigidArea(new Dimension(4, 0)));
        panel.add(sendBtn);
        panel.add(arrowBtn);
        customEditPanelSendBtnPanel = panel;
        return panel;
    }

    // --- 发送逻辑提取 ---
    private void doSendCustomRequest(JButton btn) {
        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (autoSaveManager != null) {
            autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
        } else {
            stopTableEditingForTabIndex(currentTabIndex);
        }

        // 使用RequestSenderManager发送请求
        DefaultResponseHandler responseHandler = new DefaultResponseHandler(btn, responsePanel);
        RequestSenderManager.sendCustomRequest(project, editingApi, customUrlField, customMethodBox,
                customParamsPanel, customBodyPanel, customPostOpPanel, customAuthPanel, responseHandler);
    }

    private void doSendScanRequest(JButton btn, ApiInfo apiInfo) {
        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (autoSaveManager != null) {
            autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
        } else {
            stopTableEditingForTabIndex(currentTabIndex);
        }

        // 使用RequestSenderManager发送请求
        DefaultResponseHandler responseHandler = new DefaultResponseHandler(btn, responsePanel);
        RequestSenderManager.sendScanRequest(project, apiInfo, paramsPanel, bodyPanel,
                headersPanel, cookiesPanel, authPanel, postOpPanel, responseHandler);
    }

    private void doSendAndDownloadCustom(JButton btn) {
        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (autoSaveManager != null) {
            autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
        } else {
            stopTableEditingForTabIndex(currentTabIndex);
        }

        // 使用RequestSenderManager发送请求并下载响应
        DefaultResponseHandler responseHandler = new DefaultResponseHandler(btn, responsePanel);
        RequestSenderManager.sendCustomRequestAndDownload(project, editingApi, customUrlField, customMethodBox,
                customParamsPanel, customBodyPanel, customPostOpPanel, customAuthPanel, responseHandler);
    }

    private void doSendAndDownloadScan(JButton btn, ApiInfo apiInfo) {
        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (autoSaveManager != null) {
            autoSaveManager.runWithImmediateTableUpdate(() -> stopTableEditingForTabIndex(currentTabIndex));
        } else {
            stopTableEditingForTabIndex(currentTabIndex);
        }

        // 使用RequestSenderManager发送请求并下载响应
        DefaultResponseHandler responseHandler = new DefaultResponseHandler(btn, responsePanel);
        RequestSenderManager.sendScanRequestAndDownload(project, apiInfo, paramsPanel, bodyPanel,
                headersPanel, cookiesPanel, authPanel, postOpPanel, responseHandler);
    }

    // 新增：表格主动结束编辑的工具方法
    private void stopTableEditing(JTable table) {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    // 方法参数类型比对
    private boolean methodParamTypesMatch(PsiMethod method, List<String> apiParamTypes) {
        PsiParameter[] params = method.getParameterList().getParameters();
        if (params.length != (apiParamTypes == null ? 0 : apiParamTypes.size())) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            String psiType = params[i].getType().getCanonicalText();
            if (!psiType.equalsIgnoreCase(apiParamTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 显示性能报告对话框
     */
    private void showPerformanceReport() {
        try {
            // 检查是否启用了性能监控
            boolean monitoringEnabled = PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
            if (!monitoringEnabled) {
                JOptionPane.showMessageDialog(this,
                        RequestManBundle.message("main.performance.not.enabled"),
                        RequestManBundle.message("main.performance.tip"),
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

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
            JButton closeButton = new JButton(RequestManBundle.message("main.performance.close"));
            JButton clearButton = new JButton(RequestManBundle.message("main.performance.clear"));
            JButton refreshButton = new JButton(RequestManBundle.message("main.performance.refresh"));

            closeButton.addActionListener(e -> dialog.dispose());
            clearButton.addActionListener(e -> {
                PerformanceMonitor.clearStats();
                textArea.setText(PerformanceMonitor.getPerformanceReport());
            });
            refreshButton.addActionListener(e -> {
                textArea.setText(PerformanceMonitor.getPerformanceReport());
            });

            buttonPanel.add(closeButton);
            buttonPanel.add(clearButton);
            buttonPanel.add(refreshButton);

            // 组装对话框
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // 设置对话框属性
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    RequestManBundle.message("main.performance.get.fail") + e.getMessage(),
                    RequestManBundle.message("common.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 响应格式化功能已移至DefaultResponseHandler中
     * 此处不再需要重复实现
     */

    /**
     * 刷新性能监控按钮的显示状态
     */
    public void refreshPerformanceMonitoringButton() {
        // 这个方法可以在需要时重新构建面板
        // 目前采用简单的实现：只在面板初始化时检查设置
    }

    /**
     * 刷新环境选择器
     */
    public void refreshEnvironmentSelector() {
        if (environmentSelector != null) {
            environmentSelector.refreshEnvironments();
        }
    }

    /**
     * 更新自动保存设置
     */
    public void updateAutoSaveSetting() {
        if (autoSaveManager != null) {
            autoSaveManager.updateAutoSaveSetting();
        }
    }

    /**
     * 查找指定项目的RequestManPanel实例
     */
    public static RequestManPanel findRequestManPanel(Project project) {
        return instances.get(project);
    }


    /**
     * 从Panel获取表格
     */
    private static JTable getTableFromPanel(Object panel, String tableName) {
        Object paramsPanel = SwingUtils.getObject(panel, tableName);
        if (paramsPanel == null) {
            return null;
        }
        return SwingUtils.getTable(paramsPanel);
    }


    /**
     * 从BodyPanel获取JSON文本区域
     */
    private static JTextArea getJsonAreaFromBodyPanel(BodyPanel panel) {
        try {
            java.lang.reflect.Field jsonAreaField = BodyPanel.class.getDeclaredField("jsonArea");
            jsonAreaField.setAccessible(true);
            return (JTextArea) jsonAreaField.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从EditableBodyPanel获取JSON文本区域
     */
    private static JTextArea getJsonAreaFromBodyPanel(EditableBodyPanel panel) {
        try {
            java.lang.reflect.Field jsonAreaField = EditableBodyPanel.class.getDeclaredField("jsonArea");
            jsonAreaField.setAccessible(true);
            return (JTextArea) jsonAreaField.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从BodyPanel获取XML文本区域
     */
    private static JTextArea getXmlAreaFromBodyPanel(BodyPanel panel) {
        try {
            java.lang.reflect.Field xmlAreaField = BodyPanel.class.getDeclaredField("xmlArea");
            xmlAreaField.setAccessible(true);
            return (JTextArea) xmlAreaField.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从EditableBodyPanel获取XML文本区域
     */
    private static JTextArea getXmlAreaFromBodyPanel(EditableBodyPanel panel) {
        try {
            java.lang.reflect.Field xmlAreaField = EditableBodyPanel.class.getDeclaredField("xmlArea");
            xmlAreaField.setAccessible(true);
            return (JTextArea) xmlAreaField.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从AuthPanel获取认证模式下拉框
     */
    private static JComboBox<?> getAuthModeComboBox(AuthPanel panel) {
        try {
            java.lang.reflect.Field comboField = AuthPanel.class.getDeclaredField("modeBox");
            comboField.setAccessible(true);
            return (JComboBox<?>) comboField.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从AuthPanel获取认证值文本字段
     */
    private static JTextField getAuthValueField(AuthPanel panel) {
        try {
            java.lang.reflect.Field field = AuthPanel.class.getDeclaredField("authField");
            field.setAccessible(true);
            return (JTextField) field.get(panel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置拖拽支持，允许用户通过拖拽重新排序自定义接口列表
     */
    private void setupDragAndDropSupport() {
        customApiList.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                dragIndex = customApiList.getSelectedIndex();
                if (dragIndex < 0) {
                    return null;
                }
                return new CustomApiTransferable(customApiListModel.get(dragIndex));
            }

            @Override
            public boolean canImport(TransferSupport support) {
                boolean ok = support.isDrop()
                        && support.isDataFlavorSupported(CustomApiTransferable.CUSTOM_API_FLAVOR);
                if (ok) {
                    try {
                        JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                        int newIndex = dl.getIndex();
                        boolean newInsert = dl.isInsert();
                        if (newIndex != dropLineIndex || newInsert != dropInsert) {
                            dropLineIndex = newIndex;
                            dropInsert = newInsert;
                            customApiList.repaint();
                        }
                    } catch (Exception ignore) {
                    }
                }
                return ok;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    CustomApiInfo dragged = (CustomApiInfo) support.getTransferable()
                            .getTransferData(CustomApiTransferable.CUSTOM_API_FLAVOR);

                    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                    int dropIndex = dl.getIndex();
                    dropInsert = dl.isInsert();
                    if (dropIndex < 0) {
                        dropIndex = customApiListModel.getSize();
                    }

                    // 同列表内移动：先删后插，向下移动做索引补偿
                    if (dragIndex >= 0 && dragIndex < customApiListModel.getSize()) {
                        if (dropIndex > dragIndex) {
                            dropIndex--; // 补偿
                        }
                        customApiListModel.remove(dragIndex);
                    }
                    if (dropIndex < 0) {
                        dropIndex = 0;
                    }
                    if (dropIndex > customApiListModel.getSize()) {
                        dropIndex = customApiListModel.getSize();
                    }

                    customApiListModel.add(dropIndex, dragged);
                    customApiList.setSelectedIndex(dropIndex);

                    // 防抖持久化
                    if (persistDebounce != null) {
                        persistDebounce.restart();
                    } else {
                        CustomApiStorage.persistCustomApiList(project, customApiListModel);
                    }
                    // 重置分割线状态
                    dropLineIndex = -1;
                    dropInsert = true;
                    customApiList.repaint();
                    return true;
                } catch (Exception ex) {
                    LogUtil.warn("拖拽导入失败: " + ex.getMessage());
                    return false;
                } finally {
                    dragIndex = -1;
                }
            }

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                // 列表内重排已在 importData 完成，这里不做删除/持久化，避免重复
                dragIndex = -1;
                dropLineIndex = -1;
                dropInsert = true;
                customApiList.repaint();
            }
        });
    }


    public ParamsTablePanel getParamsPanel() {
        return paramsPanel;
    }

    public BodyPanel getBodyPanel() {
        return bodyPanel;
    }

    public HeadersPanel getHeadersPanel() {
        return headersPanel;
    }

    public CookiesPanel getCookiesPanel() {
        return cookiesPanel;
    }

    public AuthPanel getAuthPanel() {
        return authPanel;
    }

    public PreOpPanel getPreOpPanel() {
        return preOpPanel;
    }

    public PostOpPanel getPostOpPanel() {
        return postOpPanel;
    }

    public EditableBodyPanel getCustomBodyPanel() {
        return customBodyPanel;
    }

    public HeadersPanel getCustomHeadersPanel() {
        return customHeadersPanel;
    }

    public CookiesPanel getCustomCookiesPanel() {
        return customCookiesPanel;
    }

    public PreOpPanel getCustomPreOpPanel() {
        return customPreOpPanel;
    }

    public PostOpPanel getCustomPostOpPanel() {
        return customPostOpPanel;
    }

    public ParamsTablePanel getCustomParamsPanel() {
        return customParamsPanel;
    }

    public JList<CustomApiInfo> getCustomApiList() {
        return customApiList;
    }

    public DefaultListModel<CustomApiInfo> getCustomApiListModel() {
        return customApiListModel;
    }

    public AuthPanel getCustomAuthPanel() {
        return customAuthPanel;
    }
}