package com.ljh.request.requestman.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目级别的API缓存管理器，解决多工程缓存混淆问题。
 * 每个项目维护独立的缓存实例，包含项目内API和全量API两个缓存池。
 * 支持缓存状态管理和清理操作。
 *
 * @author leijianhui
 * @Description 项目级别的API缓存管理器，解决多工程缓存混淆问题。
 * @date 2025/06/19 21:15
 */
public class ProjectCache {
    
    /**
     * 项目内API缓存列表
     */
    private final List<ApiSearchEntry> projectApisCache = new ArrayList<>();
    
    /**
     * 全量API缓存列表（包含第三方包）
     */
    private final List<ApiSearchEntry> allApisCache = new ArrayList<>();
    
    /**
     * 项目缓存就绪状态
     */
    private volatile boolean projectCacheReady = false;
    
    /**
     * 全量缓存就绪状态
     */
    private volatile boolean allCacheReady = false;

    /**
     * 构造函数
     *
     * @param projectName 项目名称
     */
    public ProjectCache(String projectName) {
        // 简单的缓存，不需要复杂的清理逻辑
    }
    
    /**
     * 获取项目内API缓存列表
     *
     * @return 项目内API缓存列表
     */
    public List<ApiSearchEntry> getProjectApisCache() {
        return projectApisCache;
    }
    
    /**
     * 获取全量API缓存列表
     *
     * @return 全量API缓存列表
     */
    public List<ApiSearchEntry> getAllApisCache() {
        return allApisCache;
    }
    
    /**
     * 检查项目缓存是否就绪
     *
     * @return 项目缓存就绪状态
     */
    public boolean isProjectCacheReady() {
        return projectCacheReady;
    }
    
    /**
     * 检查全量缓存是否就绪
     *
     * @return 全量缓存就绪状态
     */
    public boolean isAllCacheReady() {
        return allCacheReady;
    }
    
    /**
     * 设置项目缓存就绪状态
     *
     * @param projectCacheReady 项目缓存就绪状态
     */
    public void setProjectCacheReady(boolean projectCacheReady) {
        this.projectCacheReady = projectCacheReady;
    }
    
    /**
     * 设置全量缓存就绪状态
     *
     * @param allCacheReady 全量缓存就绪状态
     */
    public void setAllCacheReady(boolean allCacheReady) {
        this.allCacheReady = allCacheReady;
    }
    
    /**
     * 清理项目内API缓存
     */
    public void clearProjectApisCache() {
        projectApisCache.clear();
    }
    
    /**
     * 清理全量API缓存
     */
    public void clearAllApisCache() {
        allApisCache.clear();
    }
}
