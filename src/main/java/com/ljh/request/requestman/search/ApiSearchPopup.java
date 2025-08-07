package com.ljh.request.requestman.search;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.util.LogUtil;
import com.ljh.request.requestman.util.PerformanceMonitor;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 接口查询弹窗UI，支持分批加载、动态过滤、无感知加载更多。
 * 只负责UI和交互，不做接口扫描和缓存。
 *
 * @author requestman
 * @Description 接口查询弹窗。
 * @date 2025/06/19 20:10
 */
public class ApiSearchPopup {
    private static final int RESULT_POPUP_WIDTH = 1400;
    private static final int RESULT_POPUP_HEIGHT = 400;
    private static final int RESULT_INPUT_POPUP_WIDTH = 700;
    private static final int PAGE_SIZE = 20;
    private final Project project;
    private final JBList<ApiSearchEntry> resultList = new JBList<>();
    private final DefaultListModel<ApiSearchEntry> listModel = new DefaultListModel<>();
    private final SearchTextField searchField;
    private DialogWrapper inputDialog;
    private JBPopup resultPopup;
    private String lastKeyword = "";
    private Future<?> scanTask;
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"ALL", "URL", "Method", "ApiName"});
    private final JCheckBox includeLibsBox = new JCheckBox("包含第三方包", false);
    private GlobalSearchScope currentScope;
    private JPanel inputPanel;
    private JPanel resultPanel;
    /**
     * 防抖定时器
     */
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ApiSearchPopup-Debounce");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> debounceFuture;
    /**
     * 方法颜色静态缓存
     */
    private static final Map<String, Color> METHOD_COLOR_MAP = Map.of(
            "GET", new Color(46, 204, 113),
            "POST", new Color(255, 152, 0),
            "PUT", new Color(52, 152, 219),
            "DELETE", new Color(231, 76, 60),
            "PATCH", new Color(155, 89, 182),
            "HEAD", new Color(26, 188, 156),
            "OPTIONS", new Color(41, 128, 185),
            "TRACE", new Color(127, 140, 141)
    );
    /**
     * 方法图标缓存
     */
    private final Map<String, Icon> methodIconCache = new HashMap<>();
    /**
     * 字体缓存 - 使用IDEA的编辑器字体并增大字号
     */
    private static Font LIST_FONT = getIdeFont();

    /**
     * 刷新字体缓存，使设置立即生效
     */
    public static void refreshFont() {
        LIST_FONT = getIdeFont();
        // 通知已打开的弹窗更新字体
        notifyFontChanged();
    }

    /**
     * 通知字体变化的静态方法
     */
    private static void notifyFontChanged() {
        // 这里可以添加通知机制，但目前简单处理
        // 用户需要重新打开搜索弹窗才能看到字体变化
    }

    /**
     * 获取默认字体大小（IDEA字体大小加6）
     */
    private static int getDefaultFontSize() {
        // 优先使用IDEA的编辑器字体大小
        Font editorFont = UIManager.getFont("EditorPane.font");
        if (editorFont != null) {
            return editorFont.getSize() + 6;
        }

        // 如果没有编辑器字体，使用标签字体大小
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.getSize() + 6;
        }

        // 最后使用默认值
        return 18;
    }

    /**
     * 获取IDEA的编辑器字体并使用配置的字号
     */
    private static Font getIdeFont() {
        // 从配置中获取字体大小，默认值为IDEA字体大小加8
        int fontSize = getDefaultFontSize();
        try {
            String fontSizeStr = PropertiesComponent.getInstance().getValue("requestman.searchFontSize", String.valueOf(fontSize));
            fontSize = Integer.parseInt(fontSizeStr);
        } catch (Exception e) {
            // 使用默认值
        }

        // 优先使用IDEA的编辑器字体
        Font editorFont = UIManager.getFont("EditorPane.font");
        if (editorFont != null) {
            return editorFont.deriveFont((float) fontSize);
        }

        // 如果没有编辑器字体，使用标签字体
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.deriveFont((float) fontSize);
        }

        // 最后使用默认字体
        return new Font("Dialog", Font.PLAIN, fontSize);
    }

    /**
     * 项目级别的缓存管理器，解决多工程缓存混淆问题
     */
    private static final ConcurrentHashMap<String, ProjectCache> projectCacheMap = new ConcurrentHashMap<>();

    /**
     * 项目缓存内部类
     */
    private static class ProjectCache {
        private final List<ApiSearchEntry> projectApisCache = new ArrayList<>();
        private final List<ApiSearchEntry> allApisCache = new ArrayList<>();
        private volatile boolean projectCacheReady = false;
        private volatile boolean allCacheReady = false;

        public ProjectCache(String projectName) {
            // 简单的缓存，不需要复杂的清理逻辑
        }
    }

    /**
     * 获取项目缓存
     */
    private ProjectCache getProjectCache() {
        ProjectCache cache = projectCacheMap.computeIfAbsent(project.getName(), ProjectCache::new);
        return cache;
    }

    // 移除复杂的缓存管理逻辑，只在项目关闭时清理缓存

    /**
     * 清理项目缓存
     */
    public static void clearProjectCache(Project project) {
        ProjectCache cache = projectCacheMap.remove(project.getName());
        if (cache != null) {
            // 移除缓存时也清理过期项
        }
    }

    /**
     * 双缓存：项目包和三方包 - 改为基于项目的实例缓存
     */

    /**
     * 构造函数中启动缓存清理任务
     */
    public ApiSearchPopup(Project project) {
        this.project = project;
        // 创建项目级别的搜索文本框，使用项目名称作为历史记录键
        this.searchField = new SearchTextField("requestman.apiSearchPopup." + project.getName());
        // 弹窗创建时读取设置，决定搜索模式
        String mode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        isInitSearchMode = "init".equals(mode) || "popup_init".equals(mode);

        // 读取第三方包设置并设置勾选框初始状态
        Boolean includeLibsObj = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);
        includeLibsBox.setSelected(includeLibsObj != null && includeLibsObj);

    }

    private int currentPage = 0;
    private final List<ApiSearchEntry> currentFiltered = new ArrayList<>();
    private AWTEventListener globalClickListener;
    private JButton filterBtn;
    /**
     * 在ApiSearchPopup类中添加批量操作标志
     */
    private boolean isBatchSelecting = false;
    private final String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
    private final JCheckBox[] methodChecks = new JCheckBox[methods.length];
    private Set<String> lastSelectedMethods = new HashSet<>();
    private JBScrollPane scrollPane;
    /**
     * 缓存上一次resultList的selected索引，用于判断是否连续两次在边界按键
     */
    private int lastArrowSelectedIndex = -1;
    // 新增：输入栏loadingIcon和loadingLabel相关成员
    private AsyncProcessIcon loadingIcon;
    private JLabel loadingLabel;
    private String lastMode = "";
    // 新增：是否初始化搜索模式
    private final boolean isInitSearchMode;

    // 添加输入法状态检测相关字段
    private boolean isInputMethodActive = false;
    private javax.swing.Timer inputMethodTimer;

    public void show() {
        inputPanel = buildInputPanel();
        // 确保 modeBox 切换时立即显示 loading
        modeBox.addActionListener(e -> {
            if (loadingIcon != null) loadingIcon.setVisible(true);
            if (loadingLabel != null) loadingLabel.setVisible(true);
            showOrUpdateResultPopup();
        });
        // 结果弹窗内容
        resultPanel = buildResultPanel();
        // 创建输入对话框
        inputDialog = new SearchDialog(project, inputPanel);
        // 设置焦点组件为搜索框的文本编辑器
        ((SearchDialog) inputDialog).setFocusComponent(searchField.getTextEditor());

        // 为输入对话框添加ESC键监听器
        inputDialog.getWindow().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // ESC键关闭弹窗
                    closeAllPopups();
                }
            }
        });
        resultPopup = null;
        String currentMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "popup_init");
        if (StringUtils.equalsAny(currentMode, "popup_init", "instant")) {
            projectApisCache(currentMode);
        }
        // 初始化输入法状态检测定时器
        inputMethodTimer = new javax.swing.Timer(500, e -> {
            // 定时器只负责重置输入法状态，不触发搜索
            isInputMethodActive = false;
        });
        inputMethodTimer.setRepeats(false);

        // 添加输入法监听器
        searchField.getTextEditor().addInputMethodListener(new java.awt.event.InputMethodListener() {
            @Override
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent event) {
                // 检查是否有已提交的文本
                if (event.getCommittedCharacterCount() > 0) {
                    // 有文本提交，输入法活动结束
                    isInputMethodActive = false;
                    if (inputMethodTimer != null) {
                        inputMethodTimer.stop();
                    }
                } else {
                    // 输入法正在组合文本，标记为活动状态
                    isInputMethodActive = true;
                    if (inputMethodTimer != null) {
                        inputMethodTimer.restart();
                    }
                }
            }

            @Override
            public void caretPositionChanged(java.awt.event.InputMethodEvent event) {
                // 光标位置变化时，也标记为活动状态
                isInputMethodActive = true;
                if (inputMethodTimer != null) {
                    inputMethodTimer.restart();
                }
            }
        });

        // 输入监听，动态刷新结果弹窗（防抖）
        searchField.getTextEditor().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (loadingIcon != null) {
                    loadingIcon.setVisible(true);
                }
                if (loadingLabel != null) {
                    loadingLabel.setVisible(true);
                }
                if (isInitSearchMode) {
                    // 初始化搜索模式：只用缓存过滤
                    showOrUpdateResultPopup();
                } else {
                    // 即时搜索：每次都重新扫描
                    debounceShowOrUpdateResultPopup();
                }
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (loadingIcon != null) loadingIcon.setVisible(true);
                if (loadingLabel != null) loadingLabel.setVisible(true);
                if (isInitSearchMode) {
                    showOrUpdateResultPopup();
                } else {
                    debounceShowOrUpdateResultPopup();
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (loadingIcon != null) {
                    loadingIcon.setVisible(true);
                }
                if (loadingLabel != null) {
                    loadingLabel.setVisible(true);
                }
                if (isInitSearchMode) {
                    showOrUpdateResultPopup();
                } else {
                    debounceShowOrUpdateResultPopup();
                }
            }
        });
        // 三方包勾选监听
        includeLibsBox.addActionListener(e -> onIncludeLibsBoxChanged());
        // 输入框按下↓时，自动跳转到结果列表并选中第一项
        searchField.getTextEditor().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && listModel.getSize() > 0) {
                    // 选中第一个非MorePlaceholder项
                    for (int i = 0; i < listModel.size(); i++) {
                        if (!(listModel.get(i) instanceof MorePlaceholder)) {
                            resultList.setSelectedIndex(i);
                            // 确保选中项可见
                            resultList.ensureIndexIsVisible(i);
                            break;
                        }
                    }
                    // 用IDEA官方焦点管理器强制切换焦点，兼容所有弹窗/平台
                    IdeFocusManager.getInstance(project).requestFocus(resultList, true);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // ESC键关闭弹窗
                    closeAllPopups();
                }
            }
        });
        // 注册全局鼠标监听器，点击非弹窗区域时关闭弹窗
        globalClickListener = event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                MouseEvent me = (MouseEvent) event;
                Component comp = me.getComponent();
                boolean inInput = SwingUtilities.isDescendingFrom(comp, inputPanel);
                boolean inResult = SwingUtilities.isDescendingFrom(comp, resultPanel);
                boolean inModeBox = SwingUtilities.isDescendingFrom(comp, modeBox);
                boolean inFilterBtn = false;
                if (filterBtn != null) {
                    inFilterBtn = SwingUtilities.isDescendingFrom(comp, filterBtn);
                }
                // modeBox弹出菜单特殊处理
                boolean isComboPopup = false;
                boolean isPopupMenu = false;
                boolean isSearchHistoryPopup = false;
                if (comp != null) {
                    for (Container c = comp.getParent(); c != null; c = c.getParent()) {
                        if (c.getClass().getName().contains("ComboPopup")) {
                            isComboPopup = true;
                        }
                        if (c instanceof javax.swing.JPopupMenu) {
                            isPopupMenu = true;
                        }
                        // 检查是否是SearchTextField的历史记录弹出框
                        if (c instanceof JBPopup || c.getClass().getName().contains("DialogWrapperPeerImpl$MyDialog")) {
                            LogUtil.debug("Component class: " + c.getClass().getName());
                            isSearchHistoryPopup = true;
                        }
                    }
                }
                if (!inInput && !inResult && !inModeBox && !isComboPopup && !inFilterBtn && !isPopupMenu && !isSearchHistoryPopup) {
                    if (inputDialog != null && inputDialog.getWindow().isVisible()) {
                        inputDialog.close(0);
                    }
                    if (resultPopup != null && resultPopup.isVisible()) {
                        resultPopup.cancel();
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalClickListener, AWTEvent.MOUSE_EVENT_MASK);
        // 全局AWTEventListener兜底，确保上下键自定义逻辑生效
        AWTEventListener keyEventListener = event -> {
            if (event instanceof KeyEvent ke) {
                if (ke.getID() == KeyEvent.KEY_RELEASED && resultList.isShowing() && resultList.isFocusOwner() && ke.getSource() == resultList) {
                    handleResultListArrowKey(ke);
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(keyEventListener, AWTEvent.KEY_EVENT_MASK);
        // 弹窗关闭时移除AWTEventListener，防止内存泄漏
        inputDialog.getWindow().addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(globalClickListener);
                Toolkit.getDefaultToolkit().removeAWTEventListener(keyEventListener);
                if (scanTask != null) {
                    scanTask.cancel(true);
                }
                listModel.clear();
                if (resultPopup != null) {
                    resultPopup.cancel();
                }
                if (!isInitSearchMode) {
                    ProjectCache projectCache = getProjectCache();
                    projectCache.projectApisCache.clear();
                    projectCache.allApisCache.clear();
                    projectCache.projectCacheReady = false;
                    projectCache.allCacheReady = false;
                }
                if (debounceExecutor != null && !debounceExecutor.isShutdown()) {
                    try {
                        debounceExecutor.shutdown();
                        if (!debounceExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                            debounceExecutor.shutdownNow();
                        }
                    } catch (InterruptedException interruptedException) {
                        debounceExecutor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
                // 清理输入法定时器
                if (inputMethodTimer != null) {
                    inputMethodTimer.stop();
                }
            }
        });
        // 居中基础上向上偏移100像素
        Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (window != null) {
            Rectangle bounds = window.getBounds();
            // 让对话框自适应内容大小
            inputDialog.getWindow().pack();
            int dialogWidth = inputDialog.getWindow().getWidth();
            int dialogHeight = inputDialog.getWindow().getHeight();
            int centerX = bounds.x + (bounds.width - dialogWidth) / 2;
            int centerY = bounds.y + (bounds.height - dialogHeight) / 2;
            // 向上偏移100像素
            int offsetY = 100;
            inputDialog.getWindow().setLocation(centerX, centerY - offsetY);
        }
        inputDialog.show();

        // 额外的焦点设置逻辑，确保搜索框获得焦点
        SwingUtilities.invokeLater(() -> {
            // 延迟设置焦点，确保对话框完全显示
            Timer focusTimer = new Timer(200, e -> {
                if (searchField != null && searchField.getTextEditor() != null) {
                    // 检查组件状态
                    LogUtil.debug("ApiSearchPopup: searchField.isVisible: " + searchField.isVisible());
                    LogUtil.debug("ApiSearchPopup: searchField.isEnabled: " + searchField.isEnabled());
                    LogUtil.debug("ApiSearchPopup: TextEditor.isVisible: " + searchField.getTextEditor().isVisible());
                    LogUtil.debug("ApiSearchPopup: TextEditor.isEnabled: " + searchField.getTextEditor().isEnabled());

                    // 尝试多种焦点设置方法
                    boolean focusResult1 = searchField.getTextEditor().requestFocusInWindow();
                    LogUtil.debug("ApiSearchPopup: requestFocusInWindow result: " + focusResult1);

                    if (!focusResult1) {
                        // 如果失败，尝试强制设置焦点
                        searchField.getTextEditor().requestFocus();
                        searchField.getTextEditor().grabFocus();
                        boolean focusResult2 = searchField.getTextEditor().requestFocusInWindow();
                        LogUtil.debug("ApiSearchPopup: forced focus result: " + focusResult2);
                    }
                }
            });
            focusTimer.setRepeats(false);
            focusTimer.start();
        });
    }

    private JPanel buildInputPanel() {
        // 顶部第一行：左侧label左对齐，右侧控件整体右对齐
        JPanel topRow = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel pathLabel = new JLabel("Enter service URL or METHOD path:");
        pathLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        // 完全移除左边距
        pathLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pathLabel.setMaximumSize(new Dimension(400, 40));
        topRow.add(pathLabel, gbc);
        // 新增loadingIcon和loadingLabel，插入到modeBox左侧
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        loadingIcon = new AsyncProcessIcon("Searching");
        loadingIcon.setVisible(false);
        loadingLabel = new JLabel("Searching...");
        loadingLabel.setVisible(false);
        JPanel loadingWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        loadingWrapper.setOpaque(false);
        loadingWrapper.add(loadingIcon);
        loadingWrapper.add(loadingLabel);
        topRow.add(loadingWrapper, gbc);
        gbc.gridx = 2;
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setMaximumSize(new Dimension(300, 40));
        modeBox.setMaximumSize(new Dimension(120, 28));
        modeBox.setPreferredSize(new Dimension(120, 28));
        rightPanel.add(modeBox);
        rightPanel.add(Box.createHorizontalStrut(12));
        includeLibsBox.setAlignmentY(Component.CENTER_ALIGNMENT);
        rightPanel.add(includeLibsBox);
        // 新增：刷新按钮
        JButton refreshBtn = new JButton(com.intellij.icons.AllIcons.Actions.Refresh);
        refreshBtn.setToolTipText("刷新接口");
        refreshBtn.setPreferredSize(new Dimension(28, 28));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setContentAreaFilled(false);
        // 先不加到rightPanel
        filterBtn = new JButton(com.intellij.icons.AllIcons.General.Filter);
        filterBtn.setToolTipText("请求方法筛选");
        filterBtn.setPreferredSize(new Dimension(32, 32));
        filterBtn.setFocusPainted(false);
        filterBtn.setBorderPainted(false);
        filterBtn.setContentAreaFilled(false);
        // 先加筛选按钮
        rightPanel.add(filterBtn);
        // 刷新按钮紧跟筛选按钮
        rightPanel.add(Box.createHorizontalStrut(4));
        rightPanel.add(refreshBtn);
        // 刷新按钮事件：根据三方包勾选状态重新扫描并刷新缓存
        refreshBtn.addActionListener(e -> {
            refreshBtn.setEnabled(false);
            if (loadingIcon != null) loadingIcon.setVisible(true);
            if (loadingLabel != null) loadingLabel.setVisible(true);
            boolean libs = includeLibsBox.isSelected();

            // 直接调用缓存方法，它会自动在完成后刷新UI
            cacheApisOnSettingSavedWithCallback(project, libs, () -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 强制重置过滤状态，确保刷新后重新过滤
                    lastSelectedMethods.clear();
                    currentPage = 0;
                    currentFiltered.clear();
                    showOrUpdateResultPopup();
                    refreshBtn.setEnabled(true);
                    if (loadingIcon != null) loadingIcon.setVisible(false);
                    if (loadingLabel != null) loadingLabel.setVisible(false);
                });
            });
        });

        // 为刷新按钮添加鼠标交互效果
        refreshBtn.addMouseListener(new MouseAdapter() {
            private boolean isMouseInside = false;

            @Override
            public void mouseEntered(MouseEvent e) {
                isMouseInside = true;
                if (!refreshBtn.getModel().isPressed()) {
                    refreshBtn.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 1, true));
                    refreshBtn.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isMouseInside = false;
                refreshBtn.setBorder(null);
                refreshBtn.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // 按下时显示更明显的边框和背景色
                refreshBtn.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2, true));
                // 半透明背景
                refreshBtn.setBackground(new Color(37, 99, 235, 30));
                refreshBtn.setOpaque(true);
                refreshBtn.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 恢复透明背景
                refreshBtn.setOpaque(false);
                refreshBtn.setBackground(null);

                if (isMouseInside) {
                    refreshBtn.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 1, true));
                } else {
                    refreshBtn.setBorder(null);
                }
                refreshBtn.repaint();
            }
        });
        topRow.add(rightPanel, gbc);
        // 完全移除边距
        topRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        // 进一步减少高度
        topRow.setMaximumSize(new Dimension(RESULT_INPUT_POPUP_WIDTH, 32));
        // 方法多选弹窗
        String[] labels = {"GET", "POST", "PUT", "DEL", "PTCH", "HEAD", "OPT", "TRC"};
        Color[] colors = {
                // GET 绿
                new Color(46, 204, 113),
                // POST 橙
                new Color(255, 152, 0),
                // PUT 蓝
                new Color(52, 152, 219),
                // DELETE 红
                new Color(231, 76, 60),
                // PATCH 紫
                new Color(155, 89, 182),
                // HEAD 青
                new Color(26, 188, 156),
                // OPTIONS 蓝灰
                new Color(41, 128, 185),
                // TRACE 灰
                new Color(127, 140, 141)
        };
        JPopupMenu filterMenu = new JPopupMenu();
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < methods.length; i++) {
            final int colorIndex = i;
            JCheckBox cb = new JCheckBox("", true);
            cb.setFont(cb.getFont().deriveFont(Font.BOLD, 14f));
            JLabel tag = new JLabel(labels[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(colors[colorIndex]);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(getText())) / 2;
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(getText(), x, y);
                    g2.dispose();
                }
            };
            tag.setPreferredSize(new Dimension(38, 22));
            tag.setFont(tag.getFont().deriveFont(Font.BOLD, 13f));
            tag.setHorizontalAlignment(SwingConstants.CENTER);
            tag.setForeground(Color.WHITE);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.setOpaque(false);
            row.add(cb);
            row.add(tag);
            row.add(Box.createHorizontalStrut(4));
            row.add(new JLabel(methods[i]));
            checkPanel.add(row);
            cb.addActionListener(e -> {
                if (!isBatchSelecting) {
                    showOrUpdateResultPopup();
                }
            });
            methodChecks[i] = cb;
        }
        filterMenu.add(checkPanel);
        // 底部按钮区
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton allBtn = new JButton("All");
        JButton noneBtn = new JButton("None");
        JButton invertBtn = new JButton("Invert");
        allBtn.addActionListener(e -> {
            isBatchSelecting = true;
            for (JCheckBox cb : methodChecks) {
                cb.setSelected(true);
            }
            isBatchSelecting = false;
            showOrUpdateResultPopup();
        });
        noneBtn.addActionListener(e -> {
            isBatchSelecting = true;
            for (JCheckBox cb : methodChecks) {
                cb.setSelected(false);
            }
            isBatchSelecting = false;
            showOrUpdateResultPopup();
        });
        invertBtn.addActionListener(e -> {
            isBatchSelecting = true;
            for (JCheckBox cb : methodChecks) {
                cb.setSelected(!cb.isSelected());
            }
            isBatchSelecting = false;
            showOrUpdateResultPopup();
        });
        btnPanel.add(allBtn);
        btnPanel.add(noneBtn);
        btnPanel.add(invertBtn);
        filterMenu.add(btnPanel);
        filterBtn.addActionListener(e -> filterMenu.show(filterBtn, 0, filterBtn.getHeight()));

        // 第二行：搜索框，宽度拉满，高度提升
        JPanel searchRow = new JPanel();
        searchRow.setLayout(new BoxLayout(searchRow, BoxLayout.X_AXIS));
        // 恢复原始宽度
        searchField.setMaximumSize(new Dimension(RESULT_INPUT_POPUP_WIDTH, 48));
        // 恢复原始宽度
        searchField.setPreferredSize(new Dimension(RESULT_INPUT_POPUP_WIDTH, 48));
        // 设置SearchTextField内部文本编辑器的字体
        searchField.getTextEditor().setFont(LIST_FONT);
        searchRow.add(searchField);
        // 完全移除边距
        searchRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        // 进一步减少高度
        searchRow.setMaximumSize(new Dimension(RESULT_INPUT_POPUP_WIDTH, 48));
        // 主面板
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(topRow);
        topPanel.add(searchRow);
        panel.add(topPanel);
        panel.setMaximumSize(new Dimension(RESULT_INPUT_POPUP_WIDTH, 96));
        return panel;
    }

    private JPanel buildResultPanel() {
        resultList.setModel(listModel);
        resultList.setEmptyText("");
        // 设置列表字体
        resultList.setFont(LIST_FONT);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof MorePlaceholder) {
                    return super.getListCellRendererComponent(list, "加载更多...", index, isSelected, cellHasFocus);
                }
                if (value == null || !(value instanceof ApiSearchEntry)) {
                    return super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
                }
                ApiSearchEntry api = (ApiSearchEntry) value;
                String keyword = searchField.getText().trim();
                String mode = (String) modeBox.getSelectedItem();
                String url = api.url != null ? api.url : "";
                String methodName = api.methodName != null ? api.methodName : "";
                String className = api.className != null ? api.className : "";
                String description = api.description != null && !api.description.isEmpty() ? api.description : methodName;
                if (isSelected && !keyword.isEmpty()) {
                    if ("ALL".equals(mode)) {
                        // ALL模式：高亮所有匹配的字段
                        url = highlight(url, keyword);
                        methodName = highlight(methodName, keyword);
                        description = highlight(description, keyword);
                    } else if ("URL".equals(mode)) {
                        url = highlight(url, keyword);
                    } else if ("Method".equals(mode)) {
                        methodName = highlight(methodName, keyword);
                    } else if ("ApiName".equals(mode)) {
                        description = highlight(description, keyword);
                    }
                }
                String display = String.format(
                        "<html>%s&nbsp;&nbsp;%s&nbsp;&nbsp;<span style='color:#888888;'>%s#%s</span></html>",
                        url,
                        description,
                        className,
                        methodName
                );
                JLabel label = (JLabel) super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                label.setFont(LIST_FONT);
                label.setForeground(UIManager.getColor("Label.foreground"));
                label.setIcon(null);
                // 方法块图标
                String method = api.httpMethod != null ? api.httpMethod.toUpperCase() : "";
                Icon icon = methodIconCache.computeIfAbsent(method, m -> new MethodIcon(m, METHOD_COLOR_MAP.getOrDefault(m, UIManager.getColor("Label.foreground"))));
                label.setIcon(icon);
                // 支持HTML渲染
                label.setText(display);
                return label;
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    Object value = resultList.getSelectedValue();
                    if (value instanceof MorePlaceholder) {
                        int idx = resultList.getSelectedIndex();
                        if (idx >= 0) {
                            listModel.removeElementAt(idx);
                        }
                        LogUtil.debug("[ApiSearchPopup] [" + now() + "] before add page, listModel size: " + listModel.getSize());
                        currentPage++;
                        showOrUpdateResultPopup();
                        LogUtil.debug("[ApiSearchPopup] [" + now() + "] after add page, listModel size: " + listModel.getSize());
                        return;
                    }
                }
                if (e.getClickCount() == 2) {
                    ApiSearchEntry api = resultList.getSelectedValue();
                    jumpToApi(api);
                }
            }
        });
        // 保留回车KeyListener
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Object value = resultList.getSelectedValue();
                    int selected = resultList.getSelectedIndex();
                    if (value instanceof MorePlaceholder) {
                        if (selected >= 0) {
                            listModel.removeElementAt(selected);
                        }
                        currentPage++;
                        showOrUpdateResultPopup();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            int newSize = listModel.getSize();
                            if (newSize > selected) {
                                resultList.setSelectedIndex(selected);
                                resultList.ensureIndexIsVisible(selected);
                            } else if (newSize > 0) {
                                resultList.setSelectedIndex(newSize - 1);
                                resultList.ensureIndexIsVisible(newSize - 1);
                            }
                        });
                        e.consume();
                        return;
                    }
                    ApiSearchEntry api = resultList.getSelectedValue();
                    jumpToApi(api);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // ESC键关闭弹窗
                    closeAllPopups();
                }
            }
        });
        scrollPane = new JBScrollPane(resultList);
        scrollPane.setPreferredSize(new Dimension(RESULT_POPUP_WIDTH, RESULT_POPUP_HEIGHT));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(RESULT_POPUP_WIDTH, RESULT_POPUP_HEIGHT));
        return panel;
    }

    private void showOrUpdateResultPopup() {
        // 如果输入法正在活动，暂停搜索
        if (isInputMethodActive) {
            return;
        }

        logMemory("before showOrUpdateResultPopup");
        LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size before clear: " + listModel.getSize());
        ProjectCache projectCache = getProjectCache();
        List<ApiSearchEntry> source;
        boolean ready;
        if (includeLibsBox.isSelected()) {
            source = projectCache.allApisCache;
            ready = projectCache.allCacheReady;
        } else {
            source = projectCache.projectApisCache;
            ready = projectCache.projectCacheReady;
        }
        // 控制输入栏loadingIcon和loadingLabel显示/隐藏
        if (!ready) {
            if (loadingIcon != null) {
                loadingIcon.setVisible(true);
            }
            if (loadingLabel != null) {
                loadingLabel.setVisible(true);
            }
            // 不再插入LoadingPlaceholder到结果区
            return;
        }
        LogUtil.debug("[ApiSearchPopup] [" + now() + "] 开始");

        Set<String> selectedMethods = new HashSet<>();
        for (int i = 0; i < methods.length; i++) {
            if (methodChecks[i] != null && methodChecks[i].isSelected()) {
                selectedMethods.add(methods[i]);
            }
        }
        String keyword = searchField.getText().trim();
        String mode = (String) modeBox.getSelectedItem();
        boolean modeChanged = !mode.equals(lastMode);
        boolean keywordChanged = !keyword.equals(lastKeyword);
        boolean methodChanged = !selectedMethods.equals(lastSelectedMethods);
        // 输入、请求类型或URL下拉变化时重置页码和快照
        if (keywordChanged || methodChanged || modeChanged) {
            currentPage = 0;
            lastKeyword = keyword;
            lastSelectedMethods = new HashSet<>(selectedMethods);
            lastMode = mode;
        }
        if (StringUtils.isBlank(lastKeyword)) {
            currentFiltered.clear();
            if (resultPopup != null && resultPopup.isVisible()) {
                resultPopup.cancel();
            }
            return;
        }
        // 本地过滤
        if (keywordChanged || methodChanged || modeChanged) {
            currentFiltered.clear();
            if (StringUtils.isBlank(lastKeyword)) {
                if (resultPopup != null && resultPopup.isVisible()) {
                    resultPopup.cancel();
                    return;
                }
            }
            if (scrollPane != null) {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                if (verticalBar != null) {
                    verticalBar.setValue(verticalBar.getMinimum());
                }
            }
            for (ApiSearchEntry api : source) {
                boolean matches = false;

                if ("ALL".equals(mode)) {
                    // ALL模式：检查URL、Method、ApiName三个字段
                    boolean urlMatch = org.apache.commons.lang3.StringUtils.isNoneBlank(api.url) && api.url.toLowerCase().contains(keyword.toLowerCase());
                    boolean methodMatch = org.apache.commons.lang3.StringUtils.isNoneBlank(api.methodName) && api.methodName.toLowerCase().contains(keyword.toLowerCase());
                    boolean apiNameMatch = org.apache.commons.lang3.StringUtils.isNoneBlank(api.description) && api.description.toLowerCase().contains(keyword.toLowerCase());
                    matches = urlMatch || methodMatch || apiNameMatch;
                } else if ("URL".equals(mode)) {
                    // URL模式：只检查URL字段
                    matches = org.apache.commons.lang3.StringUtils.isNoneBlank(api.url) && api.url.toLowerCase().contains(keyword.toLowerCase());
                } else if ("Method".equals(mode)) {
                    // Method模式：只检查方法名字段
                    matches = org.apache.commons.lang3.StringUtils.isNoneBlank(api.methodName) && api.methodName.toLowerCase().contains(keyword.toLowerCase());
                } else if ("ApiName".equals(mode)) {
                    // ApiName模式：只检查description字段（API名称）
                    matches = org.apache.commons.lang3.StringUtils.isNoneBlank(api.description) && api.description.toLowerCase().contains(keyword.toLowerCase());
                }

                // 检查HTTP方法过滤
                if (matches && api.httpMethod != null && selectedMethods.contains(api.httpMethod.toUpperCase())) {
                    currentFiltered.add(api);
                }
            }
            logMemory("before listModel.clear (ready)");
            listModel.clear();
            logMemory("after listModel.clear (ready)");
            LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size after clear (ready): " + listModel.getSize());
        }
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(currentFiltered.size(), from + PAGE_SIZE);
        // 累加分页：只在第一页时加载第一页，否则追加新一页
        int addFrom = keywordChanged ? 0 : from;
        int addTo = to;
        for (int i = addFrom; i < addTo; i++) {
            listModel.addElement(currentFiltered.get(i));
        }
        LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size after addElement: " + listModel.getSize());
        if (to < currentFiltered.size()) {
            listModel.addElement(new MorePlaceholder() {
                public String toString() {
                    return "...";
                }
            });
            LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size after add more: " + listModel.getSize());
        }
        logMemory("after listModel.addElement (ready)");
        // 只有在缓存加载完且无结果时才变红
        if (currentFiltered.isEmpty()) {
            searchField.getTextEditor().setForeground(Color.RED);
            if (resultPopup != null && resultPopup.isVisible()) {
                resultPopup.cancel();
            }
        } else {
            searchField.getTextEditor().setForeground(UIManager.getColor("TextField.foreground"));
            // 弹窗显示逻辑
            try {
                Component comp = inputDialog.getWindow();
                int inputWidth = comp.getWidth();
                int resultWidth = RESULT_POPUP_WIDTH;
                int xOffset = (inputWidth - resultWidth) / 2;
                if (xOffset < 0) {
                    xOffset = 0;
                }
                if (resultPopup == null || !resultPopup.isVisible()) {
                    resultPopup = JBPopupFactory.getInstance()
                            // 第二参数用resultPanel，避免resultList自动获得焦点
                            .createComponentPopupBuilder(resultPanel, resultPanel)
                            .setTitle(null)
                            .setResizable(false)
                            .setMovable(false)
                            .setRequestFocus(true)
                            .setFocusable(true)
                            .setCancelOnClickOutside(false)
                            .setCancelOnWindowDeactivation(true) // 允许失去窗口焦点时关闭弹窗
                            .createPopup();
                    resultPopup.show(new com.intellij.ui.awt.RelativePoint(comp, new Point(xOffset, comp.getHeight() - 2)));
                    IdeFocusManager.getInstance(project).requestFocus(searchField.getTextEditor(), true);
                }
                // 默认选中第一行（非MorePlaceholder）
                for (int i = 0; i < listModel.size(); i++) {
                    if (!(listModel.get(i) instanceof MorePlaceholder)) {
                        resultList.setSelectedIndex(i);
                        break;
                    }
                }
                LogUtil.debug("[ApiSearchPopup] [" + now() + "] resultPopup show (ready), isVisible: " + (resultPopup != null && resultPopup.isVisible()));
            } catch (Exception ignore) {
            }
            logMemory("after popup show (ready)");
        }
        if (loadingIcon != null) {
            loadingIcon.setVisible(false);
        }
        if (loadingLabel != null) {
            loadingLabel.setVisible(false);
        }
        logMemory("after showOrUpdateResultPopup");
        LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size at end of showOrUpdateResultPopup: " + listModel.getSize());
    }

    // 新增：即时搜索模式下，异步线程回调UI渲染
    private void updateListModelWithSource(List<ApiSearchEntry> source) {
        Set<String> selectedMethods = new HashSet<>();
        for (int i = 0; i < methods.length; i++) {
            if (methodChecks[i] != null && methodChecks[i].isSelected()) {
                selectedMethods.add(methods[i]);
            }
        }
        String keyword = searchField.getText().trim();
        String mode = (String) modeBox.getSelectedItem();
        boolean modeChanged = !mode.equals(lastMode);
        boolean keywordChanged = !keyword.equals(lastKeyword);
        boolean methodChanged = !selectedMethods.equals(lastSelectedMethods);
        if (keywordChanged || methodChanged || modeChanged) {
            currentPage = 0;
            lastKeyword = keyword;
            lastSelectedMethods = new java.util.HashSet<>(selectedMethods);
            lastMode = mode;
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(lastKeyword)) {
            currentFiltered.clear();
            if (resultPopup != null && resultPopup.isVisible()) {
                resultPopup.cancel();
            }
            return;
        }
        // 本地过滤
        currentFiltered.clear();
        if (org.apache.commons.lang3.StringUtils.isBlank(lastKeyword)) {
            if (resultPopup != null && resultPopup.isVisible()) {
                resultPopup.cancel();
                return;
            }
        }
        if (scrollPane != null) {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            if (verticalBar != null) {
                verticalBar.setValue(verticalBar.getMinimum());
            }
        }
        for (ApiSearchEntry api : source) {
            String urlOrMethod = "";
            if ("URL".equals(mode)) {
                urlOrMethod = api.url;
            } else {
                urlOrMethod = api.methodName;
            }
            if (org.apache.commons.lang3.StringUtils.isNoneBlank(urlOrMethod) && api.httpMethod != null && urlOrMethod.toLowerCase().contains(keyword.toLowerCase()) && selectedMethods.contains(api.httpMethod.toUpperCase())) {
                currentFiltered.add(api);
            }
        }
        logMemory("before listModel.clear (ready)");
        listModel.clear();
        logMemory("after listModel.clear (ready)");
        LogUtil.debug("[ApiSearchPopup] [" + now() + "] listModel size after clear (ready): " + listModel.getSize());
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(currentFiltered.size(), from + PAGE_SIZE);
        int addFrom = keywordChanged ? 0 : from;
        int addTo = to;
        for (int i = addFrom; i < addTo; i++) {
            listModel.addElement(currentFiltered.get(i));
        }
        if (to < currentFiltered.size()) {
            listModel.addElement(new MorePlaceholder());
        }
        if (loadingIcon != null) {
            loadingIcon.setVisible(false);
        }
        if (loadingLabel != null) {
            loadingLabel.setVisible(false);
        }
    }

    private void jumpToApi(ApiSearchEntry api) {
        if (api == null || api instanceof MorePlaceholder) {
            return;
        }
        DumbService.getInstance(project).runWhenSmart(() -> {
            ReadAction.nonBlocking(() -> {
                // --- 慢操作部分：PSI查找和类型匹配 ---
                PsiMethod targetMethod = null;
                if (api.className != null && !api.className.isEmpty() && api.methodName != null && !api.methodName.isEmpty()) {
                    PsiClass psiClass = com.intellij.psi.JavaPsiFacade.getInstance(project)
                            .findClass(api.className, GlobalSearchScope.allScope(project));
                    if (psiClass != null) {
                        for (PsiMethod method : psiClass.getMethods()) {
                            if (method.getName().equals(api.methodName) && methodParamTypesMatch(method, api.paramTypes)) {
                                targetMethod = method;
                                break;
                            }
                        }
                    }
                }
                return targetMethod;
            }).finishOnUiThread(ModalityState.any(), method -> {
                // --- UI线程部分：导航和弹窗操作 ---
                if (method != null) {
                    com.intellij.openapi.vfs.VirtualFile vFile = com.intellij.psi.util.PsiUtilCore.getVirtualFile(method);
                    if (vFile != null) {
                        int offset = method.getTextOffset();
                        new com.intellij.openapi.fileEditor.OpenFileDescriptor(project, vFile, offset).navigate(true);
                        if (inputDialog != null && inputDialog.getWindow().isVisible()) {
                            inputDialog.close(0);
                        }
                        if (resultPopup != null && resultPopup.isVisible()) {
                            resultPopup.cancel();
                        }
                    }
                } else {
                    com.intellij.openapi.ui.Messages.showInfoMessage(project, "未找到对应方法，可能已被删除或重命名。", "跳转失败");
                }
            }).submit(AppExecutorUtil.getAppExecutorService());
        });
    }

    /**
     * "..."占位符，仅用于UI展示
     */
    private static class MorePlaceholder extends ApiSearchEntry {
        public MorePlaceholder() {
            super("", "", "", "", "", java.util.Collections.emptyList());
        }

        @Override
        public String toString() {
            return "...";
        }
    }

    /**
     * 搜索加载中占位项
     */
    private static class LoadingPlaceholder extends ApiSearchEntry {
        public LoadingPlaceholder() {
            super("", "", "", "", "", java.util.Collections.emptyList());
        }

        @Override
        public String toString() {
            return "Searching...";
        }
    }

    // 方法块图标实现
    class MethodIcon implements Icon {
        private final String method;
        private final Color color;

        public MethodIcon(String method, Color color) {
            this.method = method != null ? method.toUpperCase() : "";
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int width = getIconWidth();
            int height = getIconHeight();
            g2.fillRoundRect(x, y + 2, width, height - 4, 10, 10);
            g2.setColor(Color.WHITE);
            Font font = c.getFont().deriveFont(Font.BOLD, 12f);
            g2.setFont(font);
            FontMetrics fm = c.getFontMetrics(font);
            int strWidth = fm.stringWidth(method);
            int strX = x + (width - strWidth) / 2;
            int strY = y + height / 2 + fm.getAscent() / 2 - 2;
            g2.drawString(method, strX, strY);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 52; // 统一宽度，保证所有方法块对齐
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    }

    // 防抖触发方法
    private void debounceShowOrUpdateResultPopup() {
        // 如果输入法正在活动，暂停搜索
        if (isInputMethodActive) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            listModel.clear();
            listModel.addElement(new LoadingPlaceholder());
        });
        if (debounceFuture != null && !debounceFuture.isDone()) {
            debounceFuture.cancel(false);
        }
        debounceFuture = debounceExecutor.schedule(() -> {
            ApplicationManager.getApplication().invokeLater(this::showOrUpdateResultPopup);
        }, 300, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // 三方包勾选切换
    private void onIncludeLibsBoxChanged() {
        ProjectCache projectCache = getProjectCache();
        if (includeLibsBox.isSelected() && projectCache.allCacheReady) {
            return;
        }
        if (!includeLibsBox.isSelected() && projectCache.projectCacheReady) {
            return;
        }
        // 强制重置过滤状态，确保刷新后重新过滤
        lastSelectedMethods.clear();
        currentPage = 0;
        currentFiltered.clear();
        projectApisCache(null);
    }


    /**
     * 处理resultList的自定义上下键逻辑，兼容IDEA弹窗/JBList焦点环境。
     *
     * @param ke 键盘事件
     */
    private void handleResultListArrowKey(KeyEvent ke) {
        int selected = resultList.getSelectedIndex();
        int size = listModel.getSize();
        // 上键到顶，且上一次也在顶，才回输入框
        if (ke.getKeyCode() == KeyEvent.VK_UP
                && selected == 0
                && lastArrowSelectedIndex == 0
                && resultList.getSelectedValue() != null) {
            ke.consume();
            IdeFocusManager.getInstance(project)
                    .requestFocus(searchField.getTextEditor(), true);
            resultList.clearSelection();
        }
        // 下键到MorePlaceholder，且上一次也在MorePlaceholder，才加载更多
        else if (ke.getKeyCode() == KeyEvent.VK_DOWN
                && selected == size - 1
                && lastArrowSelectedIndex == size - 1
                && size > 0
                && listModel.getElementAt(size - 1) instanceof MorePlaceholder) {
            ke.consume();
            listModel.removeElementAt(size - 1);
            currentPage++;
            showOrUpdateResultPopup();
            ApplicationManager.getApplication().invokeLater(() -> {
                int newSize = listModel.getSize();
                if (newSize > 0 && listModel.getElementAt(newSize - 1) instanceof MorePlaceholder) {
                    resultList.setSelectedIndex(selected);
                    resultList.ensureIndexIsVisible(selected);
                } else if (newSize > 0) {
                    int firstNew = Math.max(0, newSize - PAGE_SIZE);
                    resultList.setSelectedIndex(firstNew);
                    resultList.ensureIndexIsVisible(firstNew);
                }
            });
        }
        // 缓存本次selected
        lastArrowSelectedIndex = selected;
        // 其它情况不处理，让JList默认行为生效
    }

    // 内存日志工具
    private void logMemory(String tag) {
        long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        LogUtil.debug("[ApiSearchPopup][" + now() + "][" + tag + "] Used memory: " + used + " MB");
    }

    /**
     * 轻量级接口查询专用数据结构，避免冗余字段和内存浪费
     */
    private static class ApiSearchEntry {
        final String url;
        final String httpMethod;
        final String methodName;
        final String className;
        final String description;
        final List<String> paramTypes;
        final long timestamp;

        ApiSearchEntry(String url, String httpMethod, String methodName, String className, String description, List<String> paramTypes) {
            this.url = url;
            this.httpMethod = httpMethod;
            this.methodName = methodName;
            this.className = className;
            this.description = description;
            this.paramTypes = paramTypes != null ? paramTypes : java.util.Collections.emptyList();
            this.timestamp = System.currentTimeMillis();
        }

        String getDisplayText() {
            return String.format("<html>%s&nbsp;&nbsp;%s&nbsp;&nbsp;<span style='color:#888888;'>%s#%s</span></html>",
                    url != null ? url : "",
                    description != null && !description.isEmpty() ? description : (methodName != null ? methodName : ""),
                    className != null ? className : "",
                    methodName != null ? methodName : "");
        }

        @Override
        public String toString() {
            return (methodName != null ? methodName : "") + "（" + (url != null ? url : "") + ")";
        }
    }

    // 在ApiSearchPopup类中添加highlight方法
    private String highlight(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return text;
        }
        String lower = text.toLowerCase();
        String kw = keyword.toLowerCase();
        int idx = lower.indexOf(kw);
        if (idx >= 0) {
            return text.substring(0, idx)
                    + "<span style='background:yellow;font-weight:bold;color:#000;'>"
                    + text.substring(idx, idx + keyword.length())
                    + "</span>"
                    + text.substring(idx + keyword.length());
        }
        return text;
    }

    // 在ApiSearchPopup类中添加methodParamTypesMatch方法
    private boolean methodParamTypesMatch(com.intellij.psi.PsiMethod method, List<String> paramTypes) {
        com.intellij.psi.PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length != paramTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameters.length; i++) {
            String psiType = parameters[i].getType().getCanonicalText();
            if (!psiType.equals(paramTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    // 在类字段区添加一个时间格式化方法
    private static String now() {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    /**
     * 设置页保存或插件初始化时调用，扫描接口并缓存到内存（实例变量）。
     * 添加缓存大小限制和性能优化。
     *
     * @param project     项目对象
     * @param includeLibs 是否包含三方包
     */
    public static void cacheApisOnSettingSaved(Project project, boolean includeLibs) {
        cacheApisOnSettingSavedWithCallback(project, includeLibs, null);
    }

    /**
     * 带回调的缓存方法，避免轮询等待
     */
    public static void cacheApisOnSettingSavedWithCallback(Project project, boolean includeLibs, Runnable callback) {
        ProjectCache projectCache = projectCacheMap.computeIfAbsent(project.getName(), ProjectCache::new);

        if (includeLibs) {
            projectCache.allApisCache.clear();
            projectCache.allCacheReady = false;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    List<ApiInfo> allRaw = ProjectApiScanner.scanApis(
                            project,
                            "", 0, Integer.MAX_VALUE, "URL", com.intellij.psi.search.GlobalSearchScope.allScope(project), false);
                    List<ApiSearchEntry> all = new java.util.ArrayList<>();
                    for (ApiInfo info : allRaw) {
                        all.add(new ApiSearchEntry(info.getUrl(), info.getHttpMethod(), info.getMethodName(), info.getClassName(), info.getNameOrDescription(), info.getParamTypes()));
                    }
                    projectCache.allApisCache.clear();
                    projectCache.allApisCache.addAll(all);
                    projectCache.allCacheReady = true;
                    LogUtil.info("[ApiSearchPopup] [" + now() + "] All APIs cached for project " + project.getName() + ": " + all.size());

                    // 更新性能统计
                    try {
                        PerformanceMonitor.updatePluginCacheSize(projectCache.allApisCache.size());
                        PerformanceMonitor.updatePluginApiCount(all.size());
                    } catch (Exception e) {
                        // 静默处理性能统计异常
                    }
                    // 执行回调
                    if (callback != null) {
                        callback.run();
                    }
                } catch (Exception e) {
                    LogUtil.error("[ApiSearchPopup] Error caching all APIs for project " + project.getName() + ": " + e.getMessage(), e);
                    // 即使出错也要执行回调
                    if (callback != null) {
                        callback.run();
                    }
                }
            });
        } else {
            projectCache.projectApisCache.clear();
            projectCache.projectCacheReady = false;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    List<ApiInfo> allRaw = ProjectApiScanner.scanApis(
                            project,
                            "", 0, Integer.MAX_VALUE, "URL", com.intellij.psi.search.GlobalSearchScope.projectScope(project), false);
                    List<ApiSearchEntry> all = new java.util.ArrayList<>();
                    for (ApiInfo info : allRaw) {
                        all.add(new ApiSearchEntry(info.getUrl(), info.getHttpMethod(), info.getMethodName(), info.getClassName(), info.getNameOrDescription(), info.getParamTypes()));
                    }
                    projectCache.projectApisCache.clear();
                    projectCache.projectApisCache.addAll(all);
                    projectCache.projectCacheReady = true;
                    LogUtil.info("[ApiSearchPopup] [" + now() + "] Project APIs cached for project " + project.getName() + ": " + all.size());
                    // 更新性能统计
                    try {
                        PerformanceMonitor.updatePluginCacheSize(projectCache.projectApisCache.size());
                        PerformanceMonitor.updatePluginApiCount(all.size());
                    } catch (Exception e) {
                        // 静默处理性能统计异常
                    }
                    // 执行回调
                    if (callback != null) {
                        callback.run();
                    }
                } catch (Exception e) {
                    LogUtil.error("[ApiSearchPopup] Error caching project APIs for project " + project.getName() + ": " + e.getMessage(), e);
                    // 即使出错也要执行回调
                    if (callback != null) {
                        callback.run();
                    }
                }
            });
        }
    }

    /**
     * 初始化扫描缓存
     */
    private void projectApisCache(String currentMode) {
        // 弹窗初始化搜索模式：弹窗首次打开时扫描一次，缓存后复用
        ProjectCache projectCache = getProjectCache();
        if ("popup_init".equals(currentMode)) {
            if (includeLibsBox.isSelected() && projectCache.allCacheReady) {
                return;
            }
            if (!includeLibsBox.isSelected() && projectCache.projectCacheReady) {
                return;
            }
        }

        // 显示加载图标
        if (loadingIcon != null) {
            loadingIcon.setVisible(true);
        }
        if (loadingLabel != null) {
            loadingLabel.setVisible(true);
        }

        projectCache.projectApisCache.clear();
        projectCache.allApisCache.clear();
        projectCache.projectCacheReady = false;
        projectCache.allCacheReady = false;
        logMemory("before projectApisCache赋值");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 根据第三方包勾选状态决定扫描范围
            com.intellij.psi.search.GlobalSearchScope searchScope = includeLibsBox.isSelected()
                    ? com.intellij.psi.search.GlobalSearchScope.allScope(project)
                    : com.intellij.psi.search.GlobalSearchScope.projectScope(project);

            List<ApiInfo> allRaw = ProjectApiScanner.scanApis(project, "", 0, Integer.MAX_VALUE, (String) modeBox.getSelectedItem(), searchScope, false);
            List<ApiSearchEntry> all = new java.util.ArrayList<>();
            for (ApiInfo info : allRaw) {
                all.add(new ApiSearchEntry(info.getUrl(), info.getHttpMethod(), info.getMethodName(), info.getClassName(), info.getNameOrDescription(), info.getParamTypes()));
            }

            // 根据扫描范围决定存储到哪个缓存
            if (includeLibsBox.isSelected()) {
                // 扫描包含第三方包，存储到 allApisCache
                projectCache.allApisCache.clear();
                projectCache.allApisCache.addAll(all);
                projectCache.allCacheReady = true;
                LogUtil.debug("[ApiSearchPopup] [" + now() + "] allApisCache size: " + projectCache.allApisCache.size());
                // 更新性能统计
                try {
                    PerformanceMonitor.updatePluginCacheSize(projectCache.allApisCache.size());
                    PerformanceMonitor.updatePluginApiCount(all.size());
                } catch (Exception e) {
                    // 静默处理性能统计异常
                }
            } else {
                // 只扫描项目内，存储到 projectApisCache
                projectCache.projectApisCache.clear();
                projectCache.projectApisCache.addAll(all);
                projectCache.projectCacheReady = true;
                LogUtil.debug("[ApiSearchPopup] [" + now() + "] projectApisCache size: " + projectCache.projectApisCache.size());
                // 更新性能统计
                try {
                    PerformanceMonitor.updatePluginCacheSize(projectCache.projectApisCache.size());
                    PerformanceMonitor.updatePluginApiCount(all.size());
                } catch (Exception e) {
                    // 静默处理性能统计异常
                }
            }

            long totalStrLen = all.stream().mapToLong(api -> api.getDisplayText() != null ? api.getDisplayText().length() : 0).sum();
            LogUtil.debug("[ApiSearchPopup] [" + now() + "] total displayText length: " + totalStrLen);
            int maxLen = all.stream().mapToInt(api -> api.getDisplayText() != null ? api.getDisplayText().length() : 0).max().orElse(0);
            LogUtil.debug("[ApiSearchPopup] [" + now() + "] max displayText length: " + maxLen);
            if (!all.isEmpty()) {
                ApiSearchEntry sample = all.get(0);
                LogUtil.debug("[ApiSearchPopup] [" + now() + "] sample ApiInfo: " + sample);
            }

            // 在UI线程中隐藏加载图标并更新结果
            ApplicationManager.getApplication().invokeLater(() -> {
                // 隐藏加载图标
                if (loadingIcon != null) {
                    loadingIcon.setVisible(false);
                }
                if (loadingLabel != null) {
                    loadingLabel.setVisible(false);
                }
                // 显示或更新结果弹窗
                showOrUpdateResultPopup();
            });
        });
    }

    /**
     * 关闭所有弹窗
     */
    private void closeAllPopups() {
        if (inputDialog != null && inputDialog.getWindow().isVisible()) {
            inputDialog.close(0);
        }
        if (resultPopup != null && resultPopup.isVisible()) {
            resultPopup.cancel();
        }
    }

    /**
     * 获取缓存统计信息，用于调试
     */
    public static String getCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== RequestMan 缓存统计 ===\n");
        stats.append("项目缓存数量: ").append(projectCacheMap.size()).append("\n");

        for (java.util.Map.Entry<String, ProjectCache> entry : projectCacheMap.entrySet()) {
            String projectName = entry.getKey();
            ProjectCache cache = entry.getValue();
            stats.append("项目: ").append(projectName).append("\n");
            stats.append("  项目接口缓存: ").append(cache.projectApisCache.size()).append(" 条\n");
            stats.append("  全量接口缓存: ").append(cache.allApisCache.size()).append(" 条\n");
            stats.append("  项目缓存就绪: ").append(cache.projectCacheReady).append("\n");
            stats.append("  全量缓存就绪: ").append(cache.allCacheReady).append("\n");
        }

        return stats.toString();
    }
} 