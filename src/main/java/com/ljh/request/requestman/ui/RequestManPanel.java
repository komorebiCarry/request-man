package com.ljh.request.requestman.ui;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
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
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.search.ApiSearchPopup;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import com.ljh.request.requestman.util.CustomApiStorage;
import com.ljh.request.requestman.util.PerformanceMonitor;
import com.ljh.request.requestman.util.ProjectSettingsManager;
import com.ljh.request.requestman.util.RequestSender;
import com.ljh.request.requestman.util.VariableManager;
import com.ljh.request.requestman.ui.ImportExportDialog;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
     * 本地持久化缓存文件后缀
     */
    private static final String CACHE_SUFFIX = ".json";
    /**
     * 内存缓存，避免频繁读写磁盘，LRU策略最大200条，防止内存泄漏
     */
    private final Map<String, Map<String, Object>> localCache = new LinkedHashMap<>() {
        private static final int MAX_ENTRIES = 200;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
            if (size() > MAX_ENTRIES) {
                return true;
            } else {
                return false;
            }
        }
    };
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
    private EditableParamsPanel customParamsPanel;
    /**
     * 自定义接口后置操作编辑面板
     */
    private PostOpPanel customPostOpPanel;
    /**
     * 主界面参数面板（自动扫描模式专用）
     */
    private ParamsPanel paramsPanel;
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
     * 线程池，用于异步处理请求发送等操作
     * 使用ThreadPoolExecutor手动创建，避免使用Executors工具类
     */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            2, // 核心线程数
            4, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(100), // 工作队列
            r -> {
                Thread t = new Thread(r, "RequestMan-Worker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    );

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
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 关闭主执行器
                EXECUTOR.shutdown();
                if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    EXECUTOR.shutdownNow();
                }
                
                // 关闭统计执行器
                STATS_EXECUTOR.shutdown();
                if (!STATS_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    STATS_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                EXECUTOR.shutdownNow();
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
     * 顶部接口搜索按钮
     */
    private JButton apiSearchButton;

    /**
     * 环境选择器
     */
    private EnvironmentSelector environmentSelector;

    // 静态实例管理，用于通知刷新
    private static final Map<Project, RequestManPanel> instances = new HashMap<>();

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
        // 顶部区域：模式选择 + 按钮 + 接口下拉框
        JPanel topPanel = buildTopPanel();
        // 详情区
        detailPanel.setBorder(BorderFactory.createTitledBorder("接口详情"));
        // 响应折叠面板
        responsePanel = new ResponseCollapsePanel("返回响应");
        responsePanel.setResponseText("点击'发送'按钮获取返回结果");
        // 主布局：顶部为topPanel，下方为详情区+响应区
        add(topPanel, BorderLayout.NORTH);
        add(detailPanel, BorderLayout.CENTER);
        add(responsePanel, BorderLayout.SOUTH);

        // 初始化按钮状态
        refreshButton.setToolTipText("刷新接口");

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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        // 接口搜索按钮，放在模式切换按钮前
        apiSearchButton = new JButton(AllIcons.Actions.Find);
        apiSearchButton.setToolTipText("接口搜索");
        apiSearchButton.setPreferredSize(new Dimension(36, 36));
        apiSearchButton.setMaximumSize(new Dimension(36, 36));
        apiSearchButton.setFocusPainted(false);
        apiSearchButton.setBorderPainted(true);
        // 事件：弹出接口搜索弹窗
        apiSearchButton.addActionListener(e -> {
            ApiSearchPopup popup = new ApiSearchPopup(project);
            popup.show();
        });
        // 先加搜索按钮
        panel.add(apiSearchButton);
        panel.add(Box.createHorizontalStrut(8));

        // 模式切换按钮
        modeSwitchBtn = new JButton("切换模式");
        modeSwitchBtn.setFocusPainted(false);
        modeSwitchBtn.setBorderPainted(true);
        modeSwitchBtn.setPreferredSize(new Dimension(36, 36));
        modeSwitchBtn.setMaximumSize(new Dimension(36, 36));
        updateModeSwitchBtn();
        modeSwitchBtn.addActionListener(e -> {
            customMode = !customMode;
            if (customMode) {
                refreshButton.setIcon(AllIcons.General.Add);
                refreshButton.setToolTipText("新增接口");
                switchToCustomMode();
            } else {
                refreshButton.setIcon(AllIcons.Actions.Refresh);
                refreshButton.setToolTipText("刷新接口");
                switchToScanMode();
            }
            updateModeSwitchBtn();
        });
        // 使用标志位避免重复注册，性能更优
        if (refreshButton.getActionListeners().length == 0) {
            refreshButton.addActionListener(e -> {
                if (customMode) {
                    customApiList.clearSelection();
                    showCustomApiDetail(null);
                } else {
                    refreshApiList();
                }
            });
        }
        apiComboBox.addActionListener(e -> {
            ApiInfo selected = (ApiInfo) apiComboBox.getSelectedItem();
            showApiDetail(selected);
        });
        panel.add(modeSwitchBtn);
        panel.add(Box.createHorizontalStrut(8));
        // 刷新/新增按钮
        refreshButton.setPreferredSize(new Dimension(36, 36));
        refreshButton.setMaximumSize(new Dimension(36, 36));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(true);
        panel.add(refreshButton);
        panel.add(Box.createHorizontalStrut(8));
        // 下拉框和定位按钮并排
        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.X_AXIS));
        comboPanel.add(apiComboBox);
        // 靶心定位按钮
        JButton locateButton = new JButton(AllIcons.General.Locate);
        locateButton.setToolTipText("定位到当前光标方法的接口");
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
            JOptionPane.showMessageDialog(this, "未找到对应接口", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        comboPanel.add(Box.createHorizontalStrut(4));
        comboPanel.add(locateButton);
        comboPanel.setMaximumSize(new Dimension(600, 36));
        panel.add(comboPanel);

        // 环境选择器
        panel.add(Box.createHorizontalStrut(8));
        environmentSelector = new EnvironmentSelector(project);
        panel.add(environmentSelector);

        // 根据设置决定是否显示性能监控按钮
        boolean performanceMonitoringEnabled = PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
        if (performanceMonitoringEnabled) {
            panel.add(Box.createHorizontalStrut(8));
            JButton performanceButton = new JButton("📊");
            performanceButton.setToolTipText("性能监控");
            performanceButton.setPreferredSize(new Dimension(36, 36));
            performanceButton.setMaximumSize(new Dimension(36, 36));
            performanceButton.setFocusPainted(false);
            performanceButton.setBorderPainted(true);
            performanceButton.addActionListener(e -> showPerformanceReport());
            panel.add(performanceButton);
        }

        return panel;
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
            modeSwitchBtn.setToolTipText("切换到自动扫描模式");
        } else {
            modeSwitchBtn.setText("\uD83D\uDCE1");
            modeSwitchBtn.setToolTipText("切换到自定义接口模式");
        }
    }

    /**
     * 刷新接口下拉框。
     * 1. 调用ApiInfoExtractor获取接口信息（后台线程）
     * 2. 填充下拉框模型（UI线程）
     * 3. 处理异常和空数据
     */
    private void refreshApiList() {
        apiComboBoxModel.removeAllElements();
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
                if (finalApis == null || finalApis.isEmpty()) {
                    apiComboBoxModel.addElement(new ApiInfo("未检测到接口方法", "", "", "", ""));
                } else {
                    for (ApiInfo api : finalApis) {
                        apiComboBoxModel.addElement(api);
                    }
                }
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
        detailPanel.removeAll();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText("点击'发送'按钮获取返回结果");
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        if (apiInfo == null) {
            detailPanel.add(new JLabel("未选择接口"), BorderLayout.CENTER);
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }
        JTabbedPane mainTab = buildMainTab(apiInfo);
        detailPanel.add(mainTab, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * 构建主Tab面板，包括请求、响应定义、接口说明、预览文档。
     *
     * @param apiInfo 接口信息
     * @return 主Tab面板
     */
    private JTabbedPane buildMainTab(ApiInfo apiInfo) {
        JTabbedPane mainTab = new JTabbedPane();
        mainTab.addTab("请求", buildRequestPanel(apiInfo));
        mainTab.addTab("响应定义", buildResponsePanel(apiInfo));
        mainTab.addTab("接口说明", buildDocPanel(apiInfo));
        mainTab.addTab("预览文档", buildPreviewPanel(apiInfo));
        return mainTab;
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
        JPanel requestLine = new JPanel();
        requestLine.setLayout(new BoxLayout(requestLine, BoxLayout.X_AXIS));
        JLabel methodLabel = new JLabel(apiInfo.getHttpMethod());
        methodLabel.setForeground(new java.awt.Color(220, 53, 69));
        JTextField urlField = new JTextField(apiInfo.getUrl());
        urlField.setEditable(false);
        urlField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        JPanel splitSendPanel = buildSplitSendButton(
                btn -> doSendScanRequest(btn, apiInfo),
                btn -> doSendAndDownloadScan(btn, apiInfo)
        );
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveCustomEdit(apiInfo));
        requestLine.add(methodLabel);
        requestLine.add(urlField);
        requestLine.add(splitSendPanel);
        requestLine.add(saveButton);
        return requestLine;
    }

    /**
     * 构建参数Tab，包括Params、Body、Headers、Cookies、Auth、前置操作、后置操作。
     *
     * @param apiInfo 接口信息
     * @return 参数Tab
     */
    private JTabbedPane buildParamTab(ApiInfo apiInfo) {
        JTabbedPane paramTab = new JTabbedPane();
        // Params 持久化支持
        paramsPanel = new ParamsPanel(apiInfo.getParams());
        paramTab.addTab("Params", paramsPanel);
        // Body 持久化支持
        bodyPanel = new BodyPanel(apiInfo.getBodyParams());
        paramTab.addTab("Body", bodyPanel);
        // Headers 持久化支持
        headersPanel = new HeadersPanel();
        // Cookies 持久化支持
        cookiesPanel = new CookiesPanel();
        // Auth 持久化支持
        authPanel = new AuthPanel();
        // PostOp 持久化支持
        postOpPanel = new PostOpPanel();
        // 加载本地缓存
        Map<String, Object> cache = loadCustomEdit(apiInfo);
        if (cache != null) {
            // 恢复 params
            if (cache.get("params") instanceof List<?> list) {
                List<String> paramValues = new ArrayList<>();
                for (Object obj : list) {
                    paramValues.add(obj != null ? obj.toString() : "");
                }
                paramsPanel.setParamsValueList(paramValues);
            }
            // 恢复 body（仅json类型）
            if (cache.get("bodyJson") instanceof String json) {
                bodyPanel.setJsonBodyText(json);
            }
            // 恢复 headers
            if (cache.get("headers") instanceof List<?> list) {
                List<HeadersPanel.HeaderItem> headerItems = new ArrayList<>();
                for (Object obj : list) {
                    if (obj instanceof HeadersPanel.HeaderItem) {
                        headerItems.add((HeadersPanel.HeaderItem) obj);
                    } else if (obj instanceof Map map) {
                        String name = (String) map.get("name");
                        String value = (String) map.get("value");
                        String type = (String) map.get("type");
                        String desc = (String) map.get("desc");
                        headerItems.add(new HeadersPanel.HeaderItem(name, value, type, desc));
                    }
                }
                headersPanel.setHeadersData(headerItems);
            }
            // 恢复 cookies
            if (cache.get("cookies") instanceof List<?> list) {
                List<CookiesPanel.CookieItem> cookieItems = new ArrayList<>();
                for (Object obj : list) {
                    if (obj instanceof CookiesPanel.CookieItem) {
                        cookieItems.add((CookiesPanel.CookieItem) obj);
                    } else if (obj instanceof Map map) {
                        String name = (String) map.get("name");
                        String value = (String) map.get("value");
                        String type = (String) map.get("type");
                        cookieItems.add(new CookiesPanel.CookieItem(name, value, type));
                    }
                }
                cookiesPanel.setCookiesData(cookieItems);
            }
            // 恢复 auth
            if (cache.get("auth") instanceof String authValue) {
                authPanel.setAuthValue(authValue);
            }
            // 恢复 postOp
            if (cache.get("postOp") instanceof List<?> list) {
                List<PostOpPanel.PostOpItem> postOpItems = new ArrayList<>();
                for (Object obj : list) {
                    if (obj instanceof PostOpPanel.PostOpItem) {
                        postOpItems.add((PostOpPanel.PostOpItem) obj);
                    } else if (obj instanceof Map map) {
                        String name = (String) map.get("name");
                        String type = (String) map.get("type");
                        String value = (String) map.get("value");
                        postOpItems.add(new PostOpPanel.PostOpItem(name, type, value));
                    }
                }
                postOpPanel.setPostOpData(postOpItems);
            }
        }
        paramTab.addTab("Headers", headersPanel);
        paramTab.addTab("Cookies", cookiesPanel);
        paramTab.addTab("Auth", authPanel);
        paramTab.addTab("前置操作", new PreOpPanel());
        paramTab.addTab("后置操作", postOpPanel);

        // 设置响应面板引用，用于JSONPath提取器
        postOpPanel.setResponsePanel(responsePanel);
        // 设置当前接口信息，用于获取响应定义
        postOpPanel.setCurrentApiInfo(apiInfo);
        paramTab.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        return paramTab;
    }

    /**
     * 构建响应定义Tab，直接显示响应结构树。
     *
     * @param apiInfo 当前接口信息
     * @return 响应定义面板
     */
    private JPanel buildResponsePanel(ApiInfo apiInfo) {
        JPanel responsePanel = new JPanel(new BorderLayout());
        List<ApiParam> responseParams = apiInfo != null ? apiInfo.getResponseParams() : null;
        responsePanel.add(new JsonBodyStructurePanel(responseParams), BorderLayout.CENTER);
        return responsePanel;
    }

    /**
     * 构建接口说明Tab。
     *
     * @param apiInfo 接口信息
     * @return 接口说明面板
     */
    private JPanel buildDocPanel(ApiInfo apiInfo) {
        JPanel docPanel = new JPanel();
        docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
        Border emptyBorder = BorderFactory.createEmptyBorder(20, 0, 0, 0);
        JLabel jLabelName = new JLabel("<html>接口名称: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getName() + "</html>");
        docPanel.add(jLabelName);
        JLabel jLabelHttpMethod = new JLabel("<html>接口请求: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getHttpMethod() + "</html>");
        jLabelHttpMethod.setBorder(emptyBorder);
        docPanel.add(jLabelHttpMethod);
        JLabel jLabelDisplayText = new JLabel("<html>接口信息: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getDisplayText() + "</html>");
        jLabelDisplayText.setBorder(emptyBorder);
        docPanel.add(jLabelDisplayText);
        JLabel jLabelClassName = new JLabel("<html>接口所在类: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getClassName() + "</html>");
        jLabelClassName.setBorder(emptyBorder);
        docPanel.add(jLabelClassName);
        JLabel jLabelDescription = new JLabel("<html>描述: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getDescription() + "</html>");
        jLabelDescription.setBorder(emptyBorder);
        docPanel.add(jLabelDescription);
        return docPanel;
    }


    /**
     * 构建预览文档Tab，集成PreviewDocPanel。
     *
     * @param apiInfo 接口信息
     * @return 预览文档面板
     */
    private JPanel buildPreviewPanel(ApiInfo apiInfo) {
        List<ApiParam> pathParams = apiInfo.getParams() != null ? apiInfo.getParams().stream().filter(p -> "路径变量".equals(p.getType())).toList() : Collections.emptyList();
        List<ApiParam> queryParams = apiInfo.getParams() != null ? apiInfo.getParams().stream().filter(p -> "请求参数".equals(p.getType())).toList() : Collections.emptyList();
        List<ApiParam> bodyParams = apiInfo.getBodyParams();
        ContentType contentType = ContentType.APPLICATION_JSON;
        String responseJson = "{\n  \"code\": \"\",\n  \"message\": \"\",\n  \"data\": null\n}";
        return new PreviewDocPanel(apiInfo, contentType, bodyParams, pathParams, queryParams, responseJson);
    }

    /**
     * 将字符串中的非法文件名字符替换为下划线，保证文件名合法（适用于 Windows 文件系统）
     *
     * @param name 原始字符串
     * @return 合法文件名
     */
    private static String safeFileName(String name) {
        // Windows 文件名非法字符: \\ / : * ? " < > | { }
        return name.replaceAll("[\\\\/:*?\"<>|{}]", "_");
    }

    /**
     * 保存自定义编辑内容到本地缓存
     *
     * @param apiInfo 当前接口信息
     */
    private void saveCustomEdit(ApiInfo apiInfo) {
        // 保存前，主动结束Headers、Cookies、后置操作等表格的编辑，确保内容写入TableModel，避免数据丢失
        if (headersPanel != null) {
            JTable table = getTableFromHeadersPanel(headersPanel);
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
        if (cookiesPanel != null) {
            JTable table = getTableFromCookiesPanel(cookiesPanel);
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
        if (postOpPanel != null) {
            JTable table = getTableFromPostOpPanel(postOpPanel);
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
        try {
            Map<String, Object> data = new HashMap<>();
            // Params 持久化
            if (paramsPanel != null) {
                data.put("params", paramsPanel.getParamsValueList());
            }
            // Body 持久化（仅json类型）
            if (bodyPanel != null) {
                data.put("bodyJson", bodyPanel.getJsonBodyText());
            }
            // Headers 持久化
            if (headersPanel != null) {
                data.put("headers", headersPanel.getHeadersData());
            }
            // Cookies 持久化
            if (cookiesPanel != null) {
                data.put("cookies", cookiesPanel.getCookiesData());
            }
            // Auth 持久化
            if (authPanel != null) {
                data.put("auth", authPanel.getAuthValue());
            }
            // PostOp 持久化
            if (postOpPanel != null) {
                data.put("postOp", postOpPanel.getPostOpData());
            }
            String key = buildApiKey(apiInfo);
            key = safeFileName(key); // 保证文件名合法，防止非法字符导致保存失败
            Path dir = Paths.get(getCacheDir());
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve(key + CACHE_SUFFIX);
            String json = JSONUtil.toJsonStr(data);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
            localCache.put(key, data);
            JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 构建接口唯一key（项目名+url+method+参数结构hash）
     */
    private String buildApiKey(ApiInfo apiInfo) {
        String projectName = project != null ? project.getName() : "default";
        String base = projectName + "#" + apiInfo.getUrl() + "#" + apiInfo.getHttpMethod();
        return base;
    }

    /**
     * 加载本地缓存（在 showApiDetail 或 buildParamTab、BodyPanel 等处调用）
     */
    private Map<String, Object> loadCustomEdit(ApiInfo apiInfo) {
        try {
            String key = safeFileName(buildApiKey(apiInfo));
            if (localCache.containsKey(key)) {
                return localCache.get(key);
            }
            Path dir = Paths.get(getCacheDir());
            Path file = dir.resolve(key + CACHE_SUFFIX);
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = JSONUtil.toBean(json, Map.class);
                localCache.put(key, data);
                return data;
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    /**
     * 检查接口参数结构是否变更，变更则清除本地缓存
     *
     * @param apiInfo 当前接口信息
     */
    private void clearCacheIfParamChanged(ApiInfo apiInfo) {
        String key = buildApiKey(apiInfo);
        Path file = Paths.get(getCacheDir(), key + CACHE_SUFFIX);
        if (Files.exists(file)) {
            // 只要参数结构hash变了，key就变了，旧文件不会被加载
            // 可定期清理CACHE_DIR下的无用文件
        }
    }

    /**
     * 获取本地持久化缓存目录，优先使用用户配置，按项目隔离
     *
     * @return 缓存目录绝对路径，结尾带分隔符，系统兼容
     */
    private String getCacheDir() {
        String dir = PropertiesComponent.getInstance().getValue("requestman.cacheDir");
        if (dir == null || dir.isEmpty()) {
            dir = Paths.get(System.getProperty("user.home"), ".requestman_cache").toString() + File.separator;
        }
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        // 按项目名称创建子目录，实现项目隔离
        String projectName = project != null ? project.getName() : "default";
        return dir + projectName + File.separator;
    }

    // 重写toString，保证下拉框显示友好
    static {
        javax.swing.UIManager.put("ComboBox.rendererUseListColors", Boolean.TRUE);
    }

    /**
     * 切换到自定义接口模式，彻底清空并重建布局，避免页面错乱，每次都新建customPanel
     */
    private void switchToCustomMode() {
        // 彻底移除所有子组件，防止残留
        this.removeAll();
        JPanel topPanel = buildTopPanel();
        // 每次都新建，避免引用丢失
        customPanel = buildCustomPanel();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText("点击'发送'按钮获取返回结果");
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
        // 彻底移除所有子组件，防止残留
        this.removeAll();
        JPanel topPanel = buildTopPanel();
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText("点击'发送'按钮获取返回结果");
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
        customApiList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                CustomApiInfo selected = customApiList.getSelectedValue();
                showCustomApiDetail(selected);
            }
        });

        // 添加右键菜单
        setupCustomApiListContextMenu();

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
        customParamsPanel = new EditableParamsPanel(new ArrayList<>());
        customPostOpPanel = new PostOpPanel();
        customBodyPanel = new EditableBodyPanel(new ArrayList<>());
        saveCustomBtn = new JButton("保存");
        deleteCustomBtn = new JButton("删除");
        // 组装右侧
        customEditPanel.removeAll();
        customEditPanel.setLayout(new BoxLayout(customEditPanel, BoxLayout.Y_AXIS));
        // 顶部一行
        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        JLabel nameLabel = new JLabel("接口名称:");
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        customNameField = new JTextField();
        customNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customNameField.setPreferredSize(new Dimension(0, 28));
        customNameField.setAlignmentY(Component.CENTER_ALIGNMENT);
        topRow.add(nameLabel);
        topRow.add(Box.createHorizontalStrut(8));
        topRow.add(customNameField);
        topRow.add(Box.createHorizontalStrut(16));
        topRow.add(customParamsPanel);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        customEditPanel.add(topRow);
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
        btnPanel.add(saveCustomBtn);
        btnPanel.add(deleteCustomBtn);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        customEditPanel.add(btnPanel);
        splitPane.setRightComponent(customEditPanel);
        // 事件绑定 - 使用标志位避免重复注册
        if (saveCustomBtn.getActionListeners().length == 0) {
            saveCustomBtn.addActionListener(e -> saveOrUpdateCustomApi());
        }
        if (deleteCustomBtn.getActionListeners().length == 0) {
            deleteCustomBtn.addActionListener(e -> deleteSelectedCustomApi());
        }
        return splitPane;
    }

    /**
     * 显示自定义接口详情到右侧编辑面板
     */
    private void showCustomApiDetail(CustomApiInfo api) {
        // 清空响应面板内容，避免JSONPath提取器使用上一个接口的响应内容
        if (responsePanel != null) {
            responsePanel.setResponseText("点击'发送'按钮获取返回结果");
            responsePanel.setStatusText("");
            responsePanel.collapse();
        }
        customEditPanel.removeAll();
        customEditPanel.setLayout(new BoxLayout(customEditPanel, BoxLayout.Y_AXIS));
        // 顶部一行
        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        JLabel nameLabel = new JLabel("接口名称:");
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        customNameField = new JTextField();
        customNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        customNameField.setPreferredSize(new Dimension(0, 28));
        customNameField.setAlignmentY(Component.CENTER_ALIGNMENT);
        JPanel splitSendPanel = buildSplitSendButton(
                btn -> doSendCustomRequest(btn),
                btn -> doSendAndDownloadCustom(btn)
        );
        splitSendPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        topRow.add(nameLabel);
        topRow.add(Box.createHorizontalStrut(8));
        topRow.add(customNameField);
        topRow.add(Box.createHorizontalStrut(16));
        topRow.add(splitSendPanel);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        customEditPanel.add(topRow);
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
        saveCustomBtn = new JButton("保存");
        deleteCustomBtn = new JButton("删除");
        btnPanel.add(saveCustomBtn);
        btnPanel.add(deleteCustomBtn);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        customEditPanel.add(btnPanel);
        // 回显数据
        if (api == null) {
            editingApi = null;
        } else {
            customNameField.setText(api.getName());
            customUrlField.setText(api.getUrl());
            customMethodBox.setSelectedItem(api.getHttpMethod());
            if (customParamsPanel != null) {
                customParamsPanel.setParams(api.getParams() != null ? api.getParams() : new ArrayList<>());
            }
            if (customPostOpPanel != null && api.getPostOps() != null) {
                customPostOpPanel.setPostOpData(api.getPostOps());
            }
            if (customBodyPanel != null) {
                String bodyType = api.getBodyType() != null ? api.getBodyType() : "none";
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
                    // 这里假设api.getBody()为Base64或十六进制字符串，需按实际情况转换
                    // 示例：customBodyPanel.setBinaryBody(decodeToBytes(api.getBody()));
                    // 这里直接传字符串（如有需要可实现decodeToBytes）
                    customBodyPanel.setBinaryBody(api.getBody() != null ? api.getBody().getBytes() : new byte[0]);
                }
            }
            // 恢复认证信息
            if (customAuthPanel != null) {
                customAuthPanel.setAuthMode(api.getAuthMode());
                customAuthPanel.setAuthValue(api.getAuthValue());
            }
            editingApi = api;
        }
        // 重新绑定按钮事件 - 使用标志位避免重复注册
        if (saveCustomBtn.getActionListeners().length == 0) {
            saveCustomBtn.addActionListener(e -> saveOrUpdateCustomApi());
        }
        if (deleteCustomBtn.getActionListeners().length == 0) {
            deleteCustomBtn.addActionListener(e -> deleteSelectedCustomApi());
        }
        customEditPanel.revalidate();
        customEditPanel.repaint();
    }

    /**
     * 保存或更新自定义接口
     */
    private void saveOrUpdateCustomApi() {
        // 保存前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (customParamsPanel != null) {
            JTable table = getTableFromEditableParamsPanel(customParamsPanel);
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
        if (customPostOpPanel != null) {
            JTable table = getTableFromPostOpPanel(customPostOpPanel);
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
        if (customBodyPanel != null) {
            // form-data
            JTable formTable = getTableFromEditableParamsPanel(customBodyPanel.getFormDataPanel());
            if (formTable != null && formTable.isEditing()) {
                formTable.getCellEditor().stopCellEditing();
            }
            // x-www-form-urlencoded
            JTable urlTable = getTableFromEditableParamsPanel(customBodyPanel.getUrlencodedPanel());
            if (urlTable != null && urlTable.isEditing()) {
                urlTable.getCellEditor().stopCellEditing();
            }
        }
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
                body = customBodyPanel.getJsonBody();
            } else if ("form-data".equals(bodyType)) {
                bodyParams = customBodyPanel.getFormDataParams();
            } else if ("x-www-form-urlencoded".equals(bodyType)) {
                bodyParams = customBodyPanel.getUrlencodedParams();
            } else if ("xml".equals(bodyType)) {
                body = customBodyPanel.getXmlBody();
            } else if ("binary".equals(bodyType)) {
                body = new String(customBodyPanel.getBinaryBody());
            }
        }
        List<ApiParam> params = customParamsPanel != null ? customParamsPanel.getParams() : new ArrayList<>();
        // 过滤掉参数名为空的行，防止空行被保存
        params = params.stream()
                .filter(p -> p.getName() != null && !p.getName().trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
        java.util.List<PostOpItem> postOps = customPostOpPanel != null ? customPostOpPanel.getPostOpData() : new java.util.ArrayList<>();
        if (name.isEmpty() || url.isEmpty() || method == null || method.isEmpty()) {
            JOptionPane.showMessageDialog(this, "接口名称、URL、方法不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
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
            customApiList.repaint();
        }
        persistCustomApiList();
        JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 设置自定义接口列表的右键菜单
     */
    private void setupCustomApiListContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        // 导入菜单项
        JMenuItem importMenuItem = new JMenuItem("导入");
        importMenuItem.addActionListener(e -> importCustomApis());
        contextMenu.add(importMenuItem);

        // 添加分隔线
        contextMenu.addSeparator();

        // 导出菜单项
        JMenuItem exportMenuItem = new JMenuItem("导出");
        exportMenuItem.addActionListener(e -> exportSelectedCustomApis());
        contextMenu.add(exportMenuItem);

        // 删除菜单项
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        deleteMenuItem.addActionListener(e -> deleteSelectedCustomApis());
        contextMenu.add(deleteMenuItem);

        // 添加右键菜单监听器
        customApiList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            private void showContextMenu(java.awt.event.MouseEvent e) {
                int index = customApiList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    // 如果点击的项不在选中范围内，则选中该项
                    if (!customApiList.isSelectedIndex(index)) {
                        customApiList.setSelectedIndex(index);
                    }
                }
                contextMenu.show(customApiList, e.getX(), e.getY());
            }
        });
    }


    /**
     * 导入自定义接口
     */
    private void importCustomApis() {
        // 获取当前所有接口列表用于导出时的参考
        List<CustomApiInfo> currentApis = new ArrayList<>();
        for (int i = 0; i < customApiListModel.getSize(); i++) {
            currentApis.add(customApiListModel.getElementAt(i));
        }

        ImportExportDialog dialog = new ImportExportDialog(project, true, currentApis, customApiListModel);
        if (dialog.showAndGet()) {
            // 导入成功后刷新界面
            customApiList.repaint();
            persistCustomApiList();
        }
    }

    /**
     * 导出选中的自定义接口
     */
    private void exportSelectedCustomApis() {
        int[] selectedIndices = customApiList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this, "请先选择要导出的接口", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<CustomApiInfo> selectedApis = new ArrayList<>();
        for (int index : selectedIndices) {
            selectedApis.add(customApiListModel.getElementAt(index));
        }

        ImportExportDialog dialog = new ImportExportDialog(project, false, selectedApis, customApiListModel);
        dialog.show();
    }

    /**
     * 删除选中的自定义接口
     */
    private void deleteSelectedCustomApis() {
        int[] selectedIndices = customApiList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的接口", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String message = selectedIndices.length == 1 ?
                "确定要删除选中的接口吗？" :
                String.format("确定要删除选中的 %d 个接口吗？", selectedIndices.length);

        int confirm = JOptionPane.showConfirmDialog(this, message, "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // 从后往前删除，避免索引变化
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                customApiListModel.remove(selectedIndices[i]);
            }
            showCustomApiDetail(null);
            persistCustomApiList();
        }
    }

    /**
     * 删除选中的自定义接口（单个删除，保留兼容性）
     */
    private void deleteSelectedCustomApi() {
        int idx = customApiList.getSelectedIndex();
        if (idx >= 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "确定要删除该接口吗？", "确认", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                customApiListModel.remove(idx);
                showCustomApiDetail(null);
                persistCustomApiList();
            }
        }
    }

    /**
     * 持久化自定义接口列表
     */
    private void persistCustomApiList() {
        ArrayList<CustomApiInfo> list = new ArrayList<>();
        for (int i = 0; i < customApiListModel.size(); i++) {
            list.add(customApiListModel.get(i));
        }
        CustomApiStorage.saveCustomApis(project, list);
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

    // 工具方法：判断字符串是否为JSON
    private boolean isJson(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    // 工具方法：判断字符串是否为XML
    private boolean isXml(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return t.startsWith("<") && t.endsWith(">") && t.contains("<?xml");
    }

    /**
     * 工具方法：安全获取EditableParamsPanel中的JTable实例
     *
     * @param panel EditableParamsPanel实例
     * @return JTable对象，若获取失败返回null
     */
    private static JTable getTableFromEditableParamsPanel(Object panel) {
        if (panel == null) {
            return null;
        }
        try {
            java.lang.reflect.Field tableField = panel.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object tableObj = tableField.get(panel);
            if (tableObj instanceof JTable) {
                return (JTable) tableObj;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 工具方法：安全获取PostOpPanel中的JTable实例
     *
     * @param panel PostOpPanel实例
     * @return JTable对象，若获取失败返回null
     */
    private static JTable getTableFromPostOpPanel(Object panel) {
        if (panel == null) {
            return null;
        }
        try {
            java.lang.reflect.Field tableField = panel.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object tableObj = tableField.get(panel);
            if (tableObj instanceof JTable) {
                return (JTable) tableObj;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 工具方法：安全获取HeadersPanel中的JTable实例
     *
     * @param panel HeadersPanel实例
     * @return JTable对象，若获取失败返回null
     */
    private static JTable getTableFromHeadersPanel(HeadersPanel panel) {
        if (panel == null) {
            return null;
        }
        try {
            java.lang.reflect.Field tableField = panel.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object tableObj = tableField.get(panel);
            if (tableObj instanceof JTable) {
                return (JTable) tableObj;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 工具方法：安全获取CookiesPanel中的JTable实例
     *
     * @param panel CookiesPanel实例
     * @return JTable对象，若获取失败返回null
     */
    private static JTable getTableFromCookiesPanel(CookiesPanel panel) {
        if (panel == null) {
            return null;
        }
        try {
            java.lang.reflect.Field tableField = panel.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object tableObj = tableField.get(panel);
            if (tableObj instanceof JTable) {
                return (JTable) tableObj;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- 保证showCustomApiDetail能正常调用Tab构建方法 ---
    private JTabbedPane buildCustomEditTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        customParamsPanel = new EditableParamsPanel(new ArrayList<>());
        customBodyPanel = new EditableBodyPanel(new ArrayList<>());
        HeadersPanel headersPanel = new HeadersPanel();
        CookiesPanel cookiesPanel = new CookiesPanel();
        customAuthPanel = new AuthPanel();
        customPostOpPanel = new PostOpPanel();
        // 使用已创建的customPostOpPanel，而不是创建新实例
        tabbedPane.addTab("Params", customParamsPanel);
        tabbedPane.addTab("Body", customBodyPanel);
        tabbedPane.addTab("Headers", headersPanel);
        tabbedPane.addTab("Cookies", cookiesPanel);
        tabbedPane.addTab("Auth", customAuthPanel);
        tabbedPane.addTab("后置操作", customPostOpPanel);

        // 设置响应面板引用，用于JSONPath提取器
        customPostOpPanel.setResponsePanel(responsePanel);
        // 自定义模式没有接口信息，使用默认响应定义

        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        return tabbedPane;
    }

    // --- 分体式发送按钮工具方法，返回包含主按钮和下拉按钮的JPanel ---
    private JPanel buildSplitSendButton(java.util.function.Consumer<JButton> sendAction, java.util.function.Consumer<JButton> sendAndDownloadAction) {
        JButton sendBtn = new JButton("发送");
        JButton arrowBtn = new JButton("▼");
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
        JMenuItem downloadItem = new JMenuItem("发送并下载");
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
        if (btn != null) {
            btn.setEnabled(false);
        }

        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (customParamsPanel != null) {
            JTable table = getTableFromEditableParamsPanel(customParamsPanel);
            stopTableEditing(table);
        }
        if (customPostOpPanel != null) {
            JTable table = getTableFromPostOpPanel(customPostOpPanel);
            stopTableEditing(table);
        }
        if (customBodyPanel != null) {
            // form-data
            JTable formTable = getTableFromEditableParamsPanel(customBodyPanel.getFormDataPanel());
            stopTableEditing(formTable);
            // x-www-form-urlencoded
            JTable urlTable = getTableFromEditableParamsPanel(customBodyPanel.getUrlencodedPanel());
            stopTableEditing(urlTable);
        }

        EXECUTOR.submit(() -> {
            try {
                String url = customUrlField.getText().trim();
                String method = (String) customMethodBox.getSelectedItem();
                java.util.List<ApiParam> params = customParamsPanel != null ? customParamsPanel.getParams() : new java.util.ArrayList<>();
                String bodyType = customBodyPanel != null ? customBodyPanel.getBodyType() : "none";
                java.util.List<ApiParam> bodyParams = new java.util.ArrayList<>();
                String bodyContent = "";
                byte[] binaryData = null;
                if (customBodyPanel != null) {
                    if ("json".equals(bodyType)) {
                        bodyContent = customBodyPanel.getJsonBody();
                    } else if ("xml".equals(bodyType)) {
                        bodyContent = customBodyPanel.getXmlBody();
                    } else if ("binary".equals(bodyType)) {
                        // 对于binary类型，获取二进制数据
                        binaryData = customBodyPanel.getBinaryBody();
                    } else if ("form-data".equals(bodyType)) {
                        bodyParams = customBodyPanel.getFormDataParams();
                    } else if ("x-www-form-urlencoded".equals(bodyType)) {
                        bodyParams = customBodyPanel.getUrlencodedParams();
                    }
                }
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                java.util.Map<String, String> cookies = new java.util.HashMap<>();
                String auth = "";
                if (editingApi != null && editingApi.getAuthMode() == 0) {
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else if (editingApi != null) {
                    auth = editingApi.getAuthValue();
                }
                java.util.List<PostOpItem> postOps = customPostOpPanel != null ? customPostOpPanel.getPostOpData() : new java.util.ArrayList<>();
                String urlPrefix = ProjectSettingsManager.getCurrentEnvironmentPreUrl(project);
                try (HttpResponse response = RequestSender.sendRequestRaw(
                        project, url, method, params, bodyType, bodyParams, bodyContent, binaryData, headers, cookies, auth, urlPrefix, postOps
                )) {
                    int status = response.getStatus();
                    String statusMsg = "HTTP状态: " + status + (status == 200 ? "（成功）" : "（失败）");
                    String respText = response.body();
                    String finalDisplayText = formatResponseText(respText);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        responsePanel.setStatusText(statusMsg);
                        responsePanel.setResponseText(finalDisplayText);
                        responsePanel.expand();
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                }
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    responsePanel.setStatusText("");
                    responsePanel.setResponseText("请求异常：" + ex.getMessage());
                    responsePanel.expand();
                    if (btn != null) {
                        btn.setEnabled(true);
                    }
                });
            }
        });
    }

    private void doSendScanRequest(JButton btn, ApiInfo apiInfo) {
        if (btn != null) {
            btn.setEnabled(false);
        }

        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (headersPanel != null) {
            JTable table = getTableFromHeadersPanel(headersPanel);
            stopTableEditing(table);
        }
        if (cookiesPanel != null) {
            JTable table = getTableFromCookiesPanel(cookiesPanel);
            stopTableEditing(table);
        }
        if (postOpPanel != null) {
            JTable table = getTableFromPostOpPanel(postOpPanel);
            stopTableEditing(table);
        }

        EXECUTOR.submit(() -> {
            try {
                String url = apiInfo.getUrl();
                String method = apiInfo.getHttpMethod();
                java.util.List<ApiParam> params = paramsPanel != null ? paramsPanel.getParams() : new java.util.ArrayList<>();
                String bodyType = bodyPanel != null ? bodyPanel.getBodyType() : "none";
                java.util.List<ApiParam> bodyParams = bodyPanel != null ? bodyPanel.getBodyParams() : new java.util.ArrayList<>();
                String bodyContent = bodyPanel != null ? bodyPanel.getBodyContent() : "";
                byte[] binaryData = "binary".equals(bodyType) && bodyPanel != null ? bodyPanel.getBinaryData() : null;
                java.util.Map<String, String> headers = headersPanel != null ? headersPanel.getHeadersMap() : new java.util.HashMap<>();
                java.util.Map<String, String> cookies = cookiesPanel != null ? cookiesPanel.getCookiesMap() : new java.util.HashMap<>();
                String auth = "";
                if (authPanel != null && authPanel.getAuthMode() == 0) {
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else if (authPanel != null) {
                    auth = authPanel.getAuthValue();
                }
                java.util.List<PostOpItem> postOps = postOpPanel != null ? postOpPanel.getPostOpData() : new java.util.ArrayList<>();
                String urlPrefix = ProjectSettingsManager.getCurrentEnvironmentPreUrl(project);
                try (HttpResponse response = RequestSender.sendRequestRaw(
                        project, url, method, params, bodyType, bodyParams, bodyContent, binaryData, headers, cookies, auth, urlPrefix, postOps
                )) {
                    int status = response.getStatus();
                    String statusMsg = "HTTP状态: " + status + (status == 200 ? "（成功）" : "（失败）");
                    String respText = response.body();
                    String finalDisplayText = formatResponseText(respText);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        responsePanel.setStatusText(statusMsg);
                        responsePanel.setResponseText(finalDisplayText);
                        responsePanel.expand();
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                }
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(customEditPanel, "请求异常: " + ex.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
                    if (btn != null) {
                        btn.setEnabled(true);
                    }
                });
            }
        });
    }

    private void doSendAndDownloadCustom(JButton btn) {
        if (btn != null) {
            btn.setEnabled(false);
        }

        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (customParamsPanel != null) {
            JTable table = getTableFromEditableParamsPanel(customParamsPanel);
            stopTableEditing(table);
        }
        if (customPostOpPanel != null) {
            JTable table = getTableFromPostOpPanel(customPostOpPanel);
            stopTableEditing(table);
        }
        if (customBodyPanel != null) {
            // form-data
            JTable formTable = getTableFromEditableParamsPanel(customBodyPanel.getFormDataPanel());
            stopTableEditing(formTable);
            // x-www-form-urlencoded
            JTable urlTable = getTableFromEditableParamsPanel(customBodyPanel.getUrlencodedPanel());
            stopTableEditing(urlTable);
        }

        EXECUTOR.submit(() -> {
            try {
                String url = customUrlField.getText().trim();
                String method = (String) customMethodBox.getSelectedItem();
                java.util.List<ApiParam> params = customParamsPanel != null ? customParamsPanel.getParams() : new java.util.ArrayList<>();
                String bodyType = customBodyPanel != null ? customBodyPanel.getBodyType() : "none";
                java.util.List<ApiParam> bodyParams = new java.util.ArrayList<>();
                String bodyContent = "";
                byte[] binaryData = null;
                if (customBodyPanel != null) {
                    if ("json".equals(bodyType)) {
                        bodyContent = customBodyPanel.getJsonBody();
                    } else if ("xml".equals(bodyType)) {
                        bodyContent = customBodyPanel.getXmlBody();
                    } else if ("binary".equals(bodyType)) {
                        // 对于binary类型，获取二进制数据
                        binaryData = customBodyPanel.getBinaryBody();
                    } else if ("form-data".equals(bodyType)) {
                        bodyParams = customBodyPanel.getFormDataParams();
                    } else if ("x-www-form-urlencoded".equals(bodyType)) {
                        bodyParams = customBodyPanel.getUrlencodedParams();
                    }
                }
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                java.util.Map<String, String> cookies = new java.util.HashMap<>();
                String auth = "";
                if (editingApi != null && editingApi.getAuthMode() == 0) {
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else if (editingApi != null) {
                    auth = editingApi.getAuthValue();
                }
                java.util.List<PostOpItem> postOps = postOpPanel != null ? postOpPanel.getPostOpData() : new java.util.ArrayList<>();
                String urlPrefix = ProjectSettingsManager.getCurrentEnvironmentPreUrl(project);
                try (HttpResponse response = RequestSender.sendRequestRaw(
                        project, url, method, params, bodyType, bodyParams, bodyContent, binaryData, headers, cookies, auth, urlPrefix, postOps
                )) {
                    int status = response.getStatus();
                    String statusMsg = "HTTP状态: " + status + (status == 200 ? "（成功）" : "（失败）");
                    String respText = response.body();
                    String finalDisplayText = formatResponseText(respText);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        responsePanel.setStatusText(statusMsg);
                        responsePanel.setResponseText(finalDisplayText);
                        responsePanel.expand();
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                    String contentType = response.header("Content-Type");
                    byte[] bytes = response.bodyBytes();
                    String ext = suggestFileExtension(contentType, bytes);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                        fileChooser.setDialogTitle("请选择保存文件的位置");
                        fileChooser.setSelectedFile(new java.io.File("response" + ext));
                        int userSelection = fileChooser.showSaveDialog(customEditPanel);
                        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
                            java.io.File fileToSave = fileChooser.getSelectedFile();
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fileToSave)) {
                                fos.write(bytes);
                                fos.flush();
                                javax.swing.JOptionPane.showMessageDialog(customEditPanel, "文件已保存: " + fileToSave.getAbsolutePath());
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(customEditPanel, "保存文件失败: " + ex.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                }
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(customEditPanel, "请求异常: " + ex.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
                    if (btn != null) {
                        btn.setEnabled(true);
                    }
                });
            }
        });
    }

    private void doSendAndDownloadScan(JButton btn, ApiInfo apiInfo) {
        if (btn != null) {
            btn.setEnabled(false);
        }

        // 发送请求前，主动结束所有相关表格的编辑，确保编辑内容写入TableModel，避免数据丢失
        if (headersPanel != null) {
            JTable table = getTableFromHeadersPanel(headersPanel);
            stopTableEditing(table);
        }
        if (cookiesPanel != null) {
            JTable table = getTableFromCookiesPanel(cookiesPanel);
            stopTableEditing(table);
        }
        if (postOpPanel != null) {
            JTable table = getTableFromPostOpPanel(postOpPanel);
            stopTableEditing(table);
        }

        EXECUTOR.submit(() -> {
            try {
                String url = apiInfo.getUrl();
                String method = apiInfo.getHttpMethod();
                java.util.List<ApiParam> params = paramsPanel != null ? paramsPanel.getParams() : new java.util.ArrayList<>();
                String bodyType = bodyPanel != null ? bodyPanel.getBodyType() : "none";
                java.util.List<ApiParam> bodyParams = bodyPanel != null ? bodyPanel.getBodyParams() : new java.util.ArrayList<>();
                String bodyContent = bodyPanel != null ? bodyPanel.getBodyContent() : "";
                byte[] binaryData = "binary".equals(bodyType) && bodyPanel != null ? bodyPanel.getBinaryData() : null;
                java.util.Map<String, String> headers = headersPanel != null ? headersPanel.getHeadersMap() : new java.util.HashMap<>();
                java.util.Map<String, String> cookies = cookiesPanel != null ? cookiesPanel.getCookiesMap() : new java.util.HashMap<>();
                String auth = "";
                if (authPanel != null && authPanel.getAuthMode() == 0) {
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else if (authPanel != null) {
                    auth = authPanel.getAuthValue();
                }
                java.util.List<PostOpItem> postOps = postOpPanel != null ? postOpPanel.getPostOpData() : new java.util.ArrayList<>();
                String urlPrefix = ProjectSettingsManager.getCurrentEnvironmentPreUrl(project);
                try (HttpResponse response = RequestSender.sendRequestRaw(
                        project, url, method, params, bodyType, bodyParams, bodyContent, binaryData, headers, cookies, auth, urlPrefix, postOps
                )) {
                    int status = response.getStatus();
                    String statusMsg = "HTTP状态: " + status + (status == 200 ? "（成功）" : "（失败）");
                    String respText = response.body();
                    String finalDisplayText = formatResponseText(respText);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        responsePanel.setStatusText(statusMsg);
                        responsePanel.setResponseText(finalDisplayText);
                        responsePanel.expand();
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                    String contentType = response.header("Content-Type");
                    byte[] bytes = response.bodyBytes();
                    String ext = suggestFileExtension(contentType, bytes);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                        fileChooser.setDialogTitle("请选择保存文件的位置");
                        fileChooser.setSelectedFile(new java.io.File("response" + ext));
                        int userSelection = fileChooser.showSaveDialog(responsePanel);
                        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
                            java.io.File fileToSave = fileChooser.getSelectedFile();
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fileToSave)) {
                                fos.write(bytes);
                                fos.flush();
                                javax.swing.JOptionPane.showMessageDialog(responsePanel, "文件已保存: " + fileToSave.getAbsolutePath());
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(responsePanel, "保存文件失败: " + ex.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        if (btn != null) {
                            btn.setEnabled(true);
                        }
                    });
                }
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(responsePanel, "请求异常: " + ex.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
                    if (btn != null) {
                        btn.setEnabled(true);
                    }
                });
            }
        });
    }

    // --- 文件扩展名建议工具方法 ---
    private String suggestFileExtension(String contentType, byte[] bytes) {
        if (contentType == null) {
            return ".bin";
        }
        String ct = contentType.toLowerCase();
        if (ct.contains("json")) {
            return ".json";
        }
        if (ct.contains("xml")) {
            return ".xml";
        }
        if (ct.contains("html")) {
            return ".html";
        }
        if (ct.contains("csv")) {
            return ".csv";
        }
        if (ct.contains("plain")) {
            return ".txt";
        }
        if (ct.contains("zip")) {
            return ".zip";
        }
        if (ct.contains("pdf")) {
            return ".pdf";
        }
        if (ct.contains("msword")) {
            return ".doc";
        }
        if (ct.contains("officedocument.spreadsheet")) {
            return ".xlsx";
        }
        if (ct.contains("officedocument.wordprocessingml")) {
            return ".docx";
        }
        if (ct.contains("officedocument.presentationml")) {
            return ".pptx";
        }
        if (ct.contains("excel")) {
            return ".xls";
        }
        if (ct.contains("image/png")) {
            return ".png";
        }
        if (ct.contains("image/jpeg")) {
            return ".jpg";
        }
        if (ct.contains("image/gif")) {
            return ".gif";
        }
        if (ct.contains("image/")) {
            return ".img";
        }
        return ".bin";
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
                        "性能监控未启用。请在设置页面启用性能监控后查看报告。",
                        "提示",
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
            JButton closeButton = new JButton("关闭");
            JButton clearButton = new JButton("清除统计数据");
            JButton refreshButton = new JButton("刷新");

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
                    "获取性能报告失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 格式化响应内容，支持JSON/XML/HTML美化
     */
    private String formatResponseText(String text) {
        if (isJson(text)) {
            try {
                return cn.hutool.json.JSONUtil.formatJsonStr(text);
            } catch (Exception ignore) {
            }
        } else if (isXml(text)) {
            try {
                return cn.hutool.core.util.XmlUtil.format(text);
            } catch (Exception ignore) {
            }
        } else if (isHtml(text)) {
            try {
                return org.jsoup.Jsoup.parse(text).outerHtml();
            } catch (Exception ignore) {
            }
        }
        return text;
    }

    // 工具方法：判断字符串是否为HTML
    private boolean isHtml(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim().toLowerCase();
        return t.startsWith("<!doctype html") || t.startsWith("<html");
    }

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
     * 查找指定项目的RequestManPanel实例
     */
    public static RequestManPanel findRequestManPanel(Project project) {
        return instances.get(project);
    }
}