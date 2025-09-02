package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.ljh.request.requestman.constant.SpringAnnotationConstants;
import com.ljh.request.requestman.enums.HttpMethod;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.DataTypeUtils;
import com.ljh.request.requestman.util.RequestManBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leijianhui
 * @Description API信息提取工具类，支持从PsiClass中提取接口信息。
 * @date 2025/06/17 17:12
 */
public class ApiInfoExtractor {
    /**
     * 默认 Map 容量
     */
    private static final int DEFAULT_MAP_SIZE = 8;
    private static final String SLASH = "/";
    private static final String REGEX_PATH_VAR = "\\{([^/}]+)}";
    private static final String STRING_TYPE = "string";
    private static final String INT_TYPE = "int";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String DOUBLE_TYPE = "double";
    private static final String FLOAT_TYPE = "float";
    private static final String BIGDECIMAL_TYPE = "bigdecimal";
    private static final String ARRAY_TYPE = "array";
    private static final String JAVA_DOC_LINE_BREAK = "\\r?\\n";
    private static final String DOC_COMMENT_START = "/**";
    private static final String DOC_COMMENT_END = "*/";
    private static final String AT = "@";

    // 类型判断相关常量
    private static final String INTEGER_TYPE = "Integer";
    private static final String LONG_TYPE = "long";
    private static final String LONG_CLASS_TYPE = "Long";
    private static final String SHORT_TYPE = "short";
    private static final String SHORT_CLASS_TYPE = "Short";
    private static final String BOOLEAN_CLASS_TYPE = "Boolean";
    private static final String DOUBLE_CLASS_TYPE = "Double";
    private static final String FLOAT_CLASS_TYPE = "Float";
    private static final String BIGDECIMAL_FULL_TYPE = "java.math.BigDecimal";
    private static final String JAVA_STRING_TYPE = "java.lang.String";
    private static final String LIST_TYPE = "java.util.List";
    private static final String ARRAYLIST_TYPE = "java.util.ArrayList";
    private static final String COLLECTION_TYPE = "java.util.Collection";
    private static final String FILE_TYPE = "java.io.File";
    private static final String MULTIPART_FILE_TYPE = "org.springframework.web.multipart.MultipartFile";

    /**
     * 提取当前编辑文件中的所有接口信息。
     *
     * @param project 当前项目对象
     * @return 接口信息列表
     */
    public static List<ApiInfo> extractApiInfos(Project project) {
        return ReadAction.compute(() -> {
            List<ApiInfo> apis = new ArrayList<>();
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            if (editorManager.getSelectedTextEditor() == null) {
                return apis;
            }
            PsiJavaFile psiFile = (PsiJavaFile) PsiDocumentManager.getInstance(project)
                    .getPsiFile(editorManager.getSelectedTextEditor().getDocument());
            if (psiFile == null) {
                return apis;
            }
            for (PsiClass clazz : psiFile.getClasses()) {
                // 仅处理Controller类
                boolean isController = false;
                if (clazz.getModifierList() != null) {
                    for (PsiAnnotation ann : clazz.getModifierList().getAnnotations()) {
                        String qn = ann.getQualifiedName();
                        if (qn != null && (qn.endsWith("RestController") || qn.endsWith("Controller"))) {
                            isController = true;
                            break;
                        }
                    }
                }
                if (!isController) {
                    continue;
                }
                for (PsiMethod method : clazz.getMethods()) {
                    // 过滤掉构造方法
                    if (method.isConstructor()) {
                        continue;
                    }
                    // 仅处理带 Mapping 的方法
                    boolean hasMapping = false;
                    if (method.getModifierList() != null) {
                        for (PsiAnnotation ann : method.getModifierList().getAnnotations()) {
                            String qn = ann.getQualifiedName();
                            if (qn == null) {
                                continue;
                            }
                            if (qn.equals("org.springframework.web.bind.annotation.RequestMapping")
                                    || qn.equals("org.springframework.web.bind.annotation.GetMapping")
                                    || qn.equals("org.springframework.web.bind.annotation.PostMapping")
                                    || qn.equals("org.springframework.web.bind.annotation.PutMapping")
                                    || qn.equals("org.springframework.web.bind.annotation.DeleteMapping")
                                    || qn.equals("org.springframework.web.bind.annotation.PatchMapping")
                                    || qn.equals("RequestMapping") || qn.equals("GetMapping")
                                    || qn.equals("PostMapping") || qn.equals("PutMapping")
                                    || qn.equals("DeleteMapping") || qn.equals("PatchMapping")) {
                                hasMapping = true;
                                break;
                            }
                        }
                    }
                    if (!hasMapping) {
                        continue;
                    }
                    apis.add(extractApiInfoFromMethod(method, true));
                }
            }
            return apis;
        });
    }

    /**
     * 从PsiMethod中提取单个接口信息，优先使用Swagger注解，否则清洗JavaDoc注释。
     *
     * @param method 方法对象
     * @return ApiInfo
     */
    public static ApiInfo extractApiInfoFromMethod(PsiMethod method, boolean isScanResult) {
        return ReadAction.compute(() -> {
            AnnotationParseResult annotationResult = parseMethodAnnotations(method);
            String basePath = extractBasePath(method.getContainingClass());
            String url = buildFullUrl(basePath, annotationResult.url);
            List<ApiParam> bodyParams = extractBodyParams(method, isScanResult);
            List<ApiParam> params = extractRequestParams(method);
            String description = annotationResult.hasSwagger ? annotationResult.description : extractJavaDocDescription(method);
            List<ApiParam> responseParams = new ArrayList<>();
            PsiType returnType = method.getReturnType();
            if (returnType instanceof PsiClassType classType) {
                PsiClass respClass = classType.resolve();
                if (respClass != null) {
                    PsiTypeParameter[] typeParams = respClass.getTypeParameters();
                    PsiType[] actualTypes = classType.getParameters();
                    Map<String, PsiType> genericMap = new HashMap<>(DEFAULT_MAP_SIZE);
                    for (int i = 0; i < typeParams.length && i < actualTypes.length; i++) {
                        genericMap.put(typeParams[i].getName(), actualTypes[i]);
                    }
                    Project project = respClass.getProject();
                    if (isScanResult) {
                        responseParams = PojoFieldScanner.scanPojoFieldsWithGenerics(respClass, null, new HashSet<>(), genericMap, project, null);
                    }
                }
            }

            return new ApiInfo(annotationResult.name, method.getName(), url, annotationResult.httpMethod, params, bodyParams, getParamTypes(method), description, responseParams, method.getContainingClass() != null ? method.getContainingClass().getQualifiedName() : "");
        });
    }

    /**
     * 解析方法上的注解，获取接口名、url、httpMethod、描述、是否有swagger注解。
     */
    private static AnnotationParseResult parseMethodAnnotations(PsiMethod method) {
        String name = method.getName();
        String url = "";
        String httpMethod = HttpMethod.UNKNOWN.name();
        String description = "";
        boolean hasSwagger = false;
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            if (qualifiedName.endsWith("Mapping")) {
                boolean isMapping = false;
                if (qualifiedName.contains(SpringAnnotationConstants.GET_MAPPING)) {
                    httpMethod = "GET";
                    isMapping = true;
                } else if (qualifiedName.contains(SpringAnnotationConstants.POST_MAPPING)) {
                    httpMethod = "POST";
                    isMapping = true;
                } else if (qualifiedName.contains(SpringAnnotationConstants.PUT_MAPPING)) {
                    httpMethod = "PUT";
                    isMapping = true;
                } else if (qualifiedName.contains(SpringAnnotationConstants.DELETE_MAPPING)) {
                    httpMethod = "DELETE";
                    isMapping = true;
                }
                if (isMapping) {
                    var value = annotation.findAttributeValue("value");
                    if (value != null) {
                        url = extractPathFromValue(value);
                    }
                }
            }
            if (qualifiedName.endsWith(SpringAnnotationConstants.API_OPERATION)) {
                // Swagger2: @ApiOperation(value = "xxx", notes = "yyy")
                var value = annotation.findAttributeValue("value");
                if (value != null) {
                    name = value.getText().replaceAll("[\"{}]", "");
                }
                var notes = annotation.findAttributeValue("notes");
                if (notes != null) {
                    description = notes.getText().replaceAll("[\"{}]", "");
                }
                hasSwagger = true;
            } else if (qualifiedName.endsWith(SpringAnnotationConstants.OPERATION)) {
                // OpenAPI3: @Operation(summary = "xxx", description = "yyy")
                var summary = annotation.findAttributeValue("summary");
                if (summary != null) {
                    name = summary.getText().replaceAll("[\"{}]", "");
                }
                var desc = annotation.findAttributeValue("description");
                if (desc != null) {
                    description = desc.getText().replaceAll("[\"{}]", "");
                }
                hasSwagger = true;
            }
        }
        return new AnnotationParseResult(name, url, httpMethod, description, hasSwagger);
    }

    /**
     * 从注解的value属性中提取路径，支持数组形式如 @RequestMapping({"/sys/webui"})
     *
     * @param value 注解的value属性
     * @return 提取的路径
     */
    private static String extractPathFromValue(PsiAnnotationMemberValue value) {
        if (value == null) {
            return "";
        }

        String text = value.getText();
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 处理数组形式：@RequestMapping({"/sys/webui"}) 或 @RequestMapping({"/sys/webui", "/api/webui"})
        if (text.startsWith("{") && text.endsWith("}")) {
            // 移除外层的大括号
            String arrayContent = text.substring(1, text.length() - 1);
            // 分割多个路径（如果有的话）
            String[] paths = arrayContent.split(",");
            if (paths.length > 0) {
                // 取第一个路径，移除引号
                String firstPath = paths[0].trim().replaceAll("[\"']", "");
                return firstPath;
            }
        }

        // 处理单个路径：@RequestMapping("/sys/webui")
        return text.replaceAll("[\"']", "");
    }

    /**
     * 获取controller类上的@RequestMapping等注解的基础路径。
     */
    private static String extractBasePath(PsiClass clazz) {
        if (clazz == null) {
            return "";
        }
        for (PsiAnnotation ann : clazz.getAnnotations()) {
            String qualifiedName = ann.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            if (qualifiedName.endsWith("RequestMapping") || qualifiedName.endsWith("GetMapping") || qualifiedName.endsWith("PostMapping") ||
                    qualifiedName.endsWith("PutMapping") || qualifiedName.endsWith("DeleteMapping") || qualifiedName.endsWith("PatchMapping")) {
                PsiAnnotationMemberValue value = ann.findAttributeValue("value");
                if (value != null) {
                    return extractPathFromValue(value);
                }
            }
        }
        return "";
    }

    /**
     * 拼接基础路径和方法url，处理斜杠。
     */
    private static String buildFullUrl(String basePath, String url) {
        if (basePath == null || basePath.isEmpty()) {
            return url;
        }
        String bp = basePath;
        String u = url;
        if (!bp.startsWith(SLASH)) {
            bp = SLASH + bp;
        }
        if (u.startsWith(SLASH)) {
            u = u.substring(1);
        }
        return bp + (u.isEmpty() ? "" : (bp.endsWith(SLASH) ? "" : SLASH) + u);
    }


    /**
     * 提取方法参数中的@RequestBody参数。
     */
    public static List<ApiParam> extractBodyParams(PsiMethod method, boolean isScanResult) {
        return ReadAction.compute(() -> {
            List<ApiParam> bodyParams = new ArrayList<>();
            String httpMethod = extractHttpMethod(method);
            String consumesType = extractConsumesFromMethod(method);

            for (var param : method.getParameterList().getParameters()) {
                boolean isRequestBody = false;
                boolean isRequestParam = false;
                boolean isPathVariable = false;
                String paramConsumesType = null;

                for (PsiAnnotation ann : param.getAnnotations()) {
                    String qualifiedName = ann.getQualifiedName();
                    if (qualifiedName == null) {
                        continue;
                    }

                    if (qualifiedName.endsWith("RequestBody")) {
                        isRequestBody = true;
                        // 提取consumes属性
                        paramConsumesType = extractConsumesFromRequestBody(ann);
                        break;
                    } else if (qualifiedName.endsWith("RequestParam")) {
                        isRequestParam = true;
                    } else if (qualifiedName.endsWith("PathVariable")) {
                        isPathVariable = true;
                    }
                }

                // 判断是否应该归类为body参数
                boolean shouldBeBodyParam = isRequestBody || shouldRequestParamBeBody(isPathVariable, httpMethod, consumesType, paramConsumesType, param.getType());

                if (shouldBeBodyParam) {
                    ParamDataType dataType = DataTypeUtils.guessDataType(param);
                    PsiType paramType = param.getType();

                    // 确定contentType：优先使用参数的consumes，如果没有则使用方法的consumes
                    String contentType = paramConsumesType != null ? paramConsumesType : consumesType;

                    // 如果都没有指定，且是非GET请求的Body参数，根据数据类型设置默认表单类型
                    if (!isRequestBody && contentType == null && !"GET".equalsIgnoreCase(httpMethod)) {
                        if (ParamDataType.FILE.equals(dataType)) {
                            contentType = "multipart/form-data";
                        } else {
                            contentType = "application/x-www-form-urlencoded";
                        }
                    }

                    // 处理数组类型（如byte[]）
                    if (paramType instanceof PsiArrayType arrayType) {
                        PsiType componentType = arrayType.getComponentType();
                        String componentTypeName = componentType.getCanonicalText();
                        ApiParam apiParam = new ApiParam(param.getName(), RequestManBundle.message("param.location.body"), "", dataType, paramType.getCanonicalText(), contentType);
                        apiParam.setRawType(componentTypeName + "[]");
                        bodyParams.add(apiParam);
                        continue;
                    }

                    // 处理类类型
                    if (paramType instanceof PsiClassType classType) {
                        PsiClass psiClass = classType.resolve();

                        ApiParam apiParam = new ApiParam(param.getName(), RequestManBundle.message("param.location.body"), "", dataType, paramType.getCanonicalText(), contentType);
                        if (psiClass != null) {
                            String classDesc = PojoFieldScanner.getClassDescription(psiClass);
                            if (!classDesc.isEmpty()) {
                                apiParam.setDescription(classDesc);
                            }
                            apiParam.setRawType(psiClass.getName());

                            if (isScanResult) {
                                Project project = psiClass.getProject();
                                if (ParamDataType.ARRAY.equals(dataType)) {
                                    PsiType parameter = classType.getParameters()[0];
                                    Map<String, PsiType> genericMap = new HashMap<>(DEFAULT_MAP_SIZE);
                                    genericMap.put("T", parameter);
                                    apiParam.setRawType(classType.getPresentableText());
                                    PojoFieldScanner.handleGenericType(apiParam, classType, new HashSet<>(), genericMap, project);
                                } else {
                                    apiParam.setChildren(PojoFieldScanner.scanPojoFields(psiClass, method.getProject(), apiParam));
                                }
                            }
                        }
                        bodyParams.add(apiParam);
                    } else {
                        // 处理其他类型（如基本类型）
                        ApiParam apiParam = new ApiParam(param.getName(), RequestManBundle.message("param.location.body"), "", dataType, paramType.getCanonicalText(), contentType);
                        apiParam.setRawType(paramType.getPresentableText());
                        bodyParams.add(apiParam);
                    }
                }
            }
            return bodyParams;
        });
    }

    /**
     * 从@RequestBody注解中提取consumes属性值
     *
     * @param requestBodyAnnotation @RequestBody注解
     * @return consumes类型，如果未指定则返回null
     */
    private static String extractConsumesFromRequestBody(PsiAnnotation requestBodyAnnotation) {
        if (requestBodyAnnotation == null) {
            return null;
        }

        // 尝试提取consumes属性
        PsiAnnotationMemberValue consumesValue = requestBodyAnnotation.findAttributeValue("consumes");
        if (consumesValue != null) {
            return extractContentTypeFromValue(consumesValue);
        }

        return null;
    }

    /**
     * 从方法注解中提取consumes属性值
     *
     * @param method 方法对象
     * @return consumes类型，如果未指定则返回null
     */
    public static String extractConsumesFromMethod(PsiMethod method) {
        if (method == null) {
            return null;
        }

        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            // 检查是否是Spring的Mapping注解
            if (qualifiedName.endsWith("Mapping")) {
                PsiAnnotationMemberValue consumesValue = annotation.findAttributeValue("consumes");
                if (consumesValue != null) {
                    return extractContentTypeFromValue(consumesValue);
                }
            }
        }

        return null;
    }

    /**
     * 从注解值中提取Content-Type，支持字符串字面量和常量引用
     *
     * @param value 注解值
     * @return Content-Type字符串，如果无法解析则返回null
     */
    private static String extractContentTypeFromValue(PsiAnnotationMemberValue value) {
        if (value == null) {
            return null;
        }

        // 处理字符串字面量
        if (value instanceof PsiLiteralExpression literal) {
            Object literalValue = literal.getValue();
            if (literalValue instanceof String) {
                return (String) literalValue;
            }
        }

        // 处理数组形式，如 {"application/json", "application/xml"}
        if (value instanceof PsiArrayInitializerExpression arrayExpr) {
            PsiExpression[] initializers = arrayExpr.getInitializers();
            if (initializers.length > 0) {
                String firstType = extractContentTypeFromValue(initializers[0]);
                if (firstType != null) {
                    return firstType;
                }
            }
        }

        // 处理常量引用，如 MediaType.MULTIPART_FORM_DATA_VALUE
        if (value instanceof PsiReferenceExpression refExpr) {
            return resolveMediaTypeConstant(refExpr);
        }

        // 处理其他可能的表达式
        String text = value.getText();
        if (text != null && !text.trim().isEmpty()) {
            // 尝试从文本中提取字符串
            return extractStringFromText(text);
        }

        return null;
    }

    /**
     * 解析MediaType常量引用
     *
     * @param expr 引用表达式
     * @return MediaType值，如果无法解析则返回null
     */
    private static String resolveMediaTypeConstant(PsiExpression expr) {
        if (expr == null) {
            return null;
        }

        String text = expr.getText();
        if (text == null) {
            return null;
        }

        // 常见的Spring MediaType常量映射
        switch (text) {
            case "MediaType.MULTIPART_FORM_DATA_VALUE":
            case "org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE":
                return "multipart/form-data";

            case "MediaType.APPLICATION_FORM_URLENCODED_VALUE":
            case "org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE":
                return "application/x-www-form-urlencoded";

            case "MediaType.APPLICATION_JSON_VALUE":
            case "org.springframework.http.MediaType.APPLICATION_JSON_VALUE":
                return "application/json";

            case "MediaType.APPLICATION_XML_VALUE":
            case "org.springframework.http.MediaType.APPLICATION_XML_VALUE":
                return "application/xml";

            case "MediaType.TEXT_PLAIN_VALUE":
            case "org.springframework.http.MediaType.TEXT_PLAIN_VALUE":
                return "text/plain";

            case "MediaType.TEXT_HTML_VALUE":
            case "org.springframework.http.MediaType.TEXT_HTML_VALUE":
                return "text/html";

            case "MediaType.TEXT_XML_VALUE":
            case "org.springframework.http.MediaType.TEXT_XML_VALUE":
                return "text/xml";

            case "MediaType.APPLICATION_OCTET_STREAM_VALUE":
            case "org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE":
                return "application/octet-stream";

            case "MediaType.APPLICATION_PDF_VALUE":
            case "org.springframework.http.MediaType.APPLICATION_PDF_VALUE":
                return "application/pdf";

            case "MediaType.IMAGE_PNG_VALUE":
            case "org.springframework.http.MediaType.IMAGE_PNG_VALUE":
                return "image/png";

            case "MediaType.IMAGE_JPEG_VALUE":
            case "org.springframework.http.MediaType.IMAGE_JPEG_VALUE":
                return "image/jpeg";

            case "MediaType.IMAGE_GIF_VALUE":
            case "org.springframework.http.MediaType.IMAGE_GIF_VALUE":
                return "image/gif";

            default:
                // 尝试解析其他可能的常量
                return resolveCustomMediaTypeConstant(expr);
        }
    }

    /**
     * 解析自定义MediaType常量
     *
     * @param expr 引用表达式
     * @return MediaType值，如果无法解析则返回null
     */
    private static String resolveCustomMediaTypeConstant(PsiExpression expr) {
        if (expr == null) {
            return null;
        }

        // 尝试解析引用的实际值
        if (expr instanceof PsiReferenceExpression refExpr) {
            PsiElement resolved = refExpr.resolve();
            if (resolved instanceof PsiField field) {
                // 检查是否是常量字段
                if (field.hasModifierProperty(PsiModifier.STATIC) &&
                        field.hasModifierProperty(PsiModifier.FINAL)) {

                    PsiExpression initializer = field.getInitializer();
                    if (initializer instanceof PsiLiteralExpression literal) {
                        Object value = literal.getValue();
                        if (value instanceof String) {
                            return (String) value;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 从文本中提取字符串值
     *
     * @param text 文本内容
     * @return 提取的字符串，如果无法提取则返回null
     */
    private static String extractStringFromText(String text) {
        if (text == null) {
            return null;
        }

        // 移除引号
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
            return text.substring(1, text.length() - 1);
        }

        if (text.startsWith("'") && text.endsWith("'") && text.length() > 2) {
            return text.substring(1, text.length() - 1);
        }

        return null;
    }

    /**
     * 提取方法参数中的普通请求参数（非@RequestBody、非路径变量）。
     */
    public static List<ApiParam> extractRequestParams(PsiMethod method) {
        return ReadAction.compute(() -> {
            List<ApiParam> params = new ArrayList<>();
            String httpMethod = extractHttpMethod(method);
            String consumesType = extractConsumesFromMethod(method);

            for (var param : method.getParameterList().getParameters()) {
                boolean isRequestBody = false;
                boolean isRequestParam = false;
                boolean isPathVariable = false;

                for (PsiAnnotation ann : param.getAnnotations()) {
                    String qualifiedName = ann.getQualifiedName();
                    if (qualifiedName == null) {
                        continue;
                    }

                    if (qualifiedName.endsWith("RequestBody")) {
                        isRequestBody = true;
                        break;
                    } else if (qualifiedName.endsWith("RequestParam")) {
                        isRequestParam = true;
                    } else if (qualifiedName.endsWith("PathVariable")) {
                        isPathVariable = true;
                    }
                }

                // 如果参数应该归类为body参数，则跳过
                if (isRequestBody || shouldRequestParamBeBody(isPathVariable, httpMethod, consumesType, null, param.getType())) {
                    continue;
                }

                ParamDataType dataType = DataTypeUtils.guessDataType(param);
                String rawType = param.getType().getCanonicalText();

                // 路径变量的参数
                if (isPathVariable) {
                    params.add(new ApiParam(param.getName(), "Path", "", dataType, rawType, null));
                } else if (isRequestParam) {
                    // 只有GET请求或没有明确Content-Type的@RequestParam才作为URL参数
                    params.add(new ApiParam(param.getName(), "Query", "", dataType, rawType, null));
                }
            }
            return params;
        });
    }

    /**
     * 判断@RequestParam参数是否应该归类为body参数
     *
     * @param httpMethod     HTTP方法
     * @param methodConsumes 方法的consumes类型
     * @param paramConsumes  参数的consumes类型
     * @return 是否应该归类为body参数
     */
    private static boolean shouldRequestParamBeBody(boolean isPathVariable, String httpMethod, String methodConsumes, String paramConsumes, PsiType paramType) {
        if (isPathVariable) {
            return false;
        }
        // 如果是GET请求，@RequestParam通常是URL参数
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return false;
        }

        // 检查Content-Type
        String contentType = paramConsumes != null ? paramConsumes : methodConsumes;
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            // multipart/form-data 和 x-www-form-urlencoded 中的@RequestParam都是body参数
            if (lowerContentType.contains("multipart/form-data") ||
                    lowerContentType.contains("application/x-www-form-urlencoded")) {
                return true;
            }
        }

        // 关键：检查参数类型
        if (isBasicType(paramType)) {
            // 基础类型 → 应该是 URL 参数，不是 Body 参数
            return false;
        }

        // 实体类型 → 应该是 Body 参数
        return true;
    }

    /**
     * 判断是否为基础类型
     */
    private static boolean isBasicType(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            return true;
        }

        String canonicalText = type.getCanonicalText();
        return "java.lang.String".equals(canonicalText) ||
                "java.lang.Integer".equals(canonicalText) ||
                "java.lang.Long".equals(canonicalText) ||
                "java.lang.Double".equals(canonicalText) ||
                "java.lang.Boolean".equals(canonicalText) ||
                "java.lang.Float".equals(canonicalText) ||
                "java.lang.Short".equals(canonicalText) ||
                "java.lang.Byte".equals(canonicalText) ||
                "java.lang.Character".equals(canonicalText);
    }

    /**
     * 从方法中提取HTTP方法
     *
     * @param method 方法对象
     * @return HTTP方法
     */
    private static String extractHttpMethod(PsiMethod method) {
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith("GetMapping")) {
                return "GET";
            } else if (qualifiedName.endsWith("PostMapping")) {
                return "POST";
            } else if (qualifiedName.endsWith("PutMapping")) {
                return "PUT";
            } else if (qualifiedName.endsWith("DeleteMapping")) {
                return "DELETE";
            } else if (qualifiedName.endsWith("PatchMapping")) {
                return "PATCH";
            } else if (qualifiedName.endsWith("RequestMapping")) {
                // 对于@RequestMapping，需要检查method属性
                PsiAnnotationMemberValue methodValue = annotation.findAttributeValue("method");
                if (methodValue != null) {
                    String methodText = methodValue.getText().toLowerCase();
                    if (methodText.contains("get")) return "GET";
                    if (methodText.contains("post")) return "POST";
                    if (methodText.contains("put")) return "PUT";
                    if (methodText.contains("delete")) return "DELETE";
                    if (methodText.contains("patch")) return "PATCH";
                }
                // 如果没有指定method，默认为GET
                return "GET";
            }
        }

        // 默认返回GET
        return "GET";
    }

    /**
     * 提取JavaDoc注释内容，只保留主要描述部分，去除注释符号和标签。
     */
    private static String extractJavaDocDescription(PsiMethod method) {
        if (method.getDocComment() == null || method.getDocComment().getText().isEmpty()) {
            return "";
        }
        String rawDoc = method.getDocComment().getText();
        StringBuilder cleanDoc = new StringBuilder();
        String[] lines = rawDoc.split(JAVA_DOC_LINE_BREAK);
        for (String line : lines) {
            String trim = line.trim();
            if (trim.startsWith(DOC_COMMENT_START) || trim.startsWith(DOC_COMMENT_END)) {
                continue;
            }
            if (trim.startsWith("*")) {
                trim = trim.substring(1).trim();
            }
            if (trim.startsWith(AT)) {
                break; // 只取主要描述部分
            }
            if (!trim.isEmpty()) {
                cleanDoc.append(trim).append(" ");
            }
        }
        return cleanDoc.toString().trim();
    }

    /**
     * 注解解析结果结构体。
     */
    private static class AnnotationParseResult {
        String name;
        String url;
        String httpMethod;
        String description;
        boolean hasSwagger;

        AnnotationParseResult(String name, String url, String httpMethod, String description, boolean hasSwagger) {
            this.name = name;
            this.url = url;
            this.httpMethod = httpMethod;
            this.description = description;
            this.hasSwagger = hasSwagger;
        }
    }

    /**
     * 获取ParamTypes
     *
     * @param method
     * @return
     */
    public static List<String> getParamTypes(PsiMethod method) {
        return ReadAction.compute(() -> {
            if (method == null || method.getParameterList().isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(method.getParameterList().getParameters())
                    .map(parameter -> parameter.getType().getCanonicalText())
                    .collect(Collectors.toList());
        });
    }

    /**
     * 获取bodyParamList json
     *
     * @param bodyParamList
     * @return
     */
    public static String getApiInfoBodyJson(List<ApiParam> bodyParamList) {
        if (bodyParamList == null || bodyParamList.isEmpty()) {
            return "";
        }
        StringBuilder json = new StringBuilder();
        if (bodyParamList.size() == 1 && bodyParamList.get(0).getChildren() != null && !bodyParamList.get(0).getChildren().isEmpty()) {
            ApiParam apiParam = bodyParamList.get(0);
            boolean isArray = false;
            int index = 0;
            if (ParamDataType.ARRAY.equals(apiParam.getDataType())) {
                isArray = true;
                json.append("[");
                json.append("\n");
                index = 2;
            }
            json.append(JsonExampleGenerator.genJsonWithComment(bodyParamList.get(0).getChildren(), index));
            if (isArray) {
                json.append("\n");
                json.append("]");
            }
        } else {
            json.append(JsonExampleGenerator.genJsonWithComment(bodyParamList, 0));
        }
        return json.toString();
    }


    /**
     * 使用Hutool标准格式化JSON字符串，格式化后去除冒号和逗号后的所有空格，生成极致紧凑的json
     */
    public static String formatJson(String json) {
        try {
            // 去除所有换行和多余空白
            String compact = json.replaceAll("\\s*\\n\\s*", "");
            // 去除冒号和逗号后的所有空格
            compact = compact.replaceAll(":\\s+", ":").replaceAll(",\\s+", ",");
            return JSONUtil.formatJsonStr(compact);
        } catch (Exception e) {
            return json;
        }
    }
} 