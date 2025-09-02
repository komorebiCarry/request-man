/**
 * @author leijianhui
 * @Description 项目启动后自动缓存接口（初始化搜索模式下）。
 * @date 2025/07/23 10:50
 */
package com.ljh.request.requestman.startup;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.ljh.request.requestman.search.ApiSearchPopup;
import com.ljh.request.requestman.ui.RequestManPanel;
import com.ljh.request.requestman.util.LogUtil;
import com.ljh.request.requestman.util.PojoFieldScanner;
import com.ljh.request.requestman.util.ProjectHistoryCleaner;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

/**
 * RequestMan启动活动，负责在项目启动后初始化接口缓存。
 * 采用智能延迟加载策略，平衡功能性和性能。
 *
 * @author leijianhui
 * @Description RequestMan启动活动，智能初始化接口缓存。
 * @date 2025/07/23 10:50
 */
public class RequestManStartupActivity implements ProjectActivity {

    /**
     * 延迟加载时间（毫秒）
     */
    private static final int DELAY_LOAD_MS = 2000;

    /**
     * 大项目阈值（类数量），超过此值使用更保守的策略
     */
    private static final int LARGE_PROJECT_THRESHOLD = 2000;

    /**
     * 超大项目阈值，超过此值完全禁用自动扫描
     */
    private static final int HUGE_PROJECT_THRESHOLD = 5000;

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 注册项目监听器，在项目关闭时清理缓存
        project.getMessageBus().connect().subscribe(
                com.intellij.openapi.project.ProjectManager.TOPIC,
                new com.intellij.openapi.project.ProjectManagerListener() {
                    @Override
                    public void projectClosingBeforeSave(@NotNull Project closingProject) {
                        if (closingProject.equals(project)) {
                            LogUtil.info("[RequestMan] 项目 " + project.getName() + " 正在关闭，清理缓存");
                            ApiSearchPopup.clearProjectCache(project);
                            ProjectHistoryCleaner.clearProjectHistory(project);
                            // 项目关闭时检查是否有未保存的更改
                            RequestManPanel requestManPanel = RequestManPanel.findRequestManPanel(project);
                            if (requestManPanel != null) {
                                requestManPanel.checkUnsavedChanges();
                                // 清理基线缓存，防止内存泄漏
                                requestManPanel.clearBaselines();
                            }
                            // 清理接口实现缓存，防止内存泄漏
                            PojoFieldScanner.clearImplementationCache();
                        }
                    }
                }
        );

        // 立即返回，避免阻塞项目加载
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProjectHistoryCleaner.clearProjectHistory(project);
                // 检查是否启用自动扫描
                String searchMode = PropertiesComponent.getInstance().getValue("requestman.searchMode", "init");
                if (!"init".equals(searchMode)) {
                    return;
                }

                // 在后台线程中检查项目大小
                ProjectSize projectSize = getProjectSize(project);

                // 超大项目仍然允许扫描，但会采用更保守的策略
                if (projectSize == ProjectSize.HUGE) {
                    LogUtil.info("超大项目检测到，采用保守扫描策略");
                }

                // 根据项目大小调整延迟时间
                int actualDelay = projectSize == ProjectSize.LARGE ? DELAY_LOAD_MS * 2 : DELAY_LOAD_MS;
                Thread.sleep(actualDelay);

                // 再次检查项目是否仍然有效
                if (project.isDisposed()) {
                    return;
                }

                boolean includeLibs = PropertiesComponent.getInstance().getBoolean("requestman.includeLibs", false);

                // 超大项目增加延迟保护
                if (projectSize == ProjectSize.HUGE) {
                    // 额外延迟
                    Thread.sleep(DELAY_LOAD_MS);
                }

                // 使用ReadAction确保线程安全
                com.intellij.openapi.application.ReadAction.run(() -> {
                    try {
                        // 再次检查项目是否仍然有效
                        if (project.isDisposed()) {
                            return;
                        }
                        ApiSearchPopup.cacheApisOnSettingSaved(project, includeLibs);
                    } catch (Exception e) {
                        LogUtil.error("缓存扫描失败: " + e.getMessage(), e);
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // 记录错误但不影响项目启动
                LogUtil.error("自动扫描失败: " + e.getMessage(), e);
            }
        });

        return Unit.INSTANCE;
    }

    /**
     * 项目大小枚举
     */
    private enum ProjectSize {
        // 小项目：<1000个类
        SMALL,
        // 中等项目：1000-2000个类
        MEDIUM,
        // 大项目：2000-5000个类
        LARGE,
        // 超大项目：>5000个类
        HUGE
    }

    /**
     * 检查项目大小，采用智能检测方式
     *
     * @param project 项目对象
     * @return 项目大小
     */
    private ProjectSize getProjectSize(@NotNull Project project) {
        try {
            String basePath = project.getBasePath();
            VirtualFile projectDir = null;
            if (basePath != null) {
                projectDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            }
            if (projectDir == null) {
                LogUtil.warn("项目根目录为空");
                // 默认中等项目
                return ProjectSize.MEDIUM;
            }

            // 智能检测：检查常见的源码目录
            int totalJavaFiles = 0;

            // 1. 检查标准Maven/Gradle结构
            int srcMainJava = countJavaFilesInPath(projectDir, "src/main/java");
            int srcTestJava = countJavaFilesInPath(projectDir, "src/test/java");
            totalJavaFiles += srcMainJava;
            totalJavaFiles += srcTestJava;

            // 2. 检查标准src目录
            int srcDir = countJavaFilesInPath(projectDir, "src");
            totalJavaFiles += srcDir;

            // 3. 检查常见的包目录
            int appDir = countJavaFilesInPath(projectDir, "app");
            int mainDir = countJavaFilesInPath(projectDir, "main");
            int javaDir = countJavaFilesInPath(projectDir, "java");
            totalJavaFiles += appDir;
            totalJavaFiles += mainDir;
            totalJavaFiles += javaDir;

            // 4. 检查多模块项目结构
            int multiModule = countJavaFilesInMultiModuleProject(projectDir);
            totalJavaFiles += multiModule;

            // 5. 如果都没有找到，扫描整个项目（限制深度）
            if (totalJavaFiles == 0) {
                totalJavaFiles = countJavaFiles(projectDir, 0);
            }

            // 根据Java文件数量估算项目大小
            // 提高阈值，因为很多Java文件不是API接口
            if (totalJavaFiles > 2000) {
                return ProjectSize.HUGE;
            } else if (totalJavaFiles > 800) {
                return ProjectSize.LARGE;
            } else if (totalJavaFiles > 200) {
                return ProjectSize.MEDIUM;
            } else {
                return ProjectSize.SMALL;
            }

        } catch (Exception e) {
            // 如果检查失败，保守起见认为是大项目
            LogUtil.error("[RequestMan] 项目大小检查失败: " + e.getMessage(), e);
            return ProjectSize.LARGE;
        }
    }

    /**
     * 在指定路径下统计Java文件数量
     *
     * @param rootDir      根目录
     * @param relativePath 相对路径
     * @return Java文件数量
     */
    private int countJavaFilesInPath(com.intellij.openapi.vfs.VirtualFile rootDir, String relativePath) {
        try {
            com.intellij.openapi.vfs.VirtualFile targetDir = rootDir.findFileByRelativePath(relativePath);
            if (targetDir != null && targetDir.isDirectory()) {
                return countJavaFiles(targetDir, 0);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
        return 0;
    }

    /**
     * 检测多模块项目结构
     *
     * @param rootDir 根目录
     * @return Java文件总数
     */
    private int countJavaFilesInMultiModuleProject(com.intellij.openapi.vfs.VirtualFile rootDir) {
        int totalFiles = 0;
        try {
            // 检查是否有pom.xml或build.gradle，说明是多模块项目
            boolean hasMavenPom = rootDir.findChild("pom.xml") != null;
            boolean hasGradleBuild = rootDir.findChild("build.gradle") != null || rootDir.findChild("build.gradle.kts") != null;

            if (hasMavenPom || hasGradleBuild) {
                // 扫描子目录，寻找可能的模块
                for (com.intellij.openapi.vfs.VirtualFile child : rootDir.getChildren()) {
                    if (child.isDirectory()) {
                        String dirName = child.getName();
                        // 跳过常见的非模块目录
                        if ("target".equals(dirName) || "build".equals(dirName) || ".git".equals(dirName) ||
                                "node_modules".equals(dirName) || "out".equals(dirName) || "bin".equals(dirName) ||
                                "logs".equals(dirName) || "temp".equals(dirName)) {
                            continue;
                        }

                        // 检查子目录是否包含源码
                        totalFiles += countJavaFilesInPath(child, "src/main/java");
                        totalFiles += countJavaFilesInPath(child, "src/test/java");
                        totalFiles += countJavaFilesInPath(child, "src");

                        // 如果子目录本身包含Java文件，也计算在内
                        if (child.findChild("pom.xml") != null || child.findChild("build.gradle") != null) {
                            totalFiles += countJavaFiles(child, 0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略多模块检测的错误
        }
        return totalFiles;
    }

    /**
     * 快速统计Java文件数量，限制扫描范围
     *
     * @param dir   目录
     * @param depth 当前深度
     * @return Java文件数量
     */
    private int countJavaFiles(com.intellij.openapi.vfs.VirtualFile dir, int depth) {
        // 增加扫描深度，适应多模块项目
        if (depth > 8) {
            return 0;
        }

        int count = 0;
        com.intellij.openapi.vfs.VirtualFile[] children = dir.getChildren();

        // 限制处理的子文件数量，避免过多扫描
        // 增加处理文件数量
        int maxChildren = Math.min(children.length, 100);

        for (int i = 0; i < maxChildren; i++) {
            com.intellij.openapi.vfs.VirtualFile child = children[i];
            if (child.isDirectory()) {
                // 跳过常见的非源码目录
                String name = child.getName();
                if ("target".equals(name) || "build".equals(name) || ".git".equals(name) ||
                        "node_modules".equals(name) || "out".equals(name) || "bin".equals(name) ||
                        "test".equals(name) || "tests".equals(name) || ".idea".equals(name)) {
                    continue;
                }
                count += countJavaFiles(child, depth + 1);

                // 如果已经找到足够多的文件，提前返回
                if (count > 500) {
                    return count;
                }
            } else if (child.getName().endsWith(".java")) {
                count++;

                // 如果已经找到足够多的文件，提前返回
                if (count > 500) {
                    return count;
                }
            }
        }
        return count;
    }
} 