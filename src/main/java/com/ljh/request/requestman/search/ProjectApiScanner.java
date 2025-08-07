package com.ljh.request.requestman.search;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.util.ApiInfoExtractor;
import com.ljh.request.requestman.util.LogUtil;
import com.ljh.request.requestman.util.PerformanceMonitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负责扫描整个项目中的Spring接口（如@RestController、@RequestMapping等注解方法）。
 * 只返回轻量级ApiInfo列表，避免内存泄漏。
 * 单一职责：仅做接口扫描，不做缓存、不做UI。
 *
 * @author requestman
 * @Description 项目接口扫描工具类。
 * @date 2025/06/19 20:00
 */
public class ProjectApiScanner {

    /**
     * 扫描全项目所有Spring接口，返回ApiInfo列表。
     *
     * @param project      当前Project
     * @param keyword      可选关键词过滤（可为null）
     * @param offset       分批加载起始下标
     * @param limit        每批加载数量
     * @param mode         查询模式（按URL/按方法名）
     * @param scope        搜索范围（项目/全局）
     * @param isScanResult 是否为扫描结果
     * @return 接口信息列表
     */
    public static List<ApiInfo> scanApis(Project project, String keyword, int offset, int limit, String mode, com.intellij.psi.search.GlobalSearchScope scope, boolean isScanResult) {
        return scanApisParallel(project, keyword, offset, limit, mode, scope, isScanResult);
    }

    /**
     * 并行扫描全项目所有Spring接口，返回ApiInfo列表。
     * 注意：所有Psi对象访问都在ReadAction中进行，确保线程安全。
     *
     * @param project      当前Project
     * @param keyword      可选关键词过滤（可为null）
     * @param offset       分批加载起始下标
     * @param limit        每批加载数量
     * @param mode         查询模式（按URL/按方法名）
     * @param scope        搜索范围（项目/全局）
     * @param isScanResult 是否为扫描结果
     * @return 接口信息列表
     */
    public static List<ApiInfo> scanApisParallel(Project project, String keyword, int offset, int limit, String mode, com.intellij.psi.search.GlobalSearchScope scope, boolean isScanResult) {
        // 性能监控
        long monitorStartTime = PerformanceMonitor.startOperation("scanApisParallel");

        // 使用线程安全的集合存储结果
        ConcurrentLinkedQueue<ApiInfo> resultQueue = new ConcurrentLinkedQueue<>();
        AtomicInteger processedCount = new AtomicInteger(0);
        int targetCount = offset + limit;

        // 添加超时控制
        long startTime = System.currentTimeMillis();
        // 获取用户配置的扫描超时时间，默认60秒
        int scanTimeoutSeconds = 60;
        try {
            String timeoutStr = PropertiesComponent.getInstance().getValue("requestman.scanTimeout", "60");
            scanTimeoutSeconds = Integer.parseInt(timeoutStr);
        } catch (Exception e) {
            // 使用默认值
        }
        // 转换为毫秒
        long timeoutMs = scanTimeoutSeconds * 1000L;

        // 在单个ReadAction中完成所有工作，避免频繁的线程切换
        List<com.intellij.psi.PsiClass> controllerClasses = new ArrayList<>();
        int finalScanTimeoutSeconds = scanTimeoutSeconds;
        ApplicationManager.getApplication().runReadAction(() -> {
            com.intellij.psi.search.searches.AllClassesSearch.search(scope, project).forEach(psiClass -> {
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    // 显示右下角气泡提示
                    ApplicationManager.getApplication().invokeLater(() -> {
                        com.intellij.notification.NotificationGroupManager.getInstance()
                                .getNotificationGroup("RequestMan")
                                .createNotification(
                                        "RequestMan 扫描超时",
                                        "API扫描已超时（" + finalScanTimeoutSeconds + "秒），已停止扫描。您可以在设置页面修改超时时间。",
                                        com.intellij.notification.NotificationType.WARNING
                                )
                                .notify(project);
                    });
                    return false;
                }

                if (psiClass == null || !psiClass.isValid() || psiClass.getModifierList() == null) {
                    return true;
                }
                // 检查是否为Controller类
                boolean isController = false;
                for (com.intellij.psi.PsiAnnotation ann : psiClass.getModifierList().getAnnotations()) {
                    if (ann == null || !ann.isValid()) {
                        continue;
                    }
                    String qName = ann.getQualifiedName();
                    if (qName != null && (qName.endsWith("RestController") || qName.endsWith("Controller"))) {
                        isController = true;
                        break;
                    }
                }
                if (isController) {
                    controllerClasses.add(psiClass);
                }
                return true;
            });
        });

        // 如果Controller类数量较少，直接串行处理
        if (controllerClasses.size() <= 5) {
            ApplicationManager.getApplication().runReadAction(() -> {
                for (com.intellij.psi.PsiClass psiClass : controllerClasses) {
                    if (System.currentTimeMillis() - startTime > timeoutMs) {
                        break;
                    }
                    processControllerClass(psiClass, keyword, mode, isScanResult, resultQueue, processedCount, targetCount);
                    if (processedCount.get() >= targetCount) {
                        break;
                    }
                }
            });
        } else {
            // 优化批次划分策略：每个批次处理固定数量的类，而不是按CPU核心数划分
            // 最多8个批次
            int optimalBatchSize = Math.max(10, controllerClasses.size() / 8);
            List<List<com.intellij.psi.PsiClass>> batches = new ArrayList<>();
            for (int i = 0; i < controllerClasses.size(); i += optimalBatchSize) {
                int end = Math.min(i + optimalBatchSize, controllerClasses.size());
                batches.add(controllerClasses.subList(i, end));
            }

            // 创建优化的线程池
            ExecutorService executor = createOptimizedThreadPool(batches.size());

            try {
                // 提交所有批次任务
                List<Future<?>> futures = new ArrayList<>();
                for (List<com.intellij.psi.PsiClass> batch : batches) {
                    int finalScanTimeoutSeconds1 = scanTimeoutSeconds;
                    futures.add(executor.submit(() -> {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            for (com.intellij.psi.PsiClass psiClass : batch) {
                                // 检查超时
                                if (System.currentTimeMillis() - startTime > timeoutMs) {
                                    // 显示右下角气泡提示
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        com.intellij.notification.NotificationGroupManager.getInstance()
                                                .getNotificationGroup("RequestMan")
                                                .createNotification(
                                                        "RequestMan 扫描超时",
                                                        "API扫描已超时（" + finalScanTimeoutSeconds1 + "秒），已停止扫描。您可以在设置页面修改超时时间。",
                                                        com.intellij.notification.NotificationType.WARNING
                                                )
                                                .notify(project);
                                    });
                                    break;
                                }
                                processControllerClass(psiClass, keyword, mode, isScanResult, resultQueue, processedCount, targetCount);
                                if (processedCount.get() >= targetCount) {
                                    break;
                                }
                            }
                        });
                    }));
                }

                // 等待所有任务完成或超时
                for (Future<?> future : futures) {
                    try {
                        future.get(Math.max(1, (timeoutMs - (System.currentTimeMillis() - startTime)) / futures.size()), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        future.cancel(true);
                    }
                }
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 转换为List并返回
        List<ApiInfo> result = new ArrayList<>(resultQueue);
        if (result.size() > limit) {
            result = result.subList(offset, Math.min(offset + limit, result.size()));
        }

        // 性能监控结束
        PerformanceMonitor.endOperation("scanApisParallel", monitorStartTime);

        return result;
    }

    /**
     * 优化的动态线程池创建方法
     */
    private static ExecutorService createOptimizedThreadPool(int batchCount) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        // 线程数 = min(批次数量, CPU核心数 * 1.5)，避免过多线程
        int poolSize = Math.max(2, Math.min(batchCount, (int) (cpuCores * 1.5)));
        return new ThreadPoolExecutor(
                poolSize, poolSize,
                // 减少空闲时间
                30L, TimeUnit.SECONDS,
                // 使用ArrayBlockingQueue
                new ArrayBlockingQueue<>(poolSize * 2),
                r -> {
                    Thread t = new Thread(r, "ProjectApiScanner-Worker");
                    t.setDaemon(true);
                    return t;
                },
                // 避免任务丢失
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 处理单个Controller类，提取其中的API方法
     * 注意：此方法必须在ReadAction中调用，确保Psi对象线程安全
     *
     * @param psiClass       Controller类
     * @param keyword        关键词过滤
     * @param mode           查询模式
     * @param isScanResult   是否为扫描结果
     * @param resultQueue    结果队列
     * @param processedCount 已处理计数
     * @param targetCount    目标数量
     */
    private static void processControllerClass(com.intellij.psi.PsiClass psiClass, String keyword, String mode, boolean isScanResult,
                                               ConcurrentLinkedQueue<ApiInfo> resultQueue, AtomicInteger processedCount, int targetCount) {
        // 遍历方法
        for (com.intellij.psi.PsiMethod method : psiClass.getMethods()) {
            if (method == null || !method.isValid() || method.getModifierList() == null) {
                continue;
            }

            // 过滤掉构造方法
            if (method.isConstructor()) {
                continue;
            }

            // 使用Set来避免重复添加同一个方法
            Set<String> processedMethods = new HashSet<>();
            String methodKey = method.getName() + "#" + method.getParameterList().getParametersCount();

            for (com.intellij.psi.PsiAnnotation ann : method.getModifierList().getAnnotations()) {
                if (ann == null || !ann.isValid()) {
                    continue;
                }
                String qName = ann.getQualifiedName();
                if (qName == null) {
                    continue;
                }

                // 精确匹配Spring的Mapping注解，避免自定义注解冲突
                if (isSpringMappingAnnotation(qName)) {
                    // 检查是否已经处理过这个方法
                    if (processedMethods.contains(methodKey)) {
                        continue;
                    }
                    processedMethods.add(methodKey);

                    ApiInfo api = ApiInfoExtractor.extractApiInfoFromMethod(method, isScanResult);
                    // 关键词过滤
                    String keywordLower = keyword == null ? "" : keyword.toLowerCase();
                    boolean match = false;
                    if (!keywordLower.isEmpty()) {
                        if ("URL".equals(mode)) {
                            match = (api.getUrl() != null && api.getUrl().toLowerCase().contains(keywordLower));
                        } else if ("MethodName".equals(mode)) {
                            match = (api.getMethodName() != null && api.getMethodName().toLowerCase().contains(keywordLower));
                        } else {
                            match = (api.getName() != null && api.getName().toLowerCase().contains(keywordLower))
                                    || (api.getMethodName() != null && api.getMethodName().toLowerCase().contains(keywordLower))
                                    || (api.getUrl() != null && api.getUrl().toLowerCase().contains(keywordLower))
                                    || (api.getDescription() != null && api.getDescription().toLowerCase().contains(keywordLower));
                        }
                        if (!match) {
                            continue;
                        }
                    }

                    LogUtil.debug("[PojoFieldScanner] 类: " + api.getClassName() + ", 方法: " + api.getMethodName());
                    resultQueue.offer(api);

                    // 检查是否达到目标数量
                    if (processedCount.incrementAndGet() >= targetCount) {
                        return;
                    }

                    // 找到一个Mapping注解就跳出，避免重复处理
                    break;
                }
            }
        }
    }

    /**
     * 判断是否为Spring的Mapping注解
     *
     * @param qualifiedName 注解的完全限定名
     * @return 是否为Spring Mapping注解
     */
    private static boolean isSpringMappingAnnotation(String qualifiedName) {
        if (qualifiedName == null) {
            return false;
        }

        // 精确匹配Spring的Mapping注解
        return qualifiedName.equals("org.springframework.web.bind.annotation.RequestMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.GetMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.PostMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.PutMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.DeleteMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.PatchMapping") ||
                qualifiedName.equals("org.springframework.web.bind.annotation.RequestMapping") ||
                // 支持简化的注解名称（不带包名）
                qualifiedName.equals("RequestMapping") ||
                qualifiedName.equals("GetMapping") ||
                qualifiedName.equals("PostMapping") ||
                qualifiedName.equals("PutMapping") ||
                qualifiedName.equals("DeleteMapping") ||
                qualifiedName.equals("PatchMapping");
    }

    /**
     * 原始串行扫描方法，保持向后兼容
     *
     * @param project      当前Project
     * @param keyword      可选关键词过滤（可为null）
     * @param offset       分批加载起始下标
     * @param limit        每批加载数量
     * @param mode         查询模式（按URL/按方法名）
     * @param scope        搜索范围（项目/全局）
     * @param isScanResult 是否为扫描结果
     * @return 接口信息列表
     */
    public static List<ApiInfo> scanApisSerial(Project project, String keyword, int offset, int limit, String mode, com.intellij.psi.search.GlobalSearchScope scope, boolean isScanResult) {
        List<ApiInfo> result = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            com.intellij.psi.search.searches.AllClassesSearch.search(scope, project).forEach(psiClass -> {
                if (psiClass == null || !psiClass.isValid() || psiClass.getModifierList() == null) {
                    return true;
                }
                boolean isController = false;
                for (com.intellij.psi.PsiAnnotation ann : psiClass.getModifierList().getAnnotations()) {
                    if (ann == null || !ann.isValid()) {
                        continue;
                    }
                    String qName = ann.getQualifiedName();
                    if (qName != null && (qName.endsWith("RestController") || qName.endsWith("Controller"))) {
                        isController = true;
                        break;
                    }
                }
                if (!isController) {
                    return true;
                }

                // 遍历方法
                for (com.intellij.psi.PsiMethod method : psiClass.getMethods()) {
                    if (method == null || !method.isValid() || method.getModifierList() == null) {
                        continue;
                    }

                    // 过滤掉构造方法
                    if (method.isConstructor()) {
                        continue;
                    }

                    // 使用Set来避免重复添加同一个方法
                    Set<String> processedMethods = new HashSet<>();
                    String methodKey = method.getName() + "#" + method.getParameterList().getParametersCount();

                    for (com.intellij.psi.PsiAnnotation ann : method.getModifierList().getAnnotations()) {
                        if (ann == null || !ann.isValid()) {
                            continue;
                        }
                        String qName = ann.getQualifiedName();
                        if (qName == null) {
                            continue;
                        }

                        // 精确匹配Spring的Mapping注解，避免自定义注解冲突
                        if (isSpringMappingAnnotation(qName)) {
                            // 检查是否已经处理过这个方法
                            if (processedMethods.contains(methodKey)) {
                                continue;
                            }
                            processedMethods.add(methodKey);

                            ApiInfo api = ApiInfoExtractor.extractApiInfoFromMethod(method, isScanResult);

                            // 关键词过滤
                            String keywordLower = keyword == null ? "" : keyword.toLowerCase();
                            boolean match = false;
                            if (!keywordLower.isEmpty()) {
                                if ("URL".equals(mode)) {
                                    match = (api.getUrl() != null && api.getUrl().toLowerCase().contains(keywordLower));
                                } else if ("MethodName".equals(mode)) {
                                    match = (api.getMethodName() != null && api.getMethodName().toLowerCase().contains(keywordLower));
                                } else {
                                    match = (api.getName() != null && api.getName().toLowerCase().contains(keywordLower))
                                            || (api.getMethodName() != null && api.getMethodName().toLowerCase().contains(keywordLower))
                                            || (api.getUrl() != null && api.getUrl().toLowerCase().contains(keywordLower))
                                            || (api.getDescription() != null && api.getDescription().toLowerCase().contains(keywordLower));
                                }
                                if (!match) {
                                    continue;
                                }
                            }

                            result.add(api);
                            if (result.size() >= offset + limit) {
                                // 已够一批，提前终止
                                return false;
                            }

                            // 找到一个Mapping注解就跳出，避免重复处理
                            break;
                        }
                    }
                }
                return true;
            });
        });

        // 分批返回
        if (offset >= result.size()) {
            return new ArrayList<>();
        }
        int toIdx = Math.min(result.size(), offset + limit);
        return result.subList(offset, toIdx);
    }

    /**
     * 拼接类和方法的@RequestMapping，返回完整url
     */
    public static String getFullUrl(com.intellij.psi.PsiClass psiClass, com.intellij.psi.PsiMethod method) {
        String classUrl = "";
        for (com.intellij.psi.PsiAnnotation ann : psiClass.getModifierList().getAnnotations()) {
            String qName = ann.getQualifiedName();
            if (qName != null && qName.endsWith("RequestMapping")) {
                var value = ann.findAttributeValue("value");
                if (value != null) {
                    classUrl = extractPathFromValue(value);
                }
            }
        }
        String methodUrl = "";
        for (com.intellij.psi.PsiAnnotation ann : method.getModifierList().getAnnotations()) {
            String qName = ann.getQualifiedName();
            if (qName != null && qName.endsWith("Mapping")) {
                var value = ann.findAttributeValue("value");
                if (value != null) {
                    methodUrl = extractPathFromValue(value);
                }
            }
        }
        if (!classUrl.isEmpty() && !methodUrl.startsWith("/")) {
            return classUrl + (methodUrl.startsWith("/") ? "" : "/") + methodUrl;
        } else if (!classUrl.isEmpty()) {
            return classUrl + methodUrl;
        } else {
            return methodUrl;
        }
    }

    /**
     * 从注解的value属性中提取路径，支持数组形式如 @RequestMapping({"/sys/webui"})
     *
     * @param value 注解的value属性
     * @return 提取的路径
     */
    private static String extractPathFromValue(com.intellij.psi.PsiAnnotationMemberValue value) {
        if (value == null) {
            return "";
        }

        String text = value.getText();
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 处理数组形式：@RequestMapping({"/sys/webui"}) 或 @RequestMapping({"/sys/webui", "/api/webui"})
        if (text.startsWith("{") && text.endsWith("}")) {
            // 移除外层的大括号
            String arrayContent = text.substring(1, text.length() - 1);
            // 分割多个路径（如果有的话）
            String[] paths = arrayContent.split(",");
            if (paths.length > 0) {
                // 取第一个路径，移除引号
                String firstPath = paths[0].trim().replaceAll("[\"']", "");
                return firstPath;
            }
        }

        // 处理单个路径：@RequestMapping("/sys/webui")
        return text.replaceAll("[\"']", "");
    }

    /**
     * 性能测试方法，比较串行和并行扫描的性能差异
     *
     * @param project 当前Project
     * @param scope   搜索范围
     * @return 性能测试结果
     */
    public static String performanceTest(Project project, com.intellij.psi.search.GlobalSearchScope scope) {
        StringBuilder result = new StringBuilder();
        result.append("=== ProjectApiScanner 性能测试 ===\n");

        // 测试串行扫描
        long startTime = System.currentTimeMillis();
        List<ApiInfo> serialResult = scanApisSerial(project, "", 0, Integer.MAX_VALUE, "URL", scope, false);
        long serialTime = System.currentTimeMillis() - startTime;

        // 测试并行扫描
        startTime = System.currentTimeMillis();
        List<ApiInfo> parallelResult = scanApisParallel(project, "", 0, Integer.MAX_VALUE, "URL", scope, false);
        long parallelTime = System.currentTimeMillis() - startTime;

        result.append(String.format("扫描结果数量: %d\n", serialResult.size()));
        result.append(String.format("串行扫描耗时: %d ms\n", serialTime));
        result.append(String.format("并行扫描耗时: %d ms\n", parallelTime));
        result.append(String.format("性能提升: %.2f%%\n",
                serialTime > 0 ? ((double) (serialTime - parallelTime) / serialTime) * 100 : 0));
        result.append(String.format("结果一致性: %s\n",
                serialResult.size() == parallelResult.size() ? "通过" : "失败"));

        return result.toString();
    }
} 