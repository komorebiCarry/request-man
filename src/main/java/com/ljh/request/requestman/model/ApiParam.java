package com.ljh.request.requestman.model;

import com.ljh.request.requestman.enums.ParamDataType;

import java.util.List;

/**
 * @author leijianhui
 * @Description 接口参数数据结构，包含参数名、值、类型、描述等。
 * @date 2025/06/18 15:06
 */
public class ApiParam {
    /**
     * 参数名
     */
    private String name;
    /**
     * 业务类型，如"路径变量"、"请求参数"等
     */
    private String type;
    /**
     * 参数说明
     */
    private String description;
    /**
     * 数据类型，如string、integer等
     */
    private ParamDataType dataType;
    /**
     * 嵌套参数（如对象、数组等），用于递归生成json和参数树
     */
    private List<ApiParam> children;
    /**
     * 原始类型名，如String、Integer、EscOnRoadToArrivalQueryParam等
     */
    private String rawType;
    /**
     * 原始类型完整名，如String、Integer、EscOnRoadToArrivalQueryParam等
     */
    private String rawCanonicalType;
    /**
     * 参数值
     */
    private String value;
    /**
     * Content-Type信息，用于@RequestBody参数的consumes属性
     */
    private String contentType;

    /**
     * 无参构造方法
     */
    public ApiParam() {
    }

    /**
     * 全参构造方法
     *
     * @param name             参数名
     * @param type             业务类型
     * @param description      参数说明
     * @param dataType         数据类型
     * @param rawCanonicalType 原始类型完整名
     */
    public ApiParam(String name, String type, String description, ParamDataType dataType, String rawCanonicalType) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.dataType = dataType;
        this.rawCanonicalType = rawCanonicalType;
    }

    /**
     * 全参构造方法（包含Content-Type）
     *
     * @param name             参数名
     * @param type             业务类型
     * @param description      参数说明
     * @param dataType         数据类型
     * @param rawCanonicalType 原始类型完整名
     * @param contentType      Content-Type信息
     */
    public ApiParam(String name, String type, String description, ParamDataType dataType, String rawCanonicalType, String contentType) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.dataType = dataType;
        this.rawCanonicalType = rawCanonicalType;
        this.contentType = contentType;
    }

    /**
     * 获取参数名
     *
     * @return 参数名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置参数名
     *
     * @param name 参数名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取业务类型
     *
     * @return 业务类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置业务类型
     *
     * @param type 业务类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取参数说明
     *
     * @return 参数说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置参数说明
     *
     * @param description 参数说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取数据类型
     *
     * @return 数据类型
     */
    public ParamDataType getDataType() {
        return dataType;
    }

    /**
     * 设置数据类型
     *
     * @param dataType 数据类型
     */
    public void setDataType(ParamDataType dataType) {
        this.dataType = dataType;
    }

    /**
     * 获取嵌套参数
     *
     * @return 嵌套参数
     */
    public List<ApiParam> getChildren() {
        return children;
    }

    /**
     * 设置嵌套参数
     *
     * @param children 嵌套参数
     */
    public void setChildren(List<ApiParam> children) {
        this.children = children;
    }

    /**
     * 获取原始类型名
     *
     * @return 原始类型名
     */
    public String getRawType() {
        return rawType;
    }

    /**
     * 设置原始类型名
     *
     * @param rawType 原始类型名
     */
    public void setRawType(String rawType) {
        this.rawType = rawType;
    }

    /**
     * 获取参数值
     *
     * @return 参数值
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置参数值
     *
     * @param value 参数值
     */
    public void setValue(String value) {
        this.value = value;
    }

    public String getRawCanonicalType() {
        return rawCanonicalType;
    }

    public void setRawCanonicalType(String rawCanonicalType) {
        this.rawCanonicalType = rawCanonicalType;
    }

    /**
     * 获取Content-Type信息
     *
     * @return Content-Type信息
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置Content-Type信息
     *
     * @param contentType Content-Type信息
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}