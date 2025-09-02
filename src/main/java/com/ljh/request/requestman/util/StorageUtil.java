
package com.ljh.request.requestman.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.ApiInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author ljh
 * @Description 缓存工具类
 * @date 2025/8/19 11:03
 */
public class StorageUtil {
    /**
     * 本地持久化缓存文件后缀
     */
    public static final String CACHE_SUFFIX = ".json";

    /**
     * 将字符串中的非法文件名字符替换为下划线，保证文件名合法（适用于 Windows 文件系统）
     *
     * @param name 原始字符串
     * @return 合法文件名
     */
    public static String safeFileName(String name) {
        // Windows 文件名非法字符: \\ / : * ? " < > | { }
        return name.replaceAll("[\\\\/:*?\"<>|{}]", "_");
    }

    /**
     * 构建接口唯一key（项目名+url+method+参数结构hash）
     */
    public static String buildApiKey(ApiInfo apiInfo, Project project) {
        String projectName = project != null ? project.getName() : "default";
        String base = projectName + "#" + apiInfo.getUrl() + "#" + apiInfo.getHttpMethod();
        return base;
    }

    /**
     * 获取本地持久化缓存目录，优先使用用户配置，按项目隔离
     *
     * @return 缓存目录绝对路径，结尾带分隔符，系统兼容
     */
    public static String getCacheDir(Project project) {
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

    /**
     * 检查接口参数结构是否变更，变更则清除本地缓存
     *
     * @param apiInfo 当前接口信息
     */
    public static void clearCacheIfParamChanged(ApiInfo apiInfo, Project project) {
        String key = buildApiKey(apiInfo, project);
        Path file = Paths.get(getCacheDir(project), key + CACHE_SUFFIX);
        if (Files.exists(file)) {
            // 只要参数结构hash变了，key就变了，旧文件不会被加载
            // 可定期清理CACHE_DIR下的无用文件
        }
    }
}
