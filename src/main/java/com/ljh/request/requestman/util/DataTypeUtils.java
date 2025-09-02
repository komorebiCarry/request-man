package com.ljh.request.requestman.util;

import com.intellij.psi.*;
import com.ljh.request.requestman.enums.ParamDataType;

/**
 * @author leijianhui
 * @Description 数据类型推断工具类，统一处理各种类型的推断逻辑。
 * @date 2025/01/27 16:00
 */
public class DataTypeUtils {
    
    /**
     * Java 标准包前缀
     */
    private static final String JAVA_PACKAGE_PREFIX = "java.";
    
    /**
     * 基础类型常量
     */
    private static final String INT_TYPE = "int";
    private static final String INTEGER_TYPE = "Integer";
    private static final String LONG_TYPE = "long";
    private static final String LONG_CLASS_TYPE = "Long";
    private static final String SHORT_TYPE = "short";
    private static final String SHORT_CLASS_TYPE = "Short";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String BOOLEAN_CLASS_TYPE = "Boolean";
    private static final String DOUBLE_TYPE = "double";
    private static final String DOUBLE_CLASS_TYPE = "Double";
    private static final String FLOAT_TYPE = "float";
    private static final String FLOAT_CLASS_TYPE = "Float";
    private static final String BIGDECIMAL_TYPE = "BigDecimal";
    private static final String BIGDECIMAL_FULL_TYPE = "java.math.BigDecimal";
    private static final String STRING_TYPE = "String";
    private static final String JAVA_STRING_TYPE = "java.lang.String";
    
    /**
     * 集合类型常量
     */
    private static final String LIST_TYPE = "java.util.List";
    private static final String ARRAYLIST_TYPE = "java.util.ArrayList";
    private static final String COLLECTION_TYPE = "java.util.Collection";
    
    /**
     * 文件类型常量
     */
    private static final String FILE_TYPE = "java.io.File";
    private static final String MULTIPART_FILE_TYPE = "org.springframework.web.multipart.MultipartFile";
    
    /**
     * 时间类型常量
     */
    private static final String DATE_TYPE = "Date";
    private static final String JAVA_DATE_TYPE = "java.util.Date";
    private static final String LOCAL_DATE_TIME_TYPE = "LocalDateTime";
    private static final String JAVA_LOCAL_DATE_TIME_TYPE = "java.time.LocalDateTime";
    private static final String LOCAL_DATE_TYPE = "LocalDate";
    private static final String JAVA_LOCAL_DATE_TYPE = "java.time.LocalDate";
    private static final String LOCAL_TIME_TYPE = "LocalTime";
    private static final String JAVA_LOCAL_TIME_TYPE = "java.time.LocalTime";
    private static final String TIMESTAMP_TYPE = "Timestamp";
    private static final String JAVA_TIMESTAMP_TYPE = "java.sql.Timestamp";
    private static final String CALENDAR_TYPE = "Calendar";
    private static final String JAVA_CALENDAR_TYPE = "java.util.Calendar";

    /**
     * 工具类禁止实例化
     */
    private DataTypeUtils() {
        throw new UnsupportedOperationException("DataTypeUtils is a utility class and cannot be instantiated.");
    }
    
    /**
     * 推断参数数据类型
     *
     * @param type PsiType
     * @return ParamDataType
     */
    public static ParamDataType guessDataType(PsiType type) {
        if (type == null) {
            return ParamDataType.STRING;
        }
        
        String presentableText = type.getPresentableText();
        String canonicalText = type.getCanonicalText();
        
        // 基础类型判断
        if (isIntegerType(presentableText)) {
            return ParamDataType.INTEGER;
        }
        if (isBooleanType(presentableText)) {
            return ParamDataType.BOOLEAN;
        }
        if (isNumberType(presentableText, canonicalText)) {
            return ParamDataType.NUMBER;
        }
        if (isStringType(presentableText, canonicalText)) {
            return ParamDataType.STRING;
        }
        
        // 时间类型判断
        if (isTimeType(presentableText, canonicalText)) {
            return ParamDataType.STRING;
        }

        // 数组类型判断
        if (type instanceof PsiArrayType) {
            return ParamDataType.ARRAY;
        }
        
        // 通配符类型处理
        if (type instanceof PsiWildcardType) {
            return ParamDataType.OBJECT;
        }
        
        // 集合类型判断
        if (type instanceof PsiClassType classType) {
            return guessClassType(classType);
        }
        
        // 默认返回STRING
        return ParamDataType.STRING;
    }
    
    /**
     * 推断参数数据类型（从PsiParameter）
     *
     * @param param PsiParameter
     * @return ParamDataType
     */
    public static ParamDataType guessDataType(PsiParameter param) {
        if (param == null) {
            return ParamDataType.STRING;
        }
        return guessDataType(param.getType());
    }
    
    /**
     * 推断字段数据类型（从PsiField）
     *
     * @param field PsiField
     * @return ParamDataType
     */
    public static ParamDataType guessDataType(PsiField field) {
        if (field == null) {
            return ParamDataType.UNKNOWN;
        }
        return guessDataType(field.getType());
    }
    
    /**
     * 判断是否为整数类型
     */
    private static boolean isIntegerType(String typeName) {
        return INT_TYPE.equals(typeName) || INTEGER_TYPE.equals(typeName) || 
               LONG_TYPE.equals(typeName) || LONG_CLASS_TYPE.equals(typeName) ||
               SHORT_TYPE.equals(typeName) || SHORT_CLASS_TYPE.equals(typeName);
    }
    
    /**
     * 判断是否为布尔类型
     */
    private static boolean isBooleanType(String typeName) {
        return BOOLEAN_TYPE.equals(typeName) || BOOLEAN_CLASS_TYPE.equals(typeName);
    }
    
    /**
     * 判断是否为数值类型
     */
    private static boolean isNumberType(String presentableText, String canonicalText) {
        return DOUBLE_TYPE.equals(presentableText) || DOUBLE_CLASS_TYPE.equals(presentableText) ||
               FLOAT_TYPE.equals(presentableText) || FLOAT_CLASS_TYPE.equals(presentableText) ||
               BIGDECIMAL_TYPE.equals(presentableText) || BIGDECIMAL_FULL_TYPE.equals(canonicalText);
    }
    
    /**
     * 判断是否为字符串类型
     */
    private static boolean isStringType(String presentableText, String canonicalText) {
        return STRING_TYPE.equals(presentableText) || JAVA_STRING_TYPE.equals(canonicalText);
    }
    
    /**
     * 判断是否为时间类型
     */
    private static boolean isTimeType(String presentableText, String canonicalText) {
        return DATE_TYPE.equals(presentableText) || JAVA_DATE_TYPE.equals(canonicalText) ||
               LOCAL_DATE_TIME_TYPE.equals(presentableText) || JAVA_LOCAL_DATE_TIME_TYPE.equals(canonicalText) ||
               LOCAL_DATE_TYPE.equals(presentableText) || JAVA_LOCAL_DATE_TYPE.equals(canonicalText) ||
               LOCAL_TIME_TYPE.equals(presentableText) || JAVA_LOCAL_TIME_TYPE.equals(canonicalText) ||
               TIMESTAMP_TYPE.equals(presentableText) || JAVA_TIMESTAMP_TYPE.equals(canonicalText) ||
               CALENDAR_TYPE.equals(presentableText) || JAVA_CALENDAR_TYPE.equals(canonicalText);
    }

    /**
     * 推断类类型的数据类型
     */
    private static ParamDataType guessClassType(PsiClassType classType) {
        PsiClass psiClass = classType.resolve();
        if (psiClass == null) {
            return ParamDataType.STRING;
        }
        
        String qualifiedName = psiClass.getQualifiedName();
        
        // 枚举类型判断
        if (psiClass.isEnum()) {
            return ParamDataType.ENUM;
        }
        
        // 集合类型判断
        if (isCollectionType(qualifiedName)) {
            return ParamDataType.ARRAY;
        }
        
        // 文件类型判断
        if (isFileType(qualifiedName)) {
            return ParamDataType.FILE;
        }
        
        // 时间类型判断
        if (isTimeType(psiClass.getName(), qualifiedName)) {
            return ParamDataType.STRING;
        }

        // 泛型类型参数
        if (psiClass instanceof PsiTypeParameter) {
            return ParamDataType.OBJECT;
        }
        
        // 自定义类型（非JDK内置）
        if (qualifiedName != null && !qualifiedName.startsWith(JAVA_PACKAGE_PREFIX)) {
            return ParamDataType.OBJECT;
        }
        
        // 默认返回STRING，避免递归扫描导致栈溢出
        return ParamDataType.STRING;
    }
    
    /**
     * 判断是否为集合类型
     */
    public static boolean isCollectionType(String qualifiedName) {
        return LIST_TYPE.equals(qualifiedName) || ARRAYLIST_TYPE.equals(qualifiedName) ||
               COLLECTION_TYPE.equals(qualifiedName);
    }
    
    /**
     * 判断是否为文件类型
     */
    private static boolean isFileType(String qualifiedName) {
        if (qualifiedName == null) {
            return false;
        }
        return FILE_TYPE.equals(qualifiedName) || MULTIPART_FILE_TYPE.equals(qualifiedName)
                || qualifiedName.endsWith(".MultipartFile");
    }
}
