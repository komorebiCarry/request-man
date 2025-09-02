package com.ljh.request.requestman.search;

import java.util.Collections;

/**
 * 搜索加载中占位符类，用于UI展示"Searching..."提示。
 * 继承自ApiSearchEntry，提供特殊的toString实现用于加载状态显示。
 *
 * @author leijianhui
 * @Description 搜索加载中占位符类，用于UI展示"Searching..."提示。
 * @date 2025/06/19 21:05
 */
public class LoadingPlaceholder extends ApiSearchEntry {
    
    /**
     * 构造函数，创建空的占位符对象
     */
    public LoadingPlaceholder() {
        super("", "", "", "", "", Collections.emptyList());
    }

    @Override
    public String toString() {
        return "Searching...";
    }
}
