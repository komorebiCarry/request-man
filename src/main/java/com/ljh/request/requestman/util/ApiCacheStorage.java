package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.ui.AuthPanel;
import com.ljh.request.requestman.ui.BodyPanel;
import com.ljh.request.requestman.ui.CookiesPanel;
import com.ljh.request.requestman.ui.HeadersPanel;
import com.ljh.request.requestman.ui.ParamsTablePanel;
import com.ljh.request.requestman.ui.PostOpPanel;
import com.ljh.request.requestman.ui.RequestManPanel;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义编辑内容缓存管理器，负责管理接口自定义编辑内容的本地缓存操作。
 * 包括加载、保存、清理缓存等功能，支持按项目隔离的缓存策略。
 *
 * @author leijianhui
 * @Description 扫描模式接口持久化工具类，负责加载和保存，支持按项目隔离。
 * @date 2025/01/27 10:00
 */
public class ApiCacheStorage {

    /**
     * 内存缓存，避免频繁读写磁盘，LRU策略最大200条，防止内存泄漏
     */
    private static final Map<String, ApiInfo> localCache = new LinkedHashMap<>() {
        private static final int MAX_ENTRIES = 200;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ApiInfo> eldest) {
            if (size() > MAX_ENTRIES) {
                return true;
            } else {
                return false;
            }
        }
    };

    /**
     * 加载本地缓存（在 showApiDetail 或 buildParamTab、BodyPanel 等处调用）
     *
     * @param apiInfo
     * @param project
     * @return
     */
    public static ApiInfo loadCustomEdit(ApiInfo apiInfo, Project project) {
        try {
            String key = StorageUtil.safeFileName(StorageUtil.buildApiKey(apiInfo, project));
            if (localCache.containsKey(key)) {
                return localCache.getOrDefault(key, apiInfo);
            }
            Path dir = Paths.get(StorageUtil.getCacheDir(project));
            Path file = dir.resolve(key + StorageUtil.CACHE_SUFFIX);
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                ApiInfo cache = JSONUtil.toBean(json, ApiInfo.class);
                localCache.put(key, cache);
                return cache;
            }
        } catch (Exception ignored) {
        }
        return new ApiInfo(apiInfo);
    }

    /**
     * 保存自定义编辑内容到本地缓存
     *
     * @param apiInfo 当前接口信息
     */
    public static void saveCustomEdit(ApiInfo apiInfo, Project project, RequestManPanel requestManPanel, AutoSaveManager autoSaveManager) {

        try {
            BodyPanel bodyPanel = requestManPanel.getBodyPanel();
            ParamsTablePanel paramsPanel = requestManPanel.getParamsPanel();
            PostOpPanel postOpPanel = requestManPanel.getPostOpPanel();
            AuthPanel authPanel = requestManPanel.getAuthPanel();
            HeadersPanel headersPanel = requestManPanel.getHeadersPanel();
            CookiesPanel cookiesPanel = requestManPanel.getCookiesPanel();
            // 保存时根据当前类型获取内容
            String body = "";
            String bodyType = bodyPanel != null ? bodyPanel.getBodyType() : "none";
            List<ApiParam> bodyParams = apiInfo.getBodyParams() != null && !apiInfo.getBodyParams().isEmpty() ? apiInfo.getBodyParams() : new ArrayList<>();
            if (bodyPanel != null) {
                if ("none".equals(bodyType)) {
                    body = "";
                } else if ("json".equals(bodyType)) {
                    body = bodyPanel.getJsonBodyText();
                } else if ("form-data".equals(bodyType)) {
                    bodyParams = bodyPanel.getBodyParams();
                } else if ("x-www-form-urlencoded".equals(bodyType)) {
                    bodyParams = bodyPanel.getBodyParams();
                } else if ("xml".equals(bodyType)) {
                    body = bodyPanel.getXmlBodyText();
                } else if ("binary".equals(bodyType)) {
                    body = bodyPanel.getFilePathFromBinaryText();
                }
            }
            List<ApiParam> params = paramsPanel != null ? paramsPanel.getParams() : new ArrayList<>();
            // 过滤掉参数名为空的行，防止空行被保存
            params = params.stream()
                    .filter(p -> p.getName() != null && !p.getName().trim().isEmpty())
                    .collect(Collectors.toList());
            List<PostOpPanel.PostOpItem> postOps = postOpPanel != null ? postOpPanel.getPostOpData() : new ArrayList<>();

            if (apiInfo == null) {
                return;
            } else {
                if (apiInfo.getName().isEmpty() || apiInfo.getUrl().isEmpty() || apiInfo.getMethodName().isEmpty()) {
                    return;
                }
                // 编辑
                apiInfo.setParams(params);
                apiInfo.setPostOps(postOps);
                apiInfo.setBody(body);
                apiInfo.setBodyType(bodyType);
                apiInfo.setBodyParams(bodyParams);
                // 保存认证信息
                if (authPanel != null) {
                    apiInfo.setAuthMode(authPanel.getAuthMode());
                    apiInfo.setAuthValue(authPanel.getAuthValue());
                }
                // 保存headers
                if (headersPanel != null) {
                    apiInfo.setHeaders(headersPanel.getHeadersData());
                }
                // 保存cookice
                if (cookiesPanel != null) {
                    apiInfo.setCookieItems(cookiesPanel.getCookiesData());
                }
            }
            String key = StorageUtil.buildApiKey(apiInfo, project);
            key = StorageUtil.safeFileName(key); // 保证文件名合法，防止非法字符导致保存失败
            Path dir = Paths.get(StorageUtil.getCacheDir(project));
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve(key + StorageUtil.CACHE_SUFFIX);
            String json = JSONUtil.toJsonStr(apiInfo);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
            localCache.put(key, apiInfo);
            if (!autoSaveManager.isAutoSaveEnabled()) {
                JOptionPane.showMessageDialog(requestManPanel, RequestManBundle.message("common.save.success"), RequestManBundle.message("main.tip"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            if (!autoSaveManager.isAutoSaveEnabled()) {
                JOptionPane.showMessageDialog(requestManPanel, RequestManBundle.message("common.save.fail") + ex.getMessage(), RequestManBundle.message("common.error"), JOptionPane.ERROR_MESSAGE);
            } else {
                if (!autoSaveManager.isAutoSaveEnabled()) {
                    new Notification(
                            "RequestMan", // groupDisplayId，可自定义
                            RequestManBundle.message("common.save.silentFail"),
                            "",
                            NotificationType.ERROR
                    ).notify(project);
                }
            }
        }
    }

    /**
     * 清除指定接口的本地缓存
     *
     * @param apiInfo 接口信息
     * @param project 项目
     */
    public static void clearCustomEdit(ApiInfo apiInfo, Project project) {
        try {
            String key = StorageUtil.safeFileName(StorageUtil.buildApiKey(apiInfo, project));

            // 从内存缓存中移除
            localCache.remove(key);

            // 删除磁盘文件
            Path dir = Paths.get(StorageUtil.getCacheDir(project));
            Path file = dir.resolve(key + StorageUtil.CACHE_SUFFIX);
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (Exception ex) {
            LogUtil.error("清除接口缓存时发生错误: " + ex.getMessage(), ex);
        }
    }


}
