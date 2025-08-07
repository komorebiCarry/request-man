package com.ljh.request.requestman.model;

import java.util.Objects;

/**
 * 环境配置模型，用于管理多环境的前置URL、全局认证等配置。
 *
 * @author leijianhui
 * @Description 环境配置模型，管理多环境的前置URL、全局认证等配置。
 * @date 2025/01/27 16:30
 */
public class Environment {

    /**
     * 环境ID
     */
    private String id;

    /**
     * 环境名称
     */
    private String name;

    /**
     * 前置URL
     */
    private String preUrl;


    // 移除默认环境字段

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 更新时间
     */
    private long updateTime;

    public Environment() {
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    public Environment(String name, String preUrl) {
        this();
        this.id = generateId();
        this.name = name;
        this.preUrl = preUrl;
        // 全局认证现在从项目级别获取，不在环境中存储
    }

    /**
     * 生成环境ID
     */
    private String generateId() {
        return "env_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    /**
     * 获取环境显示名称（用于下拉框显示）
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * 获取环境缩写（用于下拉框显示）
     */
    public String getAbbreviation() {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        String trimmedName = name.trim();

        // 如果第一个字符是英文，第二个是中文，取中文第一个字
        if (trimmedName.length() >= 2) {
            char first = trimmedName.charAt(0);
            char second = trimmedName.charAt(1);

            if (isEnglish(first) && isChinese(second)) {
                return String.valueOf(second);
            }
        }

        // 中文用第一个字，英文用前2个字符
        if (isChinese(trimmedName.charAt(0))) {
            return String.valueOf(trimmedName.charAt(0));
        } else {
            return trimmedName.length() >= 2 ? trimmedName.substring(0, 2) : trimmedName;
        }
    }

    /**
     * 判断是否为英文字符
     */
    private boolean isEnglish(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updateTime = System.currentTimeMillis();
    }

    public String getPreUrl() {
        return preUrl;
    }

    public void setPreUrl(String preUrl) {
        this.preUrl = preUrl;
        this.updateTime = System.currentTimeMillis();
    }


    // 移除默认环境相关方法

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
} 