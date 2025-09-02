package com.ljh.request.requestman.enums;

/**
 * @author leijianhui
 * @Description 参数数据类型枚举，支持string、integer、boolean、number、array、file等。
 * @date 2025/06/17 16:32
 */
public enum ParamDataType {
    /**
     * 字符串类型
     */
    STRING,
    /**
     * 整数类型
     */
    INTEGER,
    /**
     * 布尔类型
     */
    BOOLEAN,
    /**
     * 数值类型
     */
    NUMBER,
    /**
     * 数组类型
     */
    ARRAY,
    /**
     * 文件类型
     */
    FILE,
    /**
     * 未知类型
     */
    UNKNOWN,
    /**
     * 对象类型
     */
    OBJECT,
    /**
     * 枚举类型
     */
    ENUM
    ;

    public static ParamDataType getParamDataType(String value) {
        for (ParamDataType paramDataType : ParamDataType.values()) {
            if (paramDataType.toString().equalsIgnoreCase(value)) {
                return paramDataType;
            }
        }
        return STRING;
    }
} 