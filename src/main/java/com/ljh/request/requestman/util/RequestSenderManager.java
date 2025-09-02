package com.ljh.request.requestman.util;

import cn.hutool.http.HttpResponse;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.ui.PostOpPanel.PostOpItem;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * RequestMan请求发送管理器，统一处理自定义接口和扫描接口的请求发送逻辑。
 * 该类负责消除RequestManPanel中重复的请求发送代码，提供统一的请求发送接口。
 *
 * @author leijianhui
 * @Description RequestMan请求发送管理器，统一处理请求发送逻辑
 * @date 2025/01/27 10:30
 */
public class RequestSenderManager {
    
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
                Thread t = new Thread(r, "RequestMan-RequestSender");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    );

    /**
     * 静态初始化块，添加JVM关闭时的清理
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 关闭执行器
                EXECUTOR.shutdown();
                if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "RequestMan-RequestSender-ShutdownHook"));
    }

    /**
     * 发送自定义接口请求
     * @param project 项目对象
     * @param customApi 自定义接口信息（可能为null）
     * @param customUrlField URL输入框
     * @param customMethodBox 方法选择框
     * @param customParamsPanel 参数面板
     * @param customBodyPanel 请求体面板
     * @param customPostOpPanel 后置操作面板
     * @param customAuthPanel 认证面板
     * @param responseHandler 响应处理器
     */
    public static void sendCustomRequest(Project project, CustomApiInfo customApi,
                                       Object customUrlField, Object customMethodBox,
                                       Object customParamsPanel, Object customBodyPanel,
                                       Object customPostOpPanel, Object customAuthPanel,
                                       ResponseHandler responseHandler) {
        
        RequestParams params = buildCustomRequestParams(project, customApi, customUrlField, customMethodBox,
                                                      customParamsPanel, customBodyPanel, customPostOpPanel, customAuthPanel);
        sendRequest(project, params, responseHandler, false);
    }

    /**
     * 发送扫描接口请求
     * @param project 项目对象
     * @param apiInfo 扫描接口信息
     * @param paramsPanel 参数面板
     * @param bodyPanel 请求体面板
     * @param headersPanel 请求头面板
     * @param cookiesPanel Cookie面板
     * @param authPanel 认证面板
     * @param postOpPanel 后置操作面板
     * @param responseHandler 响应处理器
     */
    public static void sendScanRequest(Project project, ApiInfo apiInfo,
                                     Object paramsPanel, Object bodyPanel,
                                     Object headersPanel, Object cookiesPanel,
                                     Object authPanel, Object postOpPanel,
                                     ResponseHandler responseHandler) {
        
        RequestParams params = buildScanRequestParams(project, apiInfo, paramsPanel, bodyPanel,
                                                    headersPanel, cookiesPanel, authPanel, postOpPanel);
        sendRequest(project, params, responseHandler, false);
    }

        /**
     * 发送自定义接口请求并下载响应
     * @param project 项目对象
     * @param customApi 自定义接口信息（可能为null）
     * @param customUrlField URL输入框
     * @param customMethodBox 方法选择框
     * @param customParamsPanel 参数面板
     * @param customBodyPanel 请求体面板
     * @param customPostOpPanel 后置操作面板
     * @param customAuthPanel 认证面板
     * @param responseHandler 响应处理器
     */
    public static void sendCustomRequestAndDownload(Project project, CustomApiInfo customApi,
                                                   Object customUrlField, Object customMethodBox,
                                                   Object customParamsPanel, Object customBodyPanel,
                                                   Object customPostOpPanel, Object customAuthPanel,
                                                   ResponseHandler responseHandler) {
        
        RequestParams params = buildCustomRequestParams(project, customApi, customUrlField, customMethodBox,
                                                      customParamsPanel, customBodyPanel, customPostOpPanel, customAuthPanel);
        sendRequest(project, params, responseHandler, true);
    }

    /**
     * 发送扫描接口请求并下载响应
     * @param project 项目对象
     * @param apiInfo 扫描接口信息
     * @param paramsPanel 参数面板
     * @param bodyPanel 请求体面板
     * @param headersPanel 请求头面板
     * @param cookiesPanel Cookie面板
     * @param authPanel 认证面板
     * @param postOpPanel 后置操作面板
     * @param responseHandler 响应处理器
     */
    public static void sendScanRequestAndDownload(Project project, ApiInfo apiInfo,
                                                Object paramsPanel, Object bodyPanel,
                                                Object headersPanel, Object cookiesPanel,
                                                Object authPanel, Object postOpPanel,
                                                ResponseHandler responseHandler) {
        
        RequestParams params = buildScanRequestParams(project, apiInfo, paramsPanel, bodyPanel,
                                                    headersPanel, cookiesPanel, authPanel, postOpPanel);
        sendRequest(project, params, responseHandler, true);
    }

    /**
     * 构建自定义接口请求参数
     * @param project 项目对象
     * @param customApi 自定义接口信息（可能为null）
     * @param customUrlField URL输入框
     * @param customMethodBox 方法选择框
     * @param customParamsPanel 参数面板
     * @param customBodyPanel 请求体面板
     * @param customPostOpPanel 后置操作面板
     * @param customAuthPanel 认证面板
     * @return 请求参数对象
     */
    private static RequestParams buildCustomRequestParams(Project project, CustomApiInfo customApi,
                                                        Object customUrlField, Object customMethodBox,
                                                        Object customParamsPanel, Object customBodyPanel,
                                                        Object customPostOpPanel, Object customAuthPanel) {
        
        RequestParams params = new RequestParams();
        
        // 基本信息 - 自定义模式从UI组件读取URL与方法
        params.setUrl(extractUrlFromField(customUrlField));
        params.setMethod(extractMethodFromBox(customMethodBox));
        
        // 参数信息 - 从参数面板提取
        params.setParams(extractParamsFromPanel(customParamsPanel));
        
        // 请求体信息 - 从请求体面板提取
        extractBodyFromPanel(customBodyPanel, params);
        
        // 后置操作 - 从后置操作面板提取
        params.setPostOps(extractPostOpsFromPanel(customPostOpPanel));
        
        // 认证信息 - 使用封装的方法，传入null作为authPanel表示自定义接口模式
        params.setAuth(extractAuthFromPanel(project, null, customApi));
        
        // 环境配置
        params.setUrlPrefix(ProjectSettingsManager.getCurrentEnvironmentPreUrl(project));

        // 默认值
        params.setHeaders(new HashMap<>());
        params.setCookies(new HashMap<>());
        
        return params;
    }

    /**
     * 构建扫描接口请求参数
     * @param project 项目对象
     * @param apiInfo 扫描接口信息
     * @param paramsPanel 参数面板
     * @param bodyPanel 请求体面板
     * @param headersPanel 请求头面板
     * @param cookiesPanel Cookie面板
     * @param authPanel 认证面板
     * @param postOpPanel 后置操作面板
     * @return 请求参数对象
     */
    private static RequestParams buildScanRequestParams(Project project, ApiInfo apiInfo,
                                                      Object paramsPanel, Object bodyPanel,
                                                      Object headersPanel, Object cookiesPanel,
                                                      Object authPanel, Object postOpPanel) {
        
        RequestParams params = new RequestParams();
        
        // 基本信息
        params.setUrl(apiInfo.getUrl());
        params.setMethod(apiInfo.getHttpMethod());
        
        // 参数信息
        params.setParams(extractParamsFromPanel(paramsPanel));
        
        // 请求体信息
        extractBodyFromPanel(bodyPanel, params);
        
        // 请求头信息
        params.setHeaders(extractHeadersFromPanel(headersPanel));
        
        // Cookie信息
        params.setCookies(extractCookiesFromPanel(cookiesPanel));
        
        // 认证信息
        params.setAuth(extractAuthFromPanel(project, authPanel, null));
        
        // 后置操作
        params.setPostOps(extractPostOpsFromPanel(postOpPanel));
        
        // 环境配置
        params.setUrlPrefix(ProjectSettingsManager.getCurrentEnvironmentPreUrl(project));
        
        return params;
    }

    /**
     * 统一的请求发送方法
     * @param project 项目对象
     * @param params 请求参数
     * @param responseHandler 响应处理器
     * @param downloadResponse 是否下载响应
     */
    private static void sendRequest(Project project, RequestParams params, 
                                  ResponseHandler responseHandler, boolean downloadResponse) {
        
        if (responseHandler.getButton() != null) {
            responseHandler.getButton().setEnabled(false);
        }

        EXECUTOR.submit(() -> {
            try {
                // 发送请求
                try (HttpResponse response = RequestSender.sendRequestRaw(
                        project, params.getUrl(), params.getMethod(), params.getParams(),
                        params.getBodyType(), params.getBodyParams(), params.getBodyContent(),
                        params.getBinaryData(), params.getHeaders(), params.getCookies(),
                        params.getAuth(), params.getUrlPrefix(), params.getPostOps())) {
                    
                    int status = response.getStatus();
                    String responseText = response.body();
                    byte[] responseBytes = response.bodyBytes();
                    String contentType = response.header("Content-Type");
                    
                    // 处理响应
                    ApplicationManager.getApplication().invokeLater(() -> {
                        responseHandler.onSuccess(status, responseText, responseBytes, contentType);
                        
                        // 如果需要下载响应
                        if (downloadResponse) {
                            handleResponseDownload(responseBytes, contentType, responseHandler);
                        }
                        
                        // 恢复按钮状态
                        if (responseHandler.getButton() != null) {
                            responseHandler.getButton().setEnabled(true);
                        }
                    });
                }
                
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    responseHandler.onError(ex);
                    
                    // 恢复按钮状态
                    if (responseHandler.getButton() != null) {
                        responseHandler.getButton().setEnabled(true);
                    }
                });
            }
        });
    }

    /**
     * 处理响应下载
     * @param responseBytes 响应字节数组
     * @param contentType 响应内容类型
     * @param responseHandler 响应处理器
     */
    private static void handleResponseDownload(byte[] responseBytes, String contentType, 
                                            ResponseHandler responseHandler) {
        
        String ext = suggestFileExtension(contentType, responseBytes);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("请选择保存文件的位置");
        fileChooser.setSelectedFile(new File("response" + ext));
        
        int userSelection = fileChooser.showSaveDialog(responseHandler.getResponsePanel());
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                fos.write(responseBytes);
                fos.flush();
                JOptionPane.showMessageDialog(responseHandler.getResponsePanel(), 
                    "文件已保存: " + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(responseHandler.getResponsePanel(), 
                    "保存文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 从参数面板提取参数列表
     * @param paramsPanel 参数面板
     * @return 参数列表
     */
    private static List<ApiParam> extractParamsFromPanel(Object paramsPanel) {
        if (paramsPanel == null) {
            return new ArrayList<>();
        }
        
        try {
            // 使用反射获取参数
            Method getParamsMethod = paramsPanel.getClass().getMethod("getParams");
            @SuppressWarnings("unchecked")
            List<ApiParam> params = (List<ApiParam>) getParamsMethod.invoke(paramsPanel);
            return params != null ? params : new ArrayList<>();
        } catch (Exception e) {
            LogUtil.warn("无法从参数面板提取参数: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从请求体面板提取请求体信息
     * @param bodyPanel 请求体面板
     * @param params 请求参数对象
     */
    private static void extractBodyFromPanel(Object bodyPanel, RequestParams params) {
        if (bodyPanel == null) {
            params.setBodyType("none");
            params.setBodyContent("");
            params.setBodyParams(new ArrayList<>());
            return;
        }
        
        try {
            // 获取请求体类型
            Method getBodyTypeMethod = bodyPanel.getClass().getMethod("getBodyType");
            String bodyType = (String) getBodyTypeMethod.invoke(bodyPanel);
            params.setBodyType(bodyType != null ? bodyType : "none");

            // 根据类型获取内容
            if ("json".equals(bodyType)) {
                Method getJsonBodyMethod = bodyPanel.getClass().getMethod("getJsonBodyText");
                String jsonBody = (String) getJsonBodyMethod.invoke(bodyPanel);
                params.setBodyContent(jsonBody != null ? jsonBody : "");
            } else if ("xml".equals(bodyType)) {
                Method getXmlBodyMethod = bodyPanel.getClass().getMethod("getXmlBodyText");
                String xmlBody = (String) getXmlBodyMethod.invoke(bodyPanel);
                params.setBodyContent(xmlBody != null ? xmlBody : "");
            } else if ("binary".equals(bodyType)) {
                // 对于binary类型，获取二进制数据
                Method getBinaryDataMethod = bodyPanel.getClass().getMethod("getBinaryData");
                byte[] binaryData = (byte[]) getBinaryDataMethod.invoke(bodyPanel);
                params.setBinaryData(binaryData);
            } else if ("form-data".equals(bodyType) || "x-www-form-urlencoded".equals(bodyType)) {
                Method getBodyParamsMethod = bodyPanel.getClass().getMethod("getBodyParams");
                @SuppressWarnings("unchecked")
                List<ApiParam> bodyParams = (List<ApiParam>) getBodyParamsMethod.invoke(bodyPanel);
                params.setBodyParams(bodyParams != null ? bodyParams : new ArrayList<>());
            } else {
                // 其他类型，直接获取bodyContent
                Method getBodyContentMethod = bodyPanel.getClass().getMethod("getBodyContent");
                String bodyContent = (String) getBodyContentMethod.invoke(bodyPanel);
                params.setBodyContent(bodyContent != null ? bodyContent : "");
                params.setBodyParams(new ArrayList<>());
            }
            
        } catch (Exception e) {
            LogUtil.warn("无法从请求体面板提取信息: " + e.getMessage());
            params.setBodyType("none");
            params.setBodyContent("");
            params.setBodyParams(new ArrayList<>());
        }
    }

    /**
     * 从后置操作面板提取后置操作列表
     * @param postOpPanel 后置操作面板
     * @return 后置操作列表
     */
    private static List<PostOpItem> extractPostOpsFromPanel(Object postOpPanel) {
        if (postOpPanel == null) {
            return new ArrayList<>();
        }
        
        try {
            Method getPostOpDataMethod = postOpPanel.getClass().getMethod("getPostOpData");
            @SuppressWarnings("unchecked")
            List<PostOpItem> postOps = (List<PostOpItem>) getPostOpDataMethod.invoke(postOpPanel);
            return postOps != null ? postOps : new ArrayList<>();
        } catch (Exception e) {
            LogUtil.warn("无法从后置操作面板提取数据: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从认证面板提取认证信息
     * @param project 项目对象
     * @param authPanel 认证面板
     * @param customApi 自定义接口信息（可为null）
     * @return 认证值
     */
    private static String extractAuthFromPanel(Project project, Object authPanel, CustomApiInfo customApi) {
        String auth = "";
        
        try {
            // 完全按照原代码逻辑处理认证
            if (customApi != null) {
                // 自定义接口模式：从editingApi获取认证信息
                if (customApi.getAuthMode() == 0) {
                    // 全局认证
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else {
                    // 自定义认证
                    auth = customApi.getAuthValue();
                }
            } else if (authPanel != null) {
                // 扫描接口模式：从authPanel获取认证信息
                Method getAuthModeMethod = authPanel.getClass().getMethod("getAuthMode");
                Integer authMode = (Integer) getAuthModeMethod.invoke(authPanel);
                
                if (authMode != null && authMode == 0) {
                    // 全局认证
                    auth = ProjectSettingsManager.getCurrentEnvironmentGlobalAuth(project);
                } else {
                    // 自定义认证
                    Method getAuthValueMethod = authPanel.getClass().getMethod("getAuthValue");
                    String authValue = (String) getAuthValueMethod.invoke(authPanel);
                    auth = authValue != null ? authValue : "";
                }
            }
            
        } catch (Exception e) {
            LogUtil.warn("无法从认证面板提取认证信息: " + e.getMessage());
            auth = "";
        }
        
        return auth;
    }

    /**
     * 从请求头面板提取请求头信息
     * @param headersPanel 请求头面板
     * @return 请求头映射
     */
    private static Map<String, String> extractHeadersFromPanel(Object headersPanel) {
        if (headersPanel == null) {
            return new HashMap<>();
        }
        
        try {
            Method getHeadersMapMethod = headersPanel.getClass().getMethod("getHeadersMap");
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) getHeadersMapMethod.invoke(headersPanel);
            return headers != null ? headers : new HashMap<>();
        } catch (Exception e) {
            LogUtil.warn("无法从请求头面板提取信息: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 从Cookie面板提取Cookie信息
     * @param cookiesPanel Cookie面板
     * @return Cookie映射
     */
    private static Map<String, String> extractCookiesFromPanel(Object cookiesPanel) {
        if (cookiesPanel == null) {
            return new HashMap<>();
        }
        
        try {
            Method getCookiesMapMethod = cookiesPanel.getClass().getMethod("getCookiesMap");
            @SuppressWarnings("unchecked")
            Map<String, String> cookies = (Map<String, String>) getCookiesMapMethod.invoke(cookiesPanel);
            return cookies != null ? cookies : new HashMap<>();
        } catch (Exception e) {
            LogUtil.warn("无法从Cookie面板提取信息: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 从URL输入框提取URL
     * @param urlField URL输入框
     * @return URL字符串
     */
    private static String extractUrlFromField(Object urlField) {
        if (urlField == null) {
            return "";
        }
        
        try {
            Method getTextMethod = urlField.getClass().getMethod("getText");
            String url = (String) getTextMethod.invoke(urlField);
            return url != null ? url.trim() : "";
        } catch (Exception e) {
            LogUtil.warn("无法从URL输入框提取URL: " + e.getMessage());
            return "";
        }
    }

    /**
     * 从方法选择框提取HTTP方法
     * @param methodBox 方法选择框
     * @return HTTP方法字符串
     */
    private static String extractMethodFromBox(Object methodBox) {
        if (methodBox == null) {
            return "GET";
        }
        
        try {
            Method getSelectedItemMethod = methodBox.getClass().getMethod("getSelectedItem");
            Object selectedItem = getSelectedItemMethod.invoke(methodBox);
            return selectedItem != null ? selectedItem.toString() : "GET";
        } catch (Exception e) {
            LogUtil.warn("无法从方法选择框提取方法: " + e.getMessage());
            return "GET";
        }
    }

    /**
     * 根据内容类型建议文件扩展名
     * @param contentType 内容类型
     * @param bytes 字节数组
     * @return 文件扩展名
     */
    private static String suggestFileExtension(String contentType, byte[] bytes) {
        if (StringUtils.isBlank(contentType)) {
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
}
