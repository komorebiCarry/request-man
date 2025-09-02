package com.ljh.request.requestman.ui.builder;

import com.ljh.request.requestman.enums.ContentType;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.ui.JsonBodyStructurePanel;
import com.ljh.request.requestman.ui.PreviewDocPanel;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

/**
 * 负责构建与请求/响应文档展示相关的无状态视图组件。
 * 仅承载纯视图渲染逻辑，不包含任何业务和状态写入。
 *
 * @author leijianhui
 * @Description 视图构建工具，提供纯展示面板的构建方法
 * @date 2025/07/27 10:30
 */
public final class RequestViewBuilders {

    private RequestViewBuilders() {
    }

    /**
     * 构建响应定义面板。
     *
     * @param apiInfo 当前接口信息
     * @return 响应定义面板
     */
    public static JPanel buildResponseDefinitionPanel(ApiInfo apiInfo) {
        JPanel responsePanel = new JPanel(new BorderLayout());
        List<ApiParam> responseParams = apiInfo != null ? apiInfo.getResponseParams() : null;

        JsonBodyStructurePanel structurePanel = new JsonBodyStructurePanel(responseParams);
        responsePanel.add(structurePanel, BorderLayout.CENTER);
        responsePanel.putClientProperty("structurePanel", structurePanel);
        return responsePanel;
    }

    /**
     * 构建接口说明面板。
     *
     * @param apiInfo 接口信息
     * @return 接口说明面板
     */
    public static JPanel buildDocPanel(ApiInfo apiInfo) {
        JPanel docPanel = new JPanel();
        docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
        Border emptyBorder = BorderFactory.createEmptyBorder(20, 0, 0, 0);

        JLabel jLabelName = new JLabel("<html>" + RequestManBundle.message("doc.api.name") + ": <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getName() + "</html>");
        docPanel.add(jLabelName);

        JLabel jLabelHttpMethod = new JLabel("<html>" + RequestManBundle.message("doc.api.request") + ": <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getHttpMethod() + "</html>");
        jLabelHttpMethod.setBorder(emptyBorder);
        docPanel.add(jLabelHttpMethod);

        JLabel jLabelDisplayText = new JLabel("<html>" + RequestManBundle.message("doc.api.info") + ": <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getDisplayText() + "</html>");
        jLabelDisplayText.setBorder(emptyBorder);
        docPanel.add(jLabelDisplayText);

        JLabel jLabelClassName = new JLabel("<html>" + RequestManBundle.message("doc.api.class") + ": <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getClassName() + "</html>");
        jLabelClassName.setBorder(emptyBorder);
        docPanel.add(jLabelClassName);

        JLabel jLabelDescription = new JLabel("<html>" + RequestManBundle.message("doc.api.description") + ": <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + apiInfo.getDescription() + "</html>");
        jLabelDescription.setBorder(emptyBorder);
        docPanel.add(jLabelDescription);

        return docPanel;
    }

    /**
     * 构建预览文档面板。
     *
     * @param apiInfo 接口信息
     * @return 预览文档面板
     */
    public static JPanel buildPreviewPanel(ApiInfo apiInfo) {
        List<ApiParam> pathParams = apiInfo.getParams() != null ? apiInfo.getParams().stream().filter(p -> RequestManBundle.message("doc.param.type.path").equals(p.getType())).toList() : java.util.Collections.emptyList();
        List<ApiParam> queryParams = apiInfo.getParams() != null ? apiInfo.getParams().stream().filter(p -> RequestManBundle.message("doc.param.type.query").equals(p.getType())).toList() : java.util.Collections.emptyList();
        List<ApiParam> bodyParams = apiInfo.getBodyParams();
        ContentType contentType = ContentType.APPLICATION_JSON;
        String responseJson = "{\n  \"code\": \"\",\n  \"message\": \"\",\n  \"data\": null\n}";
        return new PreviewDocPanel(apiInfo, contentType, bodyParams, pathParams, queryParams, responseJson);
    }
}


