package com.ljh.request.requestman.enums;

/**
 * @author leijianhui
 * @Description ContentType 枚举，常用MIME类型。
 * @date 2025/06/17 16:32
 */
public enum ContentType {
    TEXT_PLAIN("text/plain", "纯文本"),
    TEXT_HTML("text/html", "HTML文档"),
    TEXT_CSS("text/css", "CSS样式表"),
    TEXT_JAVASCRIPT("text/javascript", "JS脚本"),
    APPLICATION_JSON("application/json", "JSON格式"),
    APPLICATION_XML("application/xml", "XML格式"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded", "表单数据"),
    MULTIPART_FORM_DATA("multipart/form-data", "多部分表单"),
    APPLICATION_OCTET_STREAM("application/octet-stream", "二进制流"),
    APPLICATION_PDF("application/pdf", "PDF文档"),
    IMAGE_PNG("image/png", "PNG图片"),
    IMAGE_JPEG("image/jpeg", "JPEG图片"),
    AUDIO_MPEG("audio/mpeg", "MP3音频"),
    VIDEO_MP4("video/mp4", "MP4视频"),
    APPLICATION_EXCEL("application/vnd.ms-excel", "Excel文件"),
    APPLICATION_EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "新版Excel文件(.xlsx)");

    /**
     * MIME类型字符串
     */
    private final String value;
    /**
     * 类型中文描述
     */
    private final String label;

    ContentType(String value, String label) {
        this.value = value;
        this.label = label;
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
     * 获取类型中文描述
     *
     * @return 中文描述
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return value + " (" + label + ")";
    }
} 