package com.ljh.request.requestman.search;

import java.util.Collections;

/**
 * 分页加载更多占位符类，用于UI展示"加载更多..."提示。
 * 继承自ApiSearchEntry，提供特殊的toString实现用于分页显示。
 *
 * @author leijianhui
 * @Description 分页加载更多占位符类，用于UI展示"加载更多..."提示。
 * @date 2025/06/19 21:05
 */
public class MorePlaceholder extends ApiSearchEntry {
    
    /**
     * 构造函数，创建空的占位符对象
     */
    public MorePlaceholder() {
        super("", "", "", "", "", Collections.emptyList());
    }

    @Override
    public String toString() {
        return "...";
    }
}
