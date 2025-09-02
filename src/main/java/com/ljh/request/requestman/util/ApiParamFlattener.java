package com.ljh.request.requestman.util;

import com.ljh.request.requestman.model.ApiParam;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leijianhui
 * @Description ApiParam参数展开工具类，将嵌套的对象结构展开为扁平化的参数列表
 * @date 2025/01/27 10:00
 */
public class ApiParamFlattener {

    /**
     * 将嵌套的ApiParam树结构展开为扁平化的参数列表
     * 自动跳过最外层对象名称，直接展开第二层字段
     *
     * @param params 原始参数列表
     * @return 展开后的扁平参数列表
     */
    public static List<ApiParam> flattenParams(List<ApiParam> params) {
        List<ApiParam> flattenedParams = new ArrayList<>();
        if (params == null || params.isEmpty()) {
            return flattenedParams;
        }

        // 直接展开第二层，跳过最外层对象名称
        for (ApiParam param : params) {
            if (param.getChildren() != null && !param.getChildren().isEmpty()) {
                // 有子参数，说明是对象，直接展开子参数
                for (ApiParam child : param.getChildren()) {
                    flattenParam(child, "", flattenedParams);
                }
            } else {
                // 没有子参数，说明是叶子节点，直接添加
                flattenedParams.add(param);
            }
        }

        return flattenedParams;
    }

    /**
     * 递归展开单个ApiParam
     *
     * @param param 当前参数
     * @param parentPath 父级路径（如 "f.", "b[0]."）
     * @param flattenedParams 展开后的参数列表
     */
    private static void flattenParam(ApiParam param, String parentPath, List<ApiParam> flattenedParams) {
        if (param == null) {
            return;
        }

        String currentPath = parentPath + param.getName();
        
        // 如果有子参数，说明是对象类型，需要递归展开
        if (param.getChildren() != null && !param.getChildren().isEmpty()) {
            // 检查是否是数组类型
            if (param.getDataType() != null && "ARRAY".equals(param.getDataType().name())) {
                flattenArrayParam(param, parentPath, flattenedParams);
            } else {
                // 普通对象类型，递归展开
                for (ApiParam child : param.getChildren()) {
                    flattenParam(child, currentPath + ".", flattenedParams);
                }
            }
        } else {
            // 没有子参数，说明是叶子节点，添加到结果列表
            ApiParam flattenedParam = new ApiParam();
            flattenedParam.setName(currentPath);
            flattenedParam.setValue(param.getValue());
            flattenedParam.setDataType(param.getDataType());
            flattenedParam.setDescription(param.getDescription());
            flattenedParam.setType(param.getType());
            flattenedParam.setRawType(param.getRawType());
            flattenedParam.setContentType(param.getContentType());
            
            // 不复制children，避免循环引用
            flattenedParams.add(flattenedParam);
        }
    }

    /**
     * 处理数组类型的参数展开
     * 例如：List<eee> b → b[0].c, b[1].c, ...
     *
     * @param param 数组参数
     * @param parentPath 父级路径
     * @param flattenedParams 展开后的参数列表
     */
    private static void flattenArrayParam(ApiParam param, String parentPath, List<ApiParam> flattenedParams) {
        if (param == null || param.getChildren() == null || param.getChildren().isEmpty()) {
            return;
        }

        String arrayName = param.getName();
        
        // 如果是数组类型，为每个元素生成索引
        for (int i = 0; i < param.getChildren().size(); i++) {
            ApiParam child = param.getChildren().get(i);
            String arrayPath = parentPath + arrayName + "[" + i + "]";
            
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                // 子元素也是对象，继续递归
                for (ApiParam grandChild : child.getChildren()) {
                    flattenParam(grandChild, arrayPath + ".", flattenedParams);
                }
            } else {
                // 子元素是叶子节点
                ApiParam flattenedParam = new ApiParam();
                flattenedParam.setName(arrayPath);
                flattenedParam.setValue(child.getValue());
                flattenedParam.setDataType(child.getDataType());
                flattenedParam.setDescription(child.getDescription());
                flattenedParam.setType(child.getType());
                flattenedParam.setRawType(child.getRawType());
                flattenedParam.setContentType(child.getContentType());
                
                flattenedParams.add(flattenedParam);
            }
        }
    }
}
