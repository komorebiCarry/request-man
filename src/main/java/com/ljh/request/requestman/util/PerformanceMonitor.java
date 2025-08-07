package com.ljh.request.requestman.util;

import com.intellij.ide.util.PropertiesComponent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控工具类，用于监控插件性能并提供诊断信息。
 * 帮助识别性能瓶颈和内存泄漏问题。
 *
 * @author leijianhui
 * @Description 性能监控工具类，提供内存、线程、执行时间等监控功能。
 * @date 2025/01/27 14:30
 */
public class PerformanceMonitor {

    /**
     * 操作耗时统计
     */
    private static final ConcurrentHashMap<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> operationTimers = new ConcurrentHashMap<>();

    /**
     * 内存监控
     */
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * 线程监控
     */
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * RequestMan插件专用统计
     */
    private static final AtomicLong pluginThreadCount = new AtomicLong(0);
    private static final AtomicLong pluginCacheSize = new AtomicLong(0);
    private static final AtomicLong pluginApiCount = new AtomicLong(0);

    /**
     * 检查性能监控是否启用
     *
     * @return 如果监控启用返回true，否则返回false
     */
    private static boolean isMonitoringEnabled() {
        try {
            return PropertiesComponent.getInstance().getBoolean("requestman.performanceMonitoring", false);
        } catch (Exception e) {
            // 如果获取配置失败，默认关闭监控
            return false;
        }
    }

    /**
     * 记录操作开始时间
     *
     * @param operationName 操作名称
     * @return 开始时间戳
     */
    public static long startOperation(String operationName) {
        // 如果监控未启用，直接返回当前时间戳，不进行数据采集
        if (!isMonitoringEnabled()) {
            return System.currentTimeMillis();
        }

        long startTime = System.currentTimeMillis();
        operationCounters.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();
        return startTime;
    }

    /**
     * 记录操作结束时间并计算耗时
     *
     * @param operationName 操作名称
     * @param startTime     开始时间戳
     */
    public static void endOperation(String operationName, long startTime) {
        // 如果监控未启用，直接返回，不进行数据采集
        if (!isMonitoringEnabled()) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        operationTimers.computeIfAbsent(operationName, k -> new AtomicLong(0)).addAndGet(duration);

        // 如果操作耗时超过阈值，记录警告
        // 1秒阈值
        if (duration > 1000) {
            LogUtil.warn("[PerformanceMonitor] Slow operation detected: " + operationName + " took " + duration + "ms");
        }
    }

    /**
     * 更新插件线程数量
     *
     * @param threadCount 线程数量
     */
    public static void updatePluginThreadCount(long threadCount) {
        // 如果监控未启用，直接返回，不进行数据采集
        if (!isMonitoringEnabled()) {
            return;
        }

        pluginThreadCount.set(threadCount);
    }

    /**
     * 更新插件缓存大小
     *
     * @param cacheSize 缓存大小
     */
    public static void updatePluginCacheSize(long cacheSize) {
        // 如果监控未启用，直接返回，不进行数据采集
        if (!isMonitoringEnabled()) {
            return;
        }

        pluginCacheSize.set(cacheSize);
    }

    /**
     * 更新插件API数量
     *
     * @param apiCount API数量
     */
    public static void updatePluginApiCount(long apiCount) {
        // 如果监控未启用，直接返回，不进行数据采集
        if (!isMonitoringEnabled()) {
            return;
        }

        pluginApiCount.set(apiCount);
    }

    /**
     * 获取操作统计信息
     *
     * @param operationName 操作名称
     * @return 统计信息字符串
     */
    public static String getOperationStats(String operationName) {
        AtomicLong count = operationCounters.get(operationName);
        AtomicLong totalTime = operationTimers.get(operationName);

        if (count == null || totalTime == null) {
            return operationName + ": No data";
        }

        long countValue = count.get();
        long totalTimeValue = totalTime.get();
        double avgTime = countValue > 0 ? (double) totalTimeValue / countValue : 0;

        return String.format("%s: count=%d, totalTime=%dms, avgTime=%.2fms",
                operationName, countValue, totalTimeValue, avgTime);
    }

    /**
     * 获取JVM内存使用情况
     *
     * @return 内存使用信息字符串
     */
    public static String getMemoryInfo() {
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

        return String.format("JVM Memory: Heap=%.1fMB/%.1fMB (%.1f%%), NonHeap=%.1fMB",
                heapUsed / 1024.0 / 1024.0,
                heapMax / 1024.0 / 1024.0,
                heapUsagePercent,
                nonHeapUsed / 1024.0 / 1024.0);
    }

    /**
     * 获取JVM线程信息
     *
     * @return 线程信息字符串
     */
    public static String getThreadInfo() {
        int threadCount = threadBean.getThreadCount();
        int daemonCount = threadBean.getDaemonThreadCount();
        int peakCount = threadBean.getPeakThreadCount();

        return String.format("JVM Threads: current=%d, daemon=%d, peak=%d",
                threadCount, daemonCount, peakCount);
    }

    /**
     * 获取RequestMan插件专用统计信息
     *
     * @return 插件统计信息字符串
     */
    public static String getPluginStats() {
        long threadCount = pluginThreadCount.get();
        long cacheSize = pluginCacheSize.get();
        long apiCount = pluginApiCount.get();

        return String.format("RequestMan Plugin: Threads=%d, Cache=%d, APIs=%d",
                threadCount, cacheSize, apiCount);
    }

    /**
     * 获取完整的性能报告
     *
     * @return 性能报告字符串
     */
    public static String getPerformanceReport() {
        // 如果监控未启用，返回提示信息
        if (!isMonitoringEnabled()) {
            return "=== RequestMan Performance Report ===\n" +
                    "性能监控未启用。请在设置页面启用性能监控后查看详细报告。\n" +
                    "--- System Information (JVM-wide) ---\n" +
                    getMemoryInfo() + "\n" +
                    getThreadInfo() + "\n";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== RequestMan Performance Report ===\n");
        report.append("--- RequestMan Plugin Statistics ---\n");
        report.append(getPluginStats()).append("\n");
        report.append("--- System Information (JVM-wide) ---\n");
        report.append(getMemoryInfo()).append("\n");
        report.append(getThreadInfo()).append("\n");
        report.append("--- RequestMan Operation Statistics ---\n");

        operationCounters.keySet().stream()
                .sorted()
                .forEach(op -> report.append(getOperationStats(op)).append("\n"));

        return report.toString();
    }

    /**
     * 清理统计数据
     */
    public static void clearStats() {
        operationCounters.clear();
        operationTimers.clear();
        pluginThreadCount.set(0);
        pluginCacheSize.set(0);
        pluginApiCount.set(0);
    }

    /**
     * 检查是否存在性能问题
     *
     * @return 性能问题描述，如果没有问题返回null
     */
    public static String checkPerformanceIssues() {
        // 如果监控未启用，只检查基本的JVM级别问题
        if (!isMonitoringEnabled()) {
            StringBuilder issues = new StringBuilder();

            // 检查内存使用
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

            if (heapUsagePercent > 80) {
                issues.append("High memory usage: ").append(String.format("%.1f%%", heapUsagePercent)).append("\n");
            }

            // 检查线程数量
            int threadCount = threadBean.getThreadCount();
            if (threadCount > 100) {
                issues.append("High thread count: ").append(threadCount).append("\n");
            }

            return issues.length() > 0 ? issues.toString() : null;
        }

        StringBuilder issues = new StringBuilder();

        // 检查内存使用
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

        if (heapUsagePercent > 80) {
            issues.append("High memory usage: ").append(String.format("%.1f%%", heapUsagePercent)).append("\n");
        }

        // 检查线程数量
        int threadCount = threadBean.getThreadCount();
        if (threadCount > 100) {
            issues.append("High thread count: ").append(threadCount).append("\n");
        }

        // 检查慢操作
        operationTimers.forEach((op, timer) -> {
            long totalTime = timer.get();
            AtomicLong count = operationCounters.get(op);
            if (count != null && count.get() > 0) {
                double avgTime = (double) totalTime / count.get();
                // 平均耗时超过1秒
                if (avgTime > 1000) {
                    issues.append("Slow operation: ").append(op).append(" (avg: ").append(String.format("%.2fms", avgTime)).append(")\n");
                }
            }
        });

        return issues.length() > 0 ? issues.toString() : null;
    }
} 