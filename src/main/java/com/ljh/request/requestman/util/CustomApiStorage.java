package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.CustomApiInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
     * @return 自定义接口列表
     */
    public static List<CustomApiInfo> loadCustomApis(Project project) {
        try {
            Path file = Paths.get(getCustomApiFilePath(project));
            if (!Files.exists(file)) {
                return new ArrayList<>();
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
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
     * @param apis    自定义接口列表
     */
    public static void saveCustomApis(Project project, List<CustomApiInfo> apis) {
        try {
            Path file = Paths.get(getCustomApiFilePath(project));
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            String json = JSONUtil.toJsonStr(apis);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
} 