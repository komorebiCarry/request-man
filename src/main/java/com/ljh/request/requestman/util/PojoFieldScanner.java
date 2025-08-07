package com.ljh.request.requestman.util;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author leijianhui
 * @Description Pojo字段扫描工具类，递归扫描PsiClass的所有字段，生成参数树结构。
 * @date 2025/06/17 19:48
 */
public class PojoFieldScanner {
    /**
     * Java 标准包前缀
     */
    private static final String JAVA_PACKAGE_PREFIX = "java.";
    /**
     * List 类型全限定名
     */
    private static final String LIST_CLASS = "java.util.List";
    /**
     * ArrayList 类型全限定名
     */
    private static final String ARRAYLIST_CLASS = "java.util.ArrayList";
    private static final String STRING_TYPE = "String";
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
    private static final String MULTIPART_FILE_TYPE = "MultipartFile";
    private static final String ARRAY_SUFFIX = "[]";
    private static final String JAVA_STRING_TYPE = "java.lang.String";
    private static final String LIST_TYPE = "java.util.List";
    private static final String ARRAYLIST_TYPE = "java.util.ArrayList";
    private static final String COLLECTION_TYPE = "java.util.Collection";
    private static final String FILE_TYPE = "java.io.File";
    /**
     * 默认 Map 容量
     */
    private static final int DEFAULT_MAP_SIZE = 8;

    /**
     * 工具类禁止实例化。
     */
    private PojoFieldScanner() {
        throw new UnsupportedOperationException("PojoFieldScanner is a utility class and cannot be instantiated.");
    }

    /**
     * 递归扫描 PsiClass 的所有字段，生成参数树结构。
     *
     * @param psiClass PsiClass 对象，不能为空
     * @param project  当前 Project 对象
     * @return 参数树结构 List<ApiParam>
     */
    public static List<ApiParam> scanPojoFields(PsiClass psiClass, Project project) {
        return ReadAction.compute(() ->
                scanPojoFieldsWithGenerics(psiClass, null, new HashSet<>(DEFAULT_MAP_SIZE), new HashMap<>(DEFAULT_MAP_SIZE), project)
        );
    }

    /**
     * 递归扫描 PsiClass 的所有字段，支持泛型递归，防止死循环，支持泛型参数映射。
     *
     * @param psiClass   PsiClass 对象
     * @param classDoc   类注释（可为空）
     * @param visited    已访问类集合（防止死循环）
     * @param genericMap 泛型参数映射
     * @param project    当前 Project 对象
     * @return 参数树结构 List<ApiParam>
     */
    public static List<ApiParam> scanPojoFieldsWithGenerics(PsiClass psiClass, String classDoc, Set<String> visited, Map<String, PsiType> genericMap, Project project) {
        return ReadAction.compute(() -> {
            LogUtil.debug("[PojoFieldScanner] 递归类型: " + (psiClass != null ? psiClass.getQualifiedName() : "null") + ", visited: " + visited + ", 泛型映射: " + genericMap);
            List<ApiParam> params = new ArrayList<>();
            if (psiClass == null) {
                return params;
            }
            String classKey = psiClass.getQualifiedName();
            // 死循环防护：只对具体类（非接口、非抽象类）做防护，接口允许递归一次
            if (classKey != null && visited.contains(classKey) && !psiClass.isInterface() && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                return params;
            }
            if (classKey != null && !psiClass.isInterface() && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                visited.add(classKey);
            }
            // 获取类注释
            String classDesc = getClassDescription(psiClass, classDoc);
            // 构建当前类的泛型参数映射
            Map<String, PsiType> localGenericMap = new HashMap<>(!genericMap.isEmpty() ? genericMap.size() : DEFAULT_MAP_SIZE);
            localGenericMap.putAll(genericMap);
            // 扫描所有字段
            for (PsiField field : psiClass.getAllFields()) {
                LogUtil.debug("[PojoFieldScanner] 字段: " + field.getName() + ", 类型: " + field.getType().getPresentableText());
                if (isConstantField(field)) {
                    continue;
                }
                ApiParam param = buildApiParamFromField(field, classDesc, localGenericMap, visited, project);
                params.add(param);
            }
            return params;
        });
    }

    /**
     * 判断字段是否为 public static final 常量字段。
     *
     * @param field PsiField
     * @return true-常量字段，false-普通字段
     */
    private static boolean isConstantField(PsiField field) {
        return field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.FINAL);
    }

    /**
     * 获取类注释（优先 Swagger 注解，无则 JavaDoc）。
     *
     * @param psiModifierListOwner PsiModifierListOwner
     * @param classDoc             外部传入注释（可为空）
     * @return 类注释字符串
     */
    public static String getClassDescription(PsiModifierListOwner psiModifierListOwner, String classDoc) {
        if (classDoc != null) {
            return classDoc;
        }
        if (psiModifierListOwner == null) {
            return "";
        }
        String desc = "";
        for (PsiAnnotation annotation : psiModifierListOwner.getAnnotations()) {
            String qName = annotation.getQualifiedName();
            if (qName == null) {
                continue;
            }
            if (qName.endsWith("ApiModel")) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value != null) {
                    desc = value.getText().replaceAll("^[\"']|[\"']$", "");
                    break;
                }
            }
            if (qName.endsWith("ApiModelProperty")) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value != null) {
                    desc = value.getText().replaceAll("^[\"']|[\"']$", "");
                    break;
                }
            }
            if (qName.endsWith("Schema")) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("description");
                if (value != null) {
                    desc = value.getText().replaceAll("^[\"']|[\"']$", "");
                    break;
                }
            }
        }
        if (desc.isEmpty()) {
            PsiDocComment docComment = null;
            if (psiModifierListOwner instanceof PsiClass) {
                docComment = ((PsiClass) psiModifierListOwner).getDocComment();
            } else if (psiModifierListOwner instanceof PsiField) {
                docComment = ((PsiField) psiModifierListOwner).getDocComment();
            }
            if (docComment != null) {
                desc = cleanJavaDoc(docComment.getText());
            }
        }

        return desc;
    }

    /**
     * 构建 ApiParam 对象，递归处理泛型、集合、对象等复杂类型。
     *
     * @param field           PsiField
     * @param classDesc       类注释
     * @param localGenericMap 当前泛型映射
     * @param visited         已访问类集合
     * @param project         当前 Project 对象
     * @return ApiParam
     */
    public static ApiParam buildApiParamFromField(PsiField field, String classDesc, Map<String, PsiType> localGenericMap, Set<String> visited, Project project) {
        String name = field.getName();
        String desc = getClassDescription(field, null);
        ParamDataType dataType = guessDataType(field.getType());
        String rawType = field.getType().getPresentableText();
        ApiParam param = new ApiParam();
        param.setName(name);
        param.setDescription(desc);
        param.setDataType(dataType);
        param.setRawType(rawType);
        // 泛型参数递归
        if (field.getType() instanceof PsiClassType) {
            String typeName = field.getType().getCanonicalText();
            if (localGenericMap.containsKey(typeName)) {
                PsiType realType = localGenericMap.get(typeName);
                LogUtil.debug("[PojoFieldScanner] 泛型参数递归: " + typeName + " -> " + realType);
                param.setRawType(realType.getPresentableText());
                // 检查realType是否为基础类型，如果是则不需要继续递归扫描
                ParamDataType realDataType = guessDataType(realType);
                param.setDataType(realDataType);
                if (realDataType == ParamDataType.OBJECT) {
                    handleGenericType(param, realType, visited, localGenericMap, project);
                } else {
                    return param;
                }
            }
        }
        // 泛型字段递归优化：区分 List<T> 和自定义泛型类
        if (field.getType() instanceof PsiClassType classType) {
            PsiClass rawClass = classType.resolve();
            PsiType[] typeArgs = classType.getParameters();
            if (rawClass != null) {
                String rawClassName = rawClass.getQualifiedName();
                if (rawClassName == null) {
                    String oldDesc = param.getDescription();
                    param.setDescription((oldDesc == null ? "" : oldDesc) + "");
                    return param;
                }
                if (isCollectionType(rawClassName)) {
                    handleCollectionType(param, typeArgs, localGenericMap, visited, project);
                } else if (!rawClassName.startsWith(JAVA_PACKAGE_PREFIX)) {
                    handleCustomType(param, rawClass, typeArgs, visited, field.getType(), project);
                }
            }
        }
        // 如果是自定义类型，类型列显示类名，注释优先显示类注释
        if (dataType == ParamDataType.OBJECT) {
            PsiClass fieldClass = PsiUtil.resolveClassInType(field.getType());
            if (fieldClass != null) {
                param.setRawType(fieldClass.getName());
                param.setDescription((classDesc != null && !classDesc.isEmpty()) ? classDesc : desc);
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                param.getChildren().addAll(scanPojoFieldsWithGenerics(fieldClass, classDesc, visited, localGenericMap, project));
            }
        }
        return param;
    }

    /**
     * 处理泛型参数递归。
     */
    public static void handleGenericType(ApiParam param, PsiType realType, Set<String> visited, Map<String, PsiType> localGenericMap, Project project) {
        if (realType instanceof PsiClassType) {
            PsiClass realClass = ((PsiClassType) realType).resolve();
            if (realClass != null) {
                String canonical = realClass.getQualifiedName();
                PsiType[] typeArgs = ((PsiClassType) realType).getParameters();
                if (isCollectionType(canonical)) {
                    if (typeArgs.length == 1) {
                        PsiType elementType = typeArgs[0];
                        ParamDataType elementDataType = guessDataType(elementType);
                        param.setDataType(ParamDataType.ARRAY);
                        param.setChildren(new ArrayList<>());
                        if (elementDataType == ParamDataType.OBJECT) {
                            PsiClass elementClass = null;
                            if (elementType instanceof PsiClassType) {
                                elementClass = ((PsiClassType) elementType).resolve();
                            }
                            if (elementClass != null) {
                                param.setDescription(getClassDescription(elementClass));
                                Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
                                implTypeArgMap.put("T", typeArgs[0]);
                                param.setChildren(scanPojoFieldsWithGenerics(elementClass, null, visited, implTypeArgMap, project));
                            }
                        }
                    }
                } else if (!canonical.startsWith(JAVA_PACKAGE_PREFIX)) {
                    handleCustomType(param, realClass, typeArgs, visited, realType, project);
                } else {
                    if (param.getChildren() == null) {
                        param.setChildren(new ArrayList<>());
                    }
                    param.getChildren().addAll(scanPojoFieldsWithGenerics(realClass, null, visited, localGenericMap, project));
                }
            }
        } else if (realType != null) {
            PsiClass globalClass = JavaPsiFacade.getInstance(project).findClass(realType.getCanonicalText(), GlobalSearchScope.allScope(project));
            if (globalClass != null) {
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                param.getChildren().addAll(scanPojoFieldsWithGenerics(globalClass, null, visited, localGenericMap, project));
            }
        }
    }

    /**
     * 判断是否为集合类型。
     */
    private static boolean isCollectionType(String rawClassName) {
        return LIST_CLASS.equals(rawClassName) || ARRAYLIST_CLASS.equals(rawClassName);
    }

    /**
     * 处理集合类型字段递归。
     */
    private static void handleCollectionType(ApiParam param, PsiType[] typeArgs, Map<String, PsiType> localGenericMap, Set<String> visited, Project project) {
        if (typeArgs.length == 1) {
            PsiType listType = typeArgs[0];
            ParamDataType elementDataType = guessDataType(listType);
            if (elementDataType != ParamDataType.OBJECT) {
                return;
            }
            PsiClass genericClass = null;
            if (listType != null && localGenericMap.containsKey(listType.getPresentableText())) {
                PsiType realType = localGenericMap.get(listType.getPresentableText());
                if (realType instanceof PsiClassType) {
                    genericClass = ((PsiClassType) realType).resolve();
                }
            }
            if (genericClass == null && listType instanceof PsiClassType) {
                genericClass = ((PsiClassType) listType).resolve();
            }
            if (genericClass != null) {
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                if (StrUtil.isNotBlank(param.getRawType())) {
                    param.setRawType(param.getRawType().replace(listType.getPresentableText(), genericClass.getName()));
                }
                param.setDescription(getClassDescription(genericClass));
                param.getChildren().addAll(scanPojoFieldsWithGenerics(genericClass, null, visited, localGenericMap, project));
            }

        }
    }

    /**
     * 处理自定义泛型类字段递归。
     */
    private static void handleCustomType(ApiParam param, PsiClass rawClass, PsiType[] typeArgs, Set<String> visited, PsiType realType, Project project) {
        PsiTypeParameter[] rawTypeParams = rawClass.getTypeParameters();
        // 如果是接口，自动查找同包下实现类
        if (rawClass.isInterface()) {
            PsiFile containingFile = rawClass.getContainingFile();
            if (containingFile != null && containingFile.getParent() != null) {
                PsiDirectory directory = containingFile.getParent();
                for (PsiFile file : directory.getFiles()) {
                    if (file instanceof PsiJavaFile) {
                        PsiClass[] classes = ((PsiJavaFile) file).getClasses();
                        for (PsiClass implClass : classes) {
                            if (implClass.isInheritor(rawClass, true)) {
                                PsiTypeParameter[] implTypeParams = implClass.getTypeParameters();
                                Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
                                for (int j = 0; j < implTypeParams.length && j < typeArgs.length; j++) {
                                    implTypeArgMap.put(implTypeParams[j].getName(), typeArgs[j]);
                                }
                                if (param.getChildren() == null) {
                                    param.setChildren(new ArrayList<>());
                                }
                                param.getChildren().addAll(scanPojoFieldsWithGenerics(implClass, null, visited, implTypeArgMap, project));
                                break;
                            }
                        }
                    }
                }
            }
        } else if (rawTypeParams.length > 0) {
            Map<String, PsiType> typeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
            for (int i = 0; i < rawTypeParams.length && i < typeArgs.length; i++) {
                typeArgMap.put(rawTypeParams[i].getName(), typeArgs[i]);
            }
            if (param.getChildren() == null) {
                param.setChildren(new ArrayList<>());
            }
            param.getChildren().addAll(scanPojoFieldsWithGenerics(rawClass, null, visited, typeArgMap, project));
        } else {
            if (param.getChildren() == null) {
                param.setChildren(new ArrayList<>());
            }
            param.setDescription(getClassDescription(rawClass));
            Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
            implTypeArgMap.put("T", realType);
            param.getChildren().addAll(scanPojoFieldsWithGenerics(rawClass, null, visited, implTypeArgMap, project));
        }
    }

    /**
     * 获取字段注释（优先 Swagger 注解，无则 JavaDoc）。
     */
    public static String getClassDescription(PsiClass psiClass) {
        return getClassDescription(psiClass, null);
    }

    /**
     * 清洗 JavaDoc 注释，仅保留主要描述部分。
     */
    private static String cleanJavaDoc(String rawDoc) {
        if (rawDoc == null) {
            return "";
        }
        StringBuilder cleanDoc = new StringBuilder();
        String[] lines = rawDoc.split("\\r?\\n");

        boolean descriptionFound = false;
        for (String line : lines) {
            String trim = line.trim();
            // 跳过开始/结束标记
            if (trim.startsWith("/**") || trim.startsWith("*/") || trim.equals("*")) {
                continue;
            }
            // 去掉行首 *
            if (trim.startsWith("*")) {
                trim = trim.substring(1).trim();
            }
            // 如果是 @description 则优先保留
            if (trim.toLowerCase().startsWith("@description")) {
                // 清除之前内容，仅保留 description
                cleanDoc.setLength(0);
                cleanDoc.append(trim.substring("@description".length()).trim());
                descriptionFound = true;
                break;
            }
            // 其他 @注解跳过
            if (trim.startsWith("@")) {
                continue;
            }
            // 忽略日期/时间内容（可选）
            if (trim.matches(".*\\d{4}/\\d{2}/\\d{2}.*")) {
                continue;
            }
            // 否则认为是正文描述（仅保留第一条）
            if (!descriptionFound && !trim.isEmpty()) {
                cleanDoc.append(trim);
                descriptionFound = true;
            }
        }
        return cleanDoc.toString().trim();
    }

    /**
     * 类型推断工具方法。
     *
     * @param type PsiType
     * @return ParamDataType
     */
    private static ParamDataType guessDataType(PsiType type) {
        String presentableText = type.getPresentableText();
        String canonicalText = type.getCanonicalText();
        // 基础类型判断
        if (INT_TYPE.equals(presentableText) || INTEGER_TYPE.equals(presentableText) || LONG_TYPE.equals(presentableText) || LONG_CLASS_TYPE.equals(presentableText)
                || SHORT_TYPE.equals(presentableText) || SHORT_CLASS_TYPE.equals(presentableText)) {
            return ParamDataType.INTEGER;
        }
        if (BOOLEAN_TYPE.equals(presentableText) || BOOLEAN_CLASS_TYPE.equals(presentableText)) {
            return ParamDataType.BOOLEAN;
        }
        if (DOUBLE_TYPE.equals(presentableText) || DOUBLE_CLASS_TYPE.equals(presentableText) || FLOAT_TYPE.equals(presentableText) || FLOAT_CLASS_TYPE.equals(presentableText)
                || BIGDECIMAL_TYPE.equals(presentableText) || BIGDECIMAL_FULL_TYPE.equals(canonicalText)) {
            return ParamDataType.NUMBER;
        }
        if (STRING_TYPE.equals(presentableText) || JAVA_STRING_TYPE.equals(canonicalText)) {
            return ParamDataType.STRING;
        }
        // 集合类型
        if (type instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass != null) {
                String qualifiedName = psiClass.getQualifiedName();
                if (LIST_TYPE.equals(qualifiedName) || ARRAYLIST_TYPE.equals(qualifiedName)
                        || COLLECTION_TYPE.equals(qualifiedName)) {
                    return ParamDataType.ARRAY;
                }
                if (FILE_TYPE.equals(qualifiedName) || MULTIPART_FILE_TYPE.equals(qualifiedName)) {
                    return ParamDataType.FILE;
                }
                // 只要不是JDK内置类型，默认按对象处理
                if (qualifiedName != null && !qualifiedName.startsWith(JAVA_PACKAGE_PREFIX)) {
                    return ParamDataType.OBJECT;
                }

                if (psiClass instanceof PsiTypeParameter) {
                    // 是泛型类型参数，如 T、E、K、V 等
                    return ParamDataType.OBJECT;
                }
            }
        }
        return ParamDataType.STRING;
    }
} 