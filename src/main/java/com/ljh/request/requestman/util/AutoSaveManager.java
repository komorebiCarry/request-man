package com.ljh.request.requestman.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.ui.ParamsTablePanel;
import com.ljh.request.requestman.util.ApiParamFlattener;
import cn.hutool.json.JSONUtil;
import com.ljh.request.requestman.util.RequestManBundle;
import com.ljh.request.requestman.ui.BodyPanel;
import com.ljh.request.requestman.ui.CookiesPanel;
import com.ljh.request.requestman.ui.HeadersPanel;
import com.ljh.request.requestman.ui.PostOpPanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author leijianhui
 * @Description 自动保存管理器，负责跟踪未保存状态和自动保存功能。
 * @date 2025/01/27 10:30
 */
public class AutoSaveManager {
    /**
     * 防抖延迟时间（毫秒）
     */
    private static final int DEBOUNCE_DELAY = 1000;

    /**
     * 内容变更检测防抖延迟（毫秒），用于减少频繁调用checkForRealChanges
     */
    private static final int CHANGE_CHECK_DEBOUNCE_DELAY = 300;

    /**
     * 当前项目
     */
    private final Project project;

    /**
     * 是否启用自动保存
     */
    private boolean autoSaveEnabled;

    /**
     * 是否有未保存的更改
     */
    private boolean hasUnsavedChanges = false;

    /**
     * 当前编辑的接口（自定义模式）
     */
    private CustomApiInfo currentEditingApi;

    /**
     * 当前编辑的扫描接口（扫描模式）
     */
    private ApiInfo currentScanningApi;

    /**
     * 当前模式（true=自定义模式，false=扫描模式）
     */
    private boolean isCustomMode = true;

    /**
     * 保存回调函数（自定义模式）
     */
    private Consumer<CustomApiInfo> saveCallback;

    /**
     * 保存回调函数（扫描模式）
     */
    private Consumer<ApiInfo> scanSaveCallback;

    /**
     * UI更新回调函数
     */
    private Runnable uiUpdateCallback;

    /**
     * URL更新回调函数（用于Path参数变化时更新URL）
     */
    private Runnable urlUpdateCallback;

    /**
     * 获取本地缓存的回调函数
     */
    private java.util.function.Supplier<Map<String, Object>> localCacheSupplier;

    /**
     * 防抖定时器
     */
    private ScheduledFuture<?> debounceTimer;

    /**
     * 内容变更检测防抖定时器
     */
    private ScheduledFuture<?> changeCheckDebounceTimer;

    /**
     * 表格监听立即更新开关（仅在当前事件线程内生效）。
     * 当为 TRUE 时，表格监听将绕过防抖，立即更新 currentContent 并同步计算未保存状态。
     */
    private static final ThreadLocal<Boolean> immediateMode = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 在一次性“立即更新”模式下运行给定任务。仅影响本线程内表格监听的本次执行，任务结束后自动还原。
     *
     * @param runnable 需要在立即模式下执行的任务
     */
    public void runWithImmediateTableUpdate(Runnable runnable) {
        immediateMode.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            immediateMode.set(Boolean.FALSE);
        }
    }

    /**
     * 立即更新内容并同步检查是否有真实变化（跳过防抖）。
     *
     * @param field 字段名
     * @param value 字段值
     */
    public void updateContentImmediately(String field, String value) {
        if (!currentContent.containsKey(field)) {
            return;
        }
        currentContent.put(field, value);

        if (!isCustomMode && currentScanningApi != null) {
            Map<String, Object> cache = getLocalCache();
            cache.put(field, value);
        }

        if (changeCheckDebounceTimer != null) {
            changeCheckDebounceTimer.cancel(false);
        }

        // 直接计算未保存状态；UI刷新已在 checkForRealChanges 内部通过 invokeLater 处理
        checkForRealChanges();
    }

    /**
     * 位置变化
     */
    private static boolean locationChange = false;


    /**
     * 定时器执行器
     */
    private final ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AutoSaveManager-Scheduler");
        t.setDaemon(true);
        return t;
    });

    /**
     * 存储原始内容，用于比较是否有真实变化
     */
    private Map<String, String> originalContent = new HashMap<>();

    /**
     * 存储当前内容，用于比较
     */
    private Map<String, String> currentContent = new HashMap<>();

    public AutoSaveManager(Project project) {
        this.project = project;
        this.autoSaveEnabled = PropertiesComponent.getInstance().getBoolean("requestman.autoSave", false);
    }

    /**
     * 设置保存回调函数（自定义模式）
     */
    public void setSaveCallback(Consumer<CustomApiInfo> saveCallback) {
        this.saveCallback = saveCallback;
    }

    /**
     * 设置扫描模式保存回调函数
     */
    public void setScanSaveCallback(Consumer<ApiInfo> scanSaveCallback) {
        this.scanSaveCallback = scanSaveCallback;
    }

    /**
     * 设置UI更新回调函数
     */
    public void setUiUpdateCallback(Runnable uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

    /**
     * 设置URL更新回调函数
     */
    public void setUrlUpdateCallback(Runnable urlUpdateCallback) {
        this.urlUpdateCallback = urlUpdateCallback;
    }

    /**
     * 设置本地缓存获取回调函数
     */
    public void setLocalCacheSupplier(Supplier<ApiInfo> apiInfoSupplier) {
        ApiInfo apiInfo = apiInfoSupplier.get();
        if(apiInfo == null) {
            this.localCacheSupplier = HashMap::new;
            return;
        }
        Map<String, Object> content = new HashMap<>();
        content.put("name", nullToEmpty(apiInfo.getName()));
        content.put("url", nullToEmpty(apiInfo.getUrl()));
        content.put("method", nullToEmpty(apiInfo.getHttpMethod()));
        // headers 原始值取自扫描对象（如果可用），否则为空
        content.put("headers", getJsonHeaders(apiInfo.getHeaders()));
        content.put("param", getJsonParam(apiInfo.getParams()));
        String bodyType = apiInfo.getBodyType();
        if (StringUtils.isBlank(bodyType)) {
            if (!apiInfo.getBodyParams().isEmpty()) {
                content.put("bodyType", BodyPanel.guessDefaultBodyType(apiInfo.getBodyParams()));
            } else {
                content.put("bodyType", "none");
            }
        } else {
            content.put("bodyType", bodyType);
        }
        String body = "";
        String bodyParams = "";
        if (!StringUtils.equalsAny(bodyType, "form-data", "x-www-form-urlencoded")) {
            body = ApiInfoExtractor.getApiInfoBodyJson(apiInfo.getBodyParams());
        } else {
            bodyParams = getJsonBodyParams(apiInfo.getBodyParams());
        }
        // body 原始内容在扫描数据中通常为空字符串，由编辑产生
        content.put("body", body);
        // 结构化Body参数（用于与表格监听保持一致的字段）
        content.put("bodyParams", bodyParams);
        content.put("description", nullToEmpty(apiInfo.getDescription()));
        // 认证与bodyType默认为空/none
        content.put("authMode", String.valueOf(apiInfo.getAuthMode()));
        content.put("authValue", nullToEmpty(apiInfo.getAuthValue()));
        // 扫描模式下postOp
        content.put("postOp", getJsonPostOp(apiInfo.getPostOps()));
        content.put("cookie", getJsonCookie(apiInfo.getCookieItems()));
        this.localCacheSupplier = (() -> content);
    }

    /**
     * 设置当前编辑的接口（自定义模式）
     */
    public void setCurrentEditingApi(CustomApiInfo api) {
        this.currentEditingApi = api == null ? new CustomApiInfo() : api;
        this.isCustomMode = true;
        // 保存原始内容
        saveOriginalContent();
    }

    /**
     * 设置当前编辑的扫描接口（扫描模式）
     */
    public void setCurrentScanningApi(ApiInfo api) {
        this.currentScanningApi = api;
        this.isCustomMode = false;
        // 保存原始内容
        saveOriginalContent();
    }

    /**
     * 保存原始内容
     */
    private void saveOriginalContent() {
        originalContent.clear();
        currentContent.clear();

        if (isCustomMode && currentEditingApi != null) {
            originalContent.put("name", nullToEmpty(currentEditingApi.getName()));
            originalContent.put("url", nullToEmpty(currentEditingApi.getUrl()));
            originalContent.put("method", nullToEmpty(currentEditingApi.getHttpMethod()));
            originalContent.put("headers", getJsonHeaders(currentEditingApi.getHeaders()));
            originalContent.put("param", getJsonParam(currentEditingApi.getParams()));
            originalContent.put("body", nullToEmpty(currentEditingApi.getBody()));
            // 结构化Body参数（form-data、x-www-form-urlencoded等）
            originalContent.put("bodyParams", getJsonBodyParams(currentEditingApi.getBodyParams()));
            originalContent.put("description", nullToEmpty(currentEditingApi.getDescription()));
            originalContent.put("authMode", String.valueOf(currentEditingApi.getAuthMode()));
            originalContent.put("authValue", nullToEmpty(currentEditingApi.getAuthValue()));
            originalContent.put("bodyType", nullToEmpty(currentEditingApi.getBodyType()));
            originalContent.put("postOp", getJsonPostOp(currentEditingApi.getPostOps()));
            originalContent.put("cookie", getJsonCookie(currentEditingApi.getCookieItems()));

            // 初始化当前内容
            currentContent.putAll(originalContent);
        } else if (!isCustomMode && currentScanningApi != null) {
            // 扫描模式：原始值来自扫描得到的 ApiInfo / 其内数据结构
            originalContent.put("name", nullToEmpty(currentScanningApi.getName()));
            originalContent.put("url", nullToEmpty(currentScanningApi.getUrl()));
            originalContent.put("method", nullToEmpty(currentScanningApi.getHttpMethod()));
            // headers 原始值取自扫描对象（如果可用），否则为空
            originalContent.put("headers", getJsonHeaders(currentScanningApi.getHeaders()));
            originalContent.put("param", getJsonParam(currentScanningApi.getParams()));
            String bodyType = currentScanningApi.getBodyType();
            if (StringUtils.isBlank(bodyType)) {
                if (!currentScanningApi.getBodyParams().isEmpty()) {
                    originalContent.put("bodyType", BodyPanel.guessDefaultBodyType(currentScanningApi.getBodyParams()));
                } else {
                    originalContent.put("bodyType", "none");
                }
            } else {
                originalContent.put("bodyType", bodyType);
            }
            String body = "";
            String bodyParams = "";
            if (!StringUtils.equalsAny(bodyType, "form-data", "x-www-form-urlencoded")) {
                body = currentScanningApi.getBody();
            } else {
                bodyParams = getJsonBodyParams(currentScanningApi.getBodyParams());
            }
            // body 原始内容在扫描数据中通常为空字符串，由编辑产生
            originalContent.put("body", body);
            // 结构化Body参数（用于与表格监听保持一致的字段）
            originalContent.put("bodyParams", bodyParams);
            originalContent.put("description", nullToEmpty(currentScanningApi.getDescription()));
            // 认证与bodyType默认为空/none
            originalContent.put("authMode", String.valueOf(currentScanningApi.getAuthMode()));
            originalContent.put("authValue", nullToEmpty(currentScanningApi.getAuthValue()));
            // 扫描模式下postOp
            originalContent.put("postOp", getJsonPostOp(currentScanningApi.getPostOps()));
            originalContent.put("cookie", getJsonCookie(currentScanningApi.getCookieItems()));

            // 初始化当前内容为原始内容
            currentContent.putAll(originalContent);
        }

        // 重置未保存状态
        hasUnsavedChanges = false;
    }

    /**
     * 获取PostOp json
     *
     * @param postOps
     * @return
     */
    private String getJsonPostOp(List<PostOpPanel.PostOpItem> postOps) {
        if (postOps == null || postOps.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (PostOpPanel.PostOpItem postOpItem : postOps) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", postOpItem.getName());
            map.put("value", nullToEmpty(postOpItem.getValue()));
            map.put("type", postOpItem.getType());
            list.add(map);
        }
        return JSONUtil.toJsonStr(list);
    }


    /**
     * 组装json
     *
     * @param list
     * @param nameGetter
     * @param valueGetter
     * @param typeGetter
     * @param descGetter
     * @param <T>
     * @return
     */
    private <T> String buildParamJson(
            List<T> list,
            Function<T, String> nameGetter,
            Function<T, String> valueGetter,
            Function<T, String> typeGetter,
            Function<T, String> descGetter
    ) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (T item : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", nameGetter.apply(item));
            String value = valueGetter.apply(item);
            map.put("value", StringUtils.isBlank(value) ? "" : value);
            String type = typeGetter.apply(item);
            map.put("dataType", ParamDataType.getParamDataType(type).name());
            map.put("description", nullToEmpty(descGetter.apply(item)));
            resultList.add(map);
        }

        return JSONUtil.toJsonStr(resultList);
    }

    /**
     * 获取Cookie
     *
     * @param cookieItems
     * @return
     */
    private String getJsonCookie(List<CookiesPanel.CookieItem> cookieItems) {
        if (cookieItems == null || cookieItems.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (CookiesPanel.CookieItem cookieItem : cookieItems) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", cookieItem.getName());
            map.put("value", nullToEmpty(cookieItem.getValue()));
            map.put("type", ParamDataType.getParamDataType(cookieItem.getType()).name());
            list.add(map);
        }
        return JSONUtil.toJsonStr(list);
    }

    /**
     * 获取Headers
     *
     * @param headerItems
     * @return
     */
    private String getJsonHeaders(List<HeadersPanel.HeaderItem> headerItems) {
        return buildParamJson(
                headerItems,
                HeadersPanel.HeaderItem::getName,
                HeadersPanel.HeaderItem::getValue,
                HeadersPanel.HeaderItem::getType,
                HeadersPanel.HeaderItem::getDescription
        );
    }

    /**
     * 获取Param
     *
     * @param params
     * @return
     */
    private String getJsonParam(List<ApiParam> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (ApiParam apiParam : params) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", apiParam.getName());
            map.put("value", nullToEmpty(apiParam.getValue()));
            map.put("dataType", apiParam.getDataType() != null ? apiParam.getDataType().name() : ParamDataType.STRING.name());
            map.put("location", apiParam.getType());

            map.put("description", nullToEmpty(apiParam.getDescription()));

            list.add(map);
        }
        return JSONUtil.toJsonStr(list);
    }

    /**
     * 获取Body
     *
     * @param params
     * @return
     */
    private String getJsonBodyParams(List<ApiParam> params) {
        // 使用展开后的扁平参数列表，确保object类型能正确保存
        List<ApiParam> flattenedParams = ApiParamFlattener.flattenParams(params);
        return buildParamJson(
                flattenedParams,
                ApiParam::getName,
                ApiParam::getValue,
                param -> param.getDataType() != null ? param.getDataType().name() : null,
                ApiParam::getDescription
        );
    }

    /**
     * 从缓存获取字符串值
     */
    private String getStringFromCache(Map<String, Object> cache, String key, String defaultValue) {
        Object value = cache.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 获取本地缓存
     */
    private Map<String, Object> getLocalCache() {
        if (localCacheSupplier != null) {
            return localCacheSupplier.get();
        }
        // 如果没有设置回调函数，返回空Map
        return new HashMap<>();
    }

    /**
     * 更新当前内容并检查是否有真实变化
     */
    public void updateContent(String field, String value) {
        if (currentContent.containsKey(field)) {
            currentContent.put(field, value);

            // 如果是扫描模式，同时更新localCache
            if (!isCustomMode && currentScanningApi != null) {
                Map<String, Object> cache = getLocalCache();
                cache.put(field, value);
            }

            // 内容变更检测采用防抖，避免每次按键都执行计算和UI更新
            if (changeCheckDebounceTimer != null) {
                changeCheckDebounceTimer.cancel(false);
            }
            changeCheckDebounceTimer = scheduler.schedule(() -> {
                ApplicationManager.getApplication().invokeLater(this::checkForRealChanges);
            }, CHANGE_CHECK_DEBOUNCE_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查是否有真实的内容变化
     */
    private void checkForRealChanges() {
        boolean hasRealChanges = false;

        for (Map.Entry<String, String> entry : currentContent.entrySet()) {
            String field = entry.getKey();
            String currentValue = entry.getValue();
            String originalValue = originalContent.get(field);

            // 比较当前值和原始值
            if (!Objects.equals(currentValue, originalValue)) {
                hasRealChanges = true;
                break;
            }
        }

        // 更新未保存状态
        boolean oldState = hasUnsavedChanges;
        hasUnsavedChanges = hasRealChanges;

        // 如果状态发生变化，通知UI更新
        if (oldState != hasUnsavedChanges && uiUpdateCallback != null) {
            ApplicationManager.getApplication().invokeLater(uiUpdateCallback);
        }
    }

    /**
     * 标记已保存
     */
    public void markAsSaved() {
        this.hasUnsavedChanges = false;
        if (debounceTimer != null) {
            debounceTimer.cancel(false);
        }
        // 更新原始内容为当前内容
        originalContent.clear();
        originalContent.putAll(currentContent);
    }

    /**
     * 检查是否有未保存的更改
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * 检查是否启用自动保存
     */
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    /**
     * 更新自动保存设置
     */
    public void updateAutoSaveSetting() {
        this.autoSaveEnabled = PropertiesComponent.getInstance().getBoolean("requestman.autoSave", false);
    }

    /**
     * 关闭管理器
     */
    public void dispose() {
        if (debounceTimer != null) {
            debounceTimer.cancel(false);
        }
        scheduler.shutdown();
    }

    /**
     * 为文本组件添加变化监听器
     */
    public void addTextChangeListener(JTextField textField, String fieldName) {
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textField.getText());
            }
        });
    }

    /**
     * 为文本区域添加变化监听器
     */
    public void addTextChangeListener(JTextArea textArea, String fieldName) {
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textArea.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textArea.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateContent(fieldName, textArea.getText());
            }
        });
    }

    /**
     * 为下拉框添加选择变化监听器
     */
    public void addSelectionChangeListener(JComboBox<?> comboBox, String fieldName) {
        comboBox.addActionListener(e -> {
            Object selected = comboBox.getSelectedItem();
            String selectedStr = selected != null ? selected.toString() : "";
            if ("authMode".equals(fieldName)) {
                selectedStr = StringUtils.equals(RequestManBundle.message("auth.inherit"), selectedStr) ? "0" : "1";
            }
            updateContent(fieldName, selectedStr);
        });
    }

    /**
     * 为表格添加变化监听器
     */
    public void addTableChangeListener(JTable table, String fieldName) {
        // 使用WeakHashMap来存储表格的更新标志，防止内存泄漏
        final java.util.Map<JTable, Boolean> updatingTables = new java.util.WeakHashMap<>();

        table.getModel().addTableModelListener(e -> {
            // 防止递归调用：如果当前表格正在更新中，则跳过
            if (updatingTables.containsKey(table) && updatingTables.get(table)) {
                return;
            }

            // 标记当前表格正在更新
            updatingTables.put(table, true);

            try {
                // 尝试提交正在编辑的单元格，避免读取到旧值
                if (table.isEditing()) {
                    try {
                        table.getCellEditor().stopCellEditing();
                    } catch (Exception ex) {
                        // 忽略提交失败，继续读取当前模型数据
                    }
                }

                int rowCount = table.getRowCount();
                List<Map<String, Object>> list = new ArrayList<>();

                for (int row = 0; row < rowCount; row++) {
                    String name = String.valueOf(table.getValueAt(row, 0));
                    if (StringUtils.isBlank(name)) {
                        // 跳过空行（至少 name 要有值）
                        continue;
                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    if ("postOp".equals(fieldName)) {
                        map.put("name", name);

                        String type = String.valueOf(table.getValueAt(row, 1));
                        map.put("type", StringUtils.defaultString(type));

                        String value = String.valueOf(table.getValueAt(row, 2));
                        map.put("value", StringUtils.defaultString(value));

                    } else if ("cookie".equals(fieldName)) {
                        map.put("name", name);

                        String value = String.valueOf(table.getValueAt(row, 1));
                        map.put("value", StringUtils.defaultString(value));

                        String type = String.valueOf(table.getValueAt(row, 2));
                        map.put("type", ParamDataType.getParamDataType(type).name());
                    } else if ("param".equals(fieldName)) {
                        map.put("name", name);

                        String valueStr = String.valueOf(table.getValueAt(row, 1));
                        map.put("value", StringUtils.defaultString(valueStr));

                        String type = String.valueOf(table.getValueAt(row, 2));
                        map.put("dataType", ParamDataType.getParamDataType(type).name());

                        String location = String.valueOf(table.getValueAt(row, 3));
                        map.put("location", StringUtils.defaultString(location));

                        String description = String.valueOf(table.getValueAt(row, 4));
                        map.put("description", StringUtils.defaultString(description));
                    } else {
                        map.put("name", name);

                        String valueStr = String.valueOf(table.getValueAt(row, 1));
                        map.put("value", StringUtils.defaultString(valueStr));

                        String type = String.valueOf(table.getValueAt(row, 2));
                        map.put("dataType", ParamDataType.getParamDataType(type).name());

                        String description = String.valueOf(table.getValueAt(row, 3));
                        map.put("description", StringUtils.defaultString(description));
                    }
                    list.add(map);
                }
                String json = list.isEmpty() ? "" : JSONUtil.toJsonStr(list);

                // 特殊处理：如果是param字段且位置列发生变化，需要触发URL更新
                if ("param".equals(fieldName) && isCustomMode) {
                    int row = e.getFirstRow();
                    if (row >= 0 && row < table.getRowCount()) {
                        // 在更新currentContent之前，先保存当前值作为"变化前的值"
                        String previousParamJson = currentContent.get("param");

                        // 获取变化后的值
                        Object currentNameValue = table.getValueAt(row, 0); // 参数名列
                        Object currentValueValue = table.getValueAt(row, 1); // 参数值列
                        Object currentLocationValue = table.getValueAt(row, 3); // 位置

                        // 如果参数名、参数值或位置发生了变化，则更新URL
                        if (hasParamValueChanged(previousParamJson, row, currentNameValue, currentValueValue, currentLocationValue)) {
                            if (urlUpdateCallback != null) {
                                ApplicationManager.getApplication().invokeLater(urlUpdateCallback);
                            }
                        }
                    }
                }

                if (Boolean.TRUE.equals(immediateMode.get())) {
                    this.updateContentImmediately(fieldName, json);
                } else {
                    this.updateContent(fieldName, json);
                }
            } finally {
                // 更新完成后，移除更新标志
                updatingTables.put(table, false);
            }
        });
    }

    /**
     * 检查参数值是否发生了变化
     */
    private boolean hasParamValueChanged(String originalParamJson, int row, Object currentNameValue, Object currentValueValue, Object currentLocationValue) {
        if (originalParamJson == null || originalParamJson.trim().isEmpty()) {
            // 如果原始内容为空，且当前值不为空，则认为发生了变化
            return (currentNameValue != null && !currentNameValue.toString().trim().isEmpty()) ||
                    (currentValueValue != null && !currentValueValue.toString().trim().isEmpty());
        }

        try {
            // 解析原始JSON，找到对应行的参数名和参数值
            JSONArray originalParams = JSONUtil.parseArray(originalParamJson);
            if (row < originalParams.size()) {
                JSONObject originalParam = originalParams.getJSONObject(row);
                String originalName = String.valueOf(originalParam.get("name"));
                String originalValue = String.valueOf(originalParam.get("value"));
                String originalLocation = String.valueOf(originalParam.get("location"));

                String currentName = currentNameValue != null ? currentNameValue.toString().trim() : "";
                String currentValue = currentValueValue != null ? currentValueValue.toString().trim() : "";
                String currentLocation = currentLocationValue != null ? currentLocationValue.toString().trim() : "";
                if (!StringUtils.equals(originalLocation, currentLocation)) {
                    return true;
                }
                // 比较参数名和参数值是否发生变化
                return !StringUtils.equals(originalName, currentName) || !StringUtils.equals(originalValue, currentValue);
            }
        } catch (Exception e) {
            // 如果解析失败，保守地认为发生了变化
            return true;
        }

        // 如果行数超出范围，认为发生了变化
        return true;
    }

}
