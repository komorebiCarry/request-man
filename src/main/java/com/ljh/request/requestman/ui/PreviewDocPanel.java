package com.ljh.request.requestman.ui;

import cn.hutool.core.util.StrUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.enums.ContentType;
import com.ljh.request.requestman.enums.ParamDataType;
import com.ljh.request.requestman.model.ApiInfo;
import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.ProjectUtils;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author leijianhui
 * @Description 接口文档预览面板，支持接口结构和示例展示。
 * @date 2025/06/17 16:32
 */
public class PreviewDocPanel extends JPanel {
    private JTextArea textArea;
    private JButton copyBtn;

    public PreviewDocPanel(ApiInfo apiInfo, ContentType contentType, List<ApiParam> bodyParams, List<ApiParam> pathParams, List<ApiParam> queryParams, String responseJson) {
        setLayout(new BorderLayout());
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        copyBtn = new JButton(RequestManBundle.message("preview.copy"));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(copyBtn);
        add(btnPanel, BorderLayout.SOUTH);
        // 生成文档内容
        String doc = buildDoc(apiInfo, contentType, bodyParams, pathParams, queryParams, responseJson);
        textArea.setText(doc);
        // 复制功能
        copyBtn.addActionListener((ActionEvent e) -> {
            StringSelection sel = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            // 右下角气泡提示（兼容所有IDE版本，不依赖自定义通知组）
            Project project = ProjectUtils.getCurrentProject();
            new Notification(
                    "RequestMan", // groupDisplayId，可自定义
                    RequestManBundle.message("preview.copy.success"),
                    "",
                    NotificationType.INFORMATION
            ).notify(project);
        });
    }

    /**
     * 生成接口文档内容
     */
    private String buildDoc(ApiInfo apiInfo, ContentType contentType, List<ApiParam> bodyParams, List<ApiParam> pathParams, List<ApiParam> queryParams, String responseJson) {
        StringBuilder sb = new StringBuilder();
        // 接口名称优先显示description，无则方法名
        String displayName = (apiInfo.getDescription() != null && !apiInfo.getDescription().isEmpty()) ? apiInfo.getDescription() : apiInfo.getMethodName();
        sb.append(RequestManBundle.message("preview.doc.api.name")).append("\n").append(displayName).append("\n\n");
        sb.append(RequestManBundle.message("preview.doc.api.url")).append("\n").append(apiInfo.getHttpMethod()).append(": ").append(apiInfo.getUrl()).append("\n\n");
        if (pathParams != null && !pathParams.isEmpty()) {
            sb.append(RequestManBundle.message("preview.doc.path.params")).append("\n");
            for (ApiParam p : pathParams) {
                sb.append(p.getName());
                String desc = getDesc(p);
                if (StrUtil.isNotBlank(desc)) {
                    sb.append(" //").append(getDesc(p));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append(RequestManBundle.message("preview.doc.query.params")).append("\n");
            for (ApiParam p : queryParams) {
                sb.append(p.getName());
                String desc = getDesc(p);
                if (StrUtil.isNotBlank(desc)) {
                    sb.append(" //").append(getDesc(p));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        sb.append(RequestManBundle.message("preview.doc.body.params")).append("\n");
        sb.append(RequestManBundle.message("preview.doc.contenttype")).append(" ").append(contentType.getValue()).append("\n\n");
        // Body参数展示，单一对象时直接展开children
        List<ApiParam> showParams = bodyParams;
        boolean isArray = false;
        int index = 0;
        if (bodyParams != null && bodyParams.size() == 1 && bodyParams.get(0).getChildren() != null && !bodyParams.get(0).getChildren().isEmpty()) {
            ApiParam apiParam = bodyParams.get(0);
            if (ParamDataType.ARRAY.equals(apiParam.getDataType())) {
                isArray = true;
                sb.append("[");
                sb.append("\n");
                index = 2;
            }
            showParams = bodyParams.get(0).getChildren();
        }
        if (contentType == ContentType.APPLICATION_JSON || contentType == ContentType.APPLICATION_XML || contentType == ContentType.TEXT_PLAIN) {
            // 展示带注释的json
            sb.append(genJsonWithComment(showParams, index));
            if (isArray) {
                sb.append("\n");
                sb.append("]\n\n");
            } else {
                sb.append("\n\n");
            }
        } else if (contentType == ContentType.MULTIPART_FORM_DATA || contentType == ContentType.APPLICATION_FORM_URLENCODED) {
            // 展示表格
            sb.append(genFormTable(showParams, 0)).append("\n\n");
        } else {
            sb.append("--\n\n");
        }
        sb.append(RequestManBundle.message("preview.doc.response")).append("\n");
        if (apiInfo.getResponseParams() != null && !apiInfo.getResponseParams().isEmpty()) {
            sb.append(genJsonWithComment(apiInfo.getResponseParams(), 0)).append("\n");
        } else {
            sb.append(genJsonWithCommentForResponse(responseJson)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 生成带注释的json（字段右侧//注释）
     */
    private String genJsonWithComment(List<ApiParam> params, int indent) {
        StringBuilder sb = new StringBuilder();
        String pad = "    ".repeat(indent);
        if (params == null || params.isEmpty()) {
            sb.append(pad).append("{}");
            return sb.toString();
        }
        sb.append(pad).append("{").append("\n");
        for (int i = 0; i < params.size(); i++) {
            ApiParam p = params.get(i);
            sb.append(pad).append("    ").append('"').append(p.getName()).append('"').append(": ");
            if (p.getDataType() != null && p.getDataType().name().equalsIgnoreCase("ARRAY")) {
                // 数组类型，递归children为数组元素结构
                sb.append("[");
                if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                    sb.append("\n");
                    sb.append(genJsonWithComment(p.getChildren(), indent + 2));
                    sb.append("\n").append(pad).append("    ");
                }
                sb.append("]");
            } else if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                sb.append(genJsonWithComment(p.getChildren(), indent + 1));
            } else {
                sb.append(getJsonDefaultValue(p));
            }
            if (i < params.size() - 1) {
                sb.append(",");
            }
            String desc = getDesc(p);
            if (!desc.isEmpty()) {
                sb.append(" // ").append(desc);
            }
            sb.append("\n");
        }
        sb.append(pad).append("}");
        return sb.toString();
    }

    /**
     * 生成表单类型的树状结构表格
     */
    private String genFormTable(List<ApiParam> params, int level) {
        StringBuilder sb = new StringBuilder();
        if (level == 0) {
            sb.append(String.format("%-20s %-10s %-10s %s\n", RequestManBundle.message("preview.doc.table.field"), RequestManBundle.message("preview.doc.table.type"), RequestManBundle.message("preview.doc.table.ref"), RequestManBundle.message("preview.doc.table.comment")));
            sb.append(RequestManBundle.message("preview.doc.table.separator")).append("\n");
        }
        if (params == null) return sb.toString();
        for (ApiParam p : params) {
            String indent = "  ".repeat(level);
            String type = p.getRawType() != null && !p.getRawType().isEmpty() ? p.getRawType() : (p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string");
            String ref = getJsonDefaultValue(p);
            String desc = getDesc(p);
            sb.append(String.format("%s%-18s %-10s %-10s", indent, p.getName(), type, ref));
            if (!desc.isEmpty()) {
                sb.append(" ").append(desc);
            }
            sb.append("\n");
            if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                sb.append(genFormTable(p.getChildren(), level + 1));
            }
        }
        return sb.toString();
    }

    /**
     * 生成带注释的响应json
     */
    private String genJsonWithCommentForResponse(String responseJson) {
        // 简单处理：每行后加 // 注释（如有）
        if (responseJson == null || responseJson.isEmpty()) return "{}";
        String[] lines = responseJson.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            // 可扩展：如需自动注释，可结合响应结构模型
            if (line.contains("\"code\"")) sb.append(" // ").append(RequestManBundle.message("preview.doc.response.code"));
            if (line.contains("\"message\"")) sb.append(" // ").append(RequestManBundle.message("preview.doc.response.message"));
            if (line.contains("\"data\"")) sb.append(" // ").append(RequestManBundle.message("preview.doc.response.data"));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取注释，优先Swagger注解，无则JavaDoc，无注释时返回空字符串
     */
    private String getDesc(ApiParam p) {
        StringBuilder desc = new StringBuilder();
        if (p.getDescription() != null && !p.getDescription().isEmpty()) {
            desc.append(p.getDescription());
        }
        
        // 如果是递归字段，添加递归标识
        if (p.isRecursive()) {
            if (desc.length() > 0) {
                desc.append(" ");
            }
            // 获取递归目标类名
            String recursiveTarget = getRecursiveTarget(p);
            if (recursiveTarget != null) {
                desc.append("[").append(RequestManBundle.message("preview.doc.recursive")).append(recursiveTarget).append("]");
            } else {
                desc.append("[").append(RequestManBundle.message("preview.doc.recursive")).append("]");
            }
        }
        
        return desc.toString();
    }

    /**
     * 获取递归目标类名
     */
    private String getRecursiveTarget(ApiParam p) {
        // 如果字段有子参数，从子参数中获取类型信息
        if (p.getChildren() != null && !p.getChildren().isEmpty()) {
            // 取第一个子参数的类型作为递归目标
            ApiParam firstChild = p.getChildren().get(0);
            if (firstChild.getRawType() != null) {
                return firstChild.getRawType();
            }
        }
        
        // 如果没有子参数，尝试从字段类型推断
        if (p.getRawType() != null) {
            return p.getRawType();
        }
        
        return null;
    }

    /**
     * 获取json默认值
     */
    private String getJsonDefaultValue(ApiParam p) {
        String type = p.getDataType() != null ? p.getDataType().name().toLowerCase() : "string";
        switch (type) {
            case "integer":
                return "0";
            case "boolean":
                return "false";
            case "number":
                return "0.0";
            case "array":
                return "[]";
            case "file":
                return "\"<file>\"";
            default:
                return "\"string\"";
        }
    }
} 