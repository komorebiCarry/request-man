package com.ljh.request.requestman.enums;

import com.ljh.request.requestman.util.RequestManBundle;

/**
 * @author leijianhui
 * @Description ContentType 枚举，常用MIME类型。
 * @date 2025/06/17 16:32
 */
public enum ContentType {
    /**
     * 纯文本
     */
    TEXT_PLAIN("text/plain", "contenttype.text.plain"),
    /**
     * HTML文档
     */
    TEXT_HTML("text/html", "contenttype.text.html"),
    /**
     * CSS样式表
     */
    TEXT_CSS("text/css", "contenttype.text.css"),
    /**
     * JS脚本
     */
    TEXT_JAVASCRIPT("text/javascript", "contenttype.text.javascript"),
    /**
     * JSON格式
     */
    APPLICATION_JSON("application/json", "contenttype.application.json"),
    /**
     * XML格式
     */
    APPLICATION_XML("application/xml", "contenttype.application.xml"),
    /**
     * 表单数据
     */
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded", "contenttype.application.form"),
    /**
     * 多部分表单
     */
    MULTIPART_FORM_DATA("multipart/form-data", "contenttype.multipart.form"),
    /**
     * 二进制流
     */
    APPLICATION_OCTET_STREAM("application/octet-stream", "contenttype.application.octet"),
    /**
     * PDF文档
     */
    APPLICATION_PDF("application/pdf", "contenttype.application.pdf"),
    /**
     * PNG图片
     */
    IMAGE_PNG("image/png", "contenttype.image.png"),
    /**
     * JPEG图片
     */
    IMAGE_JPEG("image/jpeg", "contenttype.image.jpeg"),
    /**
     * MP3音频
     */
    AUDIO_MPEG("audio/mpeg", "contenttype.audio.mpeg"),
    /**
     * MP4视频
     */
    VIDEO_MP4("video/mp4", "contenttype.video.mp4"),
    /**
     * Excel文件
     */
    APPLICATION_EXCEL("application/vnd.ms-excel", "contenttype.application.excel"),
    /**
     * 新版Excel文件(.xlsx)
     */
    APPLICATION_EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "contenttype.application.excelxlsx");

    /**
     * MIME类型字符串
     */
    private final String value;
    /**
     * 类型国际化键
     */
    private final String labelKey;

    ContentType(String value, String labelKey) {
        this.value = value;
        this.labelKey = labelKey;
    }

    /**
     * 获取MIME类型字符串
     *
     * @return MIME类型
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取类型国际化描述
     *
     * @return 国际化描述
     */
    public String getLabel() {
        return RequestManBundle.message(labelKey);
    }

    @Override
    public String toString() {
        return value + " (" + getLabel() + ")";
    }
} 