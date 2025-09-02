package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.ui.ParamsTablePanel;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leijianhui
 * @Description 自定义接口持久化工具类，负责加载和保存custom_apis.json，支持按项目隔离。
 * @date 2025/06/18 15:07
 */
public class CustomApiStorage {
    /**
     * 获取自定义接口存储文件路径
     *
     * @param project 项目对象
     * @return 文件路径
     */
    private static String getCustomApiFilePath(Project project) {
        String cacheDir = PropertiesComponent.getInstance().getValue("requestman.cacheDir");
        if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = Paths.get(System.getProperty("user.home"), ".requestman_cache").toString() + File.separator;
        }
        if (!cacheDir.endsWith(File.separator)) {
            cacheDir = cacheDir + File.separator;
        }
        // 按项目名称创建子目录，实现项目隔离
        String projectName = project != null ? project.getName() : "default";
        return cacheDir + projectName + File.separator + "custom_apis.json";
    }

    /**
     * 加载所有自定义接口
     *
     * @param project 项目对象
     * @return 自定义接口列表（包含headers字段）
     */
    public static List<CustomApiInfo> loadCustomApis(Project project) {
        try {
            Path file = Paths.get(getCustomApiFilePath(project));
            if (!Files.exists(file)) {
                return new ArrayList<>();
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            // headers字段已自动支持序列化/反序列化
            List<CustomApiInfo> list = JSONUtil.toList(json, CustomApiInfo.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 保存所有自定义接口
     *
     * @param project 项目对象
     * @param apis    自定义接口列表（包含headers字段）
     */
    public static void saveCustomApis(Project project, List<CustomApiInfo> apis) {
        try {
            Path file = Paths.get(getCustomApiFilePath(project));
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            // headers字段已自动支持序列化/反序列化
            String json = JSONUtil.toJsonStr(apis);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    /**
     * 持久化自定义接口列表
     *
     * @param project
     * @param customApiListModel
     */
    public static void persistCustomApiList(Project project, DefaultListModel<CustomApiInfo> customApiListModel) {
        ArrayList<CustomApiInfo> list = new ArrayList<>();
        for (int i = 0; i < customApiListModel.size(); i++) {
            list.add(customApiListModel.get(i));
        }
        saveCustomApis(project, list);
    }

    /**
     * 从缓存加载自定义参数
     */
    public static Map<String, Object> loadCustomParamsFromCache(Project project) {
        try {
            String key = "custom_params_cache";
            Path dir = Paths.get(StorageUtil.getCacheDir(project));
            Path file = dir.resolve(key + StorageUtil.CACHE_SUFFIX);
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = JSONUtil.toBean(json, Map.class);
                return data;
            }
        } catch (Exception ex) {
            LogUtil.warn("加载自定义参数缓存失败: " + ex.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * 保存自定义参数到缓存
     */
    public static void saveCustomParamsToCache(ParamsTablePanel customParamsPanel, Project project) {
        try {
            Map<String, Object> data = new HashMap<>();
            if (customParamsPanel != null) {
                data.put("customParams", customParamsPanel.getParams());
            }
            String key = "custom_params_cache";
            Path dir = Paths.get(StorageUtil.getCacheDir(project));
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve(key + StorageUtil.CACHE_SUFFIX);
            String json = JSONUtil.toJsonStr(data);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            LogUtil.warn("保存自定义参数缓存失败: " + ex.getMessage());
        }
    }
} 