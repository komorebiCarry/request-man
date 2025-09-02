package com.ljh.request.requestman.util;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.PsiPackage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.util.Query;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.DataTypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static List<ApiParam> scanPojoFields(PsiClass psiClass, Project project, ApiParam apiParam) {
        return ReadAction.compute(() ->
                scanPojoFieldsWithGenerics(psiClass, null, new HashSet<>(DEFAULT_MAP_SIZE), new HashMap<>(DEFAULT_MAP_SIZE), project, apiParam)
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
    public static List<ApiParam> scanPojoFieldsWithGenerics(PsiClass psiClass, String classDoc, Set<String> visited, Map<String, PsiType> genericMap, Project project, ApiParam apiParam) {
        return ReadAction.compute(() -> {
            LogUtil.debug("[PojoFieldScanner] 递归类型: " + (psiClass != null ? psiClass.getQualifiedName() : "null") + ", visited: " + visited + ", 泛型映射: " + genericMap);
            List<ApiParam> params = new ArrayList<>();
            if (psiClass == null) {
                return params;
            }
            String classKey = psiClass.getQualifiedName();

            // 分层递归防护机制
            if (classKey != null) {
                // 检查是否已经访问过该类
                if (visited.contains(classKey)) {
                    // 如果已经访问过，检查是否允许递归
                    if (psiClass.isInterface() || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                        // 接口和抽象类允许递归一次
                        LogUtil.debug("[PojoFieldScanner] 允许接口/抽象类递归: " + classKey);
                    } else {
                        // 具体类检查递归深度
                        String recursionKey = classKey + "_recursion";
                        if (visited.contains(recursionKey)) {
                            // 已经递归过一次，防止无限循环
                            LogUtil.debug("[PojoFieldScanner] 阻止无限递归: " + classKey);
                            if (apiParam != null) {
                                apiParam.setRecursive(true);
                            }
                            return params;
                        } else {
                            // 允许递归一次，标记递归状态
                            visited.add(recursionKey);
                            LogUtil.debug("[PojoFieldScanner] 允许具体类递归一次: " + classKey);
                        }
                    }
                } else {
                    // 首次访问，添加到visited集合
                    visited.add(classKey);
                }
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
        ParamDataType dataType = DataTypeUtils.guessDataType(field.getType());
        String rawType = field.getType().getPresentableText();
        ApiParam param = new ApiParam();
        param.setName(name);
        param.setDescription(desc);
        param.setDataType(dataType);
        param.setRawType(rawType);
        // 新增：枚举类型特殊处理
        if (dataType == ParamDataType.ENUM) {
            // 枚举类型不需要递归扫描，但收集枚举值
            PsiClass fieldClass = PsiUtil.resolveClassInType(field.getType());
            if (fieldClass != null && fieldClass.isEnum()) {
                String enumValues = getEnumValues(fieldClass);
                if (!enumValues.isEmpty()) {
                    String newDesc = desc.isEmpty() ? "枚举类型" : desc;
                    param.setDescription(newDesc + " (可选值: " + enumValues + ")");
                }
            }
            return param; // 枚举类型直接返回，不继续递归
        }
        // 泛型参数递归
        if (field.getType() instanceof PsiClassType) {
            String typeName = field.getType().getCanonicalText();
            if (localGenericMap.containsKey(typeName)) {
                PsiType realType = localGenericMap.get(typeName);
                LogUtil.debug("[PojoFieldScanner] 泛型参数递归: " + typeName + " -> " + realType);
                param.setRawType(realType.getPresentableText());
                // 检查realType是否为基础类型，如果是则不需要继续递归扫描
                ParamDataType realDataType = DataTypeUtils.guessDataType(realType);
                param.setDataType(realDataType);
                if (realDataType == ParamDataType.OBJECT || realDataType == ParamDataType.ARRAY) {
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
                if (DataTypeUtils.isCollectionType(rawClassName)) {
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
                param.getChildren().addAll(scanPojoFieldsWithGenerics(fieldClass, classDesc, visited, localGenericMap, project, param));
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
                if (DataTypeUtils.isCollectionType(canonical)) {
                    if (typeArgs.length == 1) {
                        PsiType elementType = typeArgs[0];
                        ParamDataType elementDataType = DataTypeUtils.guessDataType(elementType);
                        param.setDataType(ParamDataType.ARRAY);
                        param.setChildren(new ArrayList<>());
                        if (elementDataType == ParamDataType.OBJECT) {
                            PsiClass elementClass = null;

                            // 处理通配符类型：? extends T 或 ? super T
                            if (elementType instanceof PsiWildcardType wildcardType) {
                                PsiType boundType = wildcardType.getExtendsBound();
                                if (boundType == null) {
                                    boundType = wildcardType.getSuperBound();
                                }
                                if (boundType instanceof PsiClassType) {
                                    elementClass = ((PsiClassType) boundType).resolve();
                                }
                            } else if (elementType instanceof PsiClassType) {
                                elementClass = ((PsiClassType) elementType).resolve();
                            }

                            if (elementClass != null) {
                                param.setDescription(getClassDescription(elementClass));
                                Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
                                implTypeArgMap.put("T", elementType);
                                param.getChildren().addAll(scanPojoFieldsWithGenerics(elementClass, null, visited, implTypeArgMap, project, param));
                            }
                        }
                    }
                } else if (!canonical.startsWith(JAVA_PACKAGE_PREFIX)) {
                    handleCustomType(param, realClass, typeArgs, visited, realType, project);
                } else {
                    if (param.getChildren() == null) {
                        param.setChildren(new ArrayList<>());
                    }
                    param.getChildren().addAll(scanPojoFieldsWithGenerics(realClass, null, visited, localGenericMap, project, param));
                }
            }
        } else if (realType != null) {
            PsiClass globalClass = JavaPsiFacade.getInstance(project).findClass(realType.getCanonicalText(), GlobalSearchScope.allScope(project));
            if (globalClass != null) {
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                param.getChildren().addAll(scanPojoFieldsWithGenerics(globalClass, null, visited, localGenericMap, project, param));
            }
        }
    }

    /**
     * 判断是否为集合类型。
     */


    /**
     * 处理集合类型字段递归。
     */
    private static void handleCollectionType(ApiParam param, PsiType[] typeArgs, Map<String, PsiType> localGenericMap, Set<String> visited, Project project) {
        if (typeArgs.length == 1) {
            PsiType listType = typeArgs[0];
            ParamDataType elementDataType = DataTypeUtils.guessDataType(listType);
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

            // 如果localGenericMap中没有，则尝试直接解析
            if (genericClass == null) {
                if (listType instanceof PsiWildcardType wildcardType) {
                    // 处理通配符类型：? extends T 或 ? super T
                    PsiType boundType = wildcardType.getExtendsBound();
                    if (boundType == null) {
                        boundType = wildcardType.getSuperBound();
                    }
                    if (boundType instanceof PsiClassType) {
                        genericClass = ((PsiClassType) boundType).resolve();
                    }
                } else if (listType instanceof PsiClassType) {
                    genericClass = ((PsiClassType) listType).resolve();
                }
            }

            if (genericClass != null) {
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                if (StrUtil.isNotBlank(param.getRawType())) {
                    param.setRawType(param.getRawType().replace(listType.getPresentableText(), genericClass.getName()));
                }
                param.setDescription(getClassDescription(genericClass));
                param.getChildren().addAll(scanPojoFieldsWithGenerics(genericClass, null, visited, localGenericMap, project, param));
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
            List<ApiParam> apiParams = IMPLEMENTATION_CACHE.get(rawClass.getQualifiedName());
            if (apiParams != null) {
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                param.getChildren().addAll(apiParams);
                return;
            }
            PsiClass implClass = findBestImplementation(rawClass, project);
            if (implClass != null) {
                PsiTypeParameter[] implTypeParams = implClass.getTypeParameters();
                Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
                for (int j = 0; j < implTypeParams.length && j < typeArgs.length; j++) {
                    PsiType typeArg = typeArgs[j];
                    if (typeArg instanceof PsiWildcardType wildcardType) {
                        PsiType boundType = wildcardType.getExtendsBound();
                        if (boundType == null) {
                            boundType = wildcardType.getSuperBound();
                        }
                        if (boundType != null) {
                            implTypeArgMap.put(implTypeParams[j].getName(), boundType);
                        } else {
                            implTypeArgMap.put(implTypeParams[j].getName(), typeArg);
                        }
                    } else {
                        implTypeArgMap.put(implTypeParams[j].getName(), typeArg);
                    }
                }
                if (param.getChildren() == null) {
                    param.setChildren(new ArrayList<>());
                }
                apiParams = scanPojoFieldsWithGenerics(implClass, null, visited, implTypeArgMap, project, param);
                param.getChildren().addAll(apiParams);
                // 缓存
                IMPLEMENTATION_CACHE.put(rawClass.getQualifiedName(), apiParams);
            }
        } else if (rawTypeParams.length > 0) {
            Map<String, PsiType> typeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
            for (int i = 0; i < rawTypeParams.length && i < typeArgs.length; i++) {
                // 处理通配符类型：? extends T 或 ? super T
                PsiType typeArg = typeArgs[i];
                if (typeArg instanceof PsiWildcardType wildcardType) {
                    PsiType boundType = wildcardType.getExtendsBound();
                    if (boundType == null) {
                        boundType = wildcardType.getSuperBound();
                    }
                    if (boundType != null) {
                        typeArgMap.put(rawTypeParams[i].getName(), boundType);
                    } else {
                        typeArgMap.put(rawTypeParams[i].getName(), typeArg);
                    }
                } else {
                    typeArgMap.put(rawTypeParams[i].getName(), typeArg);
                }
            }
            if (param.getChildren() == null) {
                param.setChildren(new ArrayList<>());
            }
            param.getChildren().addAll(scanPojoFieldsWithGenerics(rawClass, null, visited, typeArgMap, project, param));
        } else {
            if (param.getChildren() == null) {
                param.setChildren(new ArrayList<>());
            }
            param.setDescription(getClassDescription(rawClass));
            Map<String, PsiType> implTypeArgMap = new HashMap<>(DEFAULT_MAP_SIZE);
            implTypeArgMap.put("T", realType);
            param.getChildren().addAll(scanPojoFieldsWithGenerics(rawClass, null, visited, implTypeArgMap, project, param));
        }
    }

    /**
     * 选择接口的最佳实现类，搜索顺序：同包 → 同模块 → 全项目。仅返回一个最优候选。
     */
    private static PsiClass findBestImplementation(PsiClass interfaceClass, Project project) {
        if (interfaceClass == null || !interfaceClass.isInterface()) {
            return null;
        }

        String interfaceFqn = interfaceClass.getQualifiedName();
        if (interfaceFqn == null) {
            return null;
        }

        String pkgName = getPackageName(interfaceClass);
        PsiPackage psiPackage = pkgName != null ? JavaPsiFacade.getInstance(project).findPackage(pkgName) : null;
        Module module = ModuleUtilCore.findModuleForPsiElement(interfaceClass);

        PsiClass impl = null;
        // 1) 同包（含子包）
        if (psiPackage != null) {
            // 创建包级别的搜索范围
            PsiFile[] packageFiles = psiPackage.getFiles(GlobalSearchScope.allScope(project));
            if (packageFiles.length > 0) {
                // 将 PsiFile[] 转换为 VirtualFile 集合
                java.util.Collection<com.intellij.openapi.vfs.VirtualFile> virtualFiles = new java.util.ArrayList<>();
                for (PsiFile psiFile : packageFiles) {
                    if (psiFile.getVirtualFile() != null) {
                        virtualFiles.add(psiFile.getVirtualFile());
                    }
                }
                if (!virtualFiles.isEmpty()) {
                    GlobalSearchScope packageScope = GlobalSearchScope.filesScope(project, virtualFiles);
                    impl = pickImplementationInScope(interfaceClass, packageScope, pkgName);
                }
            }
        }
        // 2) 同模块
        if (impl == null && module != null) {
            impl = pickImplementationInScope(interfaceClass, GlobalSearchScope.moduleWithDependenciesScope(module), pkgName);
        }
        // 3) 全项目
        if (impl == null) {
            impl = pickImplementationInScope(interfaceClass, GlobalSearchScope.projectScope(project), pkgName);
        }

        return impl;
    }

    private static final Map<String, List<ApiParam>> IMPLEMENTATION_CACHE = new HashMap<>(DEFAULT_MAP_SIZE);


    /**
     * 在给定Scope内挑选一个最优实现类：非抽象、非接口、非枚举；同包优先；名称以Impl/Default结尾优先。
     */
    private static PsiClass pickImplementationInScope(PsiClass interfaceClass, GlobalSearchScope scope, String pkgName) {
        Query<PsiClass> query = ClassInheritorsSearch.search(interfaceClass, scope, true);
        PsiClass best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PsiClass candidate : query.findAll()) {
            if (candidate.isInterface() || candidate.hasModifierProperty(PsiModifier.ABSTRACT) || candidate.isEnum()) {
                continue;
            }
            int score = 0;
            String qn = candidate.getQualifiedName();
            String name = candidate.getName();
            if (qn != null && pkgName != null && qn.startsWith(pkgName + ".")) {
                score += 2;
            }
            if (name != null && (name.endsWith("Impl") || name.endsWith("Default"))) {
                score += 3;
            }
            // 非抽象可实例化加权
            score += 1;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
                // 最高优先级快速返回
                if (score >= 6) {
                    break;
                }
            }
        }
        return best;
    }

    private static String getPackageName(PsiClass clazz) {
        if (clazz == null) {
            return null;
        }
        PsiFile file = clazz.getContainingFile();
        if (file instanceof PsiJavaFile javaFile) {
            String pn = javaFile.getPackageName();
            if (pn != null && !pn.isEmpty()) {
                return pn;
            }
        }
        String fqn = clazz.getQualifiedName();
        if (fqn != null) {
            int idx = fqn.lastIndexOf('.');
            if (idx > 0) {
                return fqn.substring(0, idx);
            }
        }
        return null;
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
     * 获取枚举类的可选值（最多展示8个）
     */
    private static String getEnumValues(PsiClass enumClass) {
        if (enumClass == null || !enumClass.isEnum()) {
            return "";
        }
        StringBuilder values = new StringBuilder();
        int count = 0;
        for (PsiField field : enumClass.getAllFields()) {
            if (field instanceof PsiEnumConstant) {
                if (count > 0) {
                    values.append(", ");
                }
                values.append(field.getName());
                count++;
                if (count >= 8) {
                    values.append("...");
                    break;
                }
            }
        }
        return values.toString();
    }

    /**
     * 清理接口实现缓存
     */
    public static void clearImplementationCache() {
        IMPLEMENTATION_CACHE.clear();
        LogUtil.debug("[PojoFieldScanner] 接口实现缓存已清理");
    }
}