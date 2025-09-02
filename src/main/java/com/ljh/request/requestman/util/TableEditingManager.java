package com.ljh.request.requestman.util;

import com.ljh.request.requestman.ui.*;

import javax.swing.*;

/**
 * 表格编辑统一停止管理器。
 * 仅封装停止编辑的具体实现，保持与原有行为一致。
 *
 * @author leijianhui
 * @Description 统一停止表格编辑的工具
 * @date 2025/07/27 11:40
 */
public final class TableEditingManager {

    private TableEditingManager() {
    }

    /**
     * 调用方上下文，仅承载面板引用。
     */
    public static final class RequestContext {
        public ParamsTablePanel paramsPanel;
        public BodyPanel bodyPanel;
        public HeadersPanel headersPanel;
        public CookiesPanel cookiesPanel;
        public AuthPanel authPanel;
        public PreOpPanel preOpPanel;
        public PostOpPanel postOpPanel;

        public EditableBodyPanel customBodyPanel;
        public HeadersPanel customHeadersPanel;
        public CookiesPanel customCookiesPanel;
        public PreOpPanel customPreOpPanel;
        public PostOpPanel customPostOpPanel;
        public ParamsTablePanel customParamsPanel;

        public RequestContext(RequestManPanel requestManPanel) {
            this.paramsPanel = requestManPanel.getParamsPanel();
            this.bodyPanel = requestManPanel.getBodyPanel();
            this.headersPanel = requestManPanel.getHeadersPanel();
            this.cookiesPanel = requestManPanel.getCookiesPanel();
            this.authPanel = requestManPanel.getAuthPanel();
            this.preOpPanel = requestManPanel.getPreOpPanel();
            this.postOpPanel = requestManPanel.getPostOpPanel();
            this.customBodyPanel = requestManPanel.getCustomBodyPanel();
            this.customHeadersPanel = requestManPanel.getCustomHeadersPanel();
            this.customCookiesPanel = requestManPanel.getCustomCookiesPanel();
            this.customPreOpPanel = requestManPanel.getCustomPreOpPanel();
            this.customPostOpPanel = requestManPanel.getCustomPostOpPanel();
            this.customParamsPanel = requestManPanel.getCustomParamsPanel();
        }
    }

    public static void stopAll(RequestContext ctx) {
        stopParams(ctx);
        stopBody(ctx);
        stopHeaders(ctx);
        stopCookies(ctx);
        stopPreOp(ctx);
        stopPostOp(ctx);
    }

    public static void stopByTabIndex(RequestContext ctx, int tabIndex) {
        switch (tabIndex) {
            case 0 -> stopParams(ctx);
            case 1 -> stopBody(ctx);
            case 2 -> stopHeaders(ctx);
            case 3 -> stopCookies(ctx);
            case 4 -> { /* Auth无表格，保持兼容 */ }
            case 5 -> stopPreOp(ctx);
            case 6 -> stopPostOp(ctx);
            default -> stopAll(ctx);
        }
    }

    private static void stopParams(RequestContext ctx) {
        if (ctx.customParamsPanel != null) {
            JTable table = SwingUtils.getTable(ctx.customParamsPanel);
            stop(table);
        }
        if (ctx.paramsPanel != null) {
            JTable table = SwingUtils.getTable(ctx.paramsPanel);
            stop(table);
        }
    }

    private static void stopBody(RequestContext ctx) {
        if (ctx.customBodyPanel != null) {
            Object formDataPanel = SwingUtils.getObject(ctx.customBodyPanel, "formDataPanel");
            stop(SwingUtils.getTable(formDataPanel));
            Object urlencodedPanel = SwingUtils.getObject(ctx.customBodyPanel, "urlencodedPanel");
            stop(SwingUtils.getTable(urlencodedPanel));
        }
        if (ctx.bodyPanel != null) {
            Object formDataPanel = SwingUtils.getObject(ctx.bodyPanel, "formDataPanel");
            stop(SwingUtils.getTable(formDataPanel));
            Object urlencodedPanel = SwingUtils.getObject(ctx.bodyPanel, "urlencodedPanel");
            stop(SwingUtils.getTable(urlencodedPanel));
        }
    }

    private static void stopHeaders(RequestContext ctx) {
        if (ctx.customHeadersPanel != null) {
            Object pp = SwingUtils.getObject(ctx.customHeadersPanel, "paramsPanel");
            stop(SwingUtils.getTable(pp));
        }
        if (ctx.headersPanel != null) {
            Object pp = SwingUtils.getObject(ctx.headersPanel, "paramsPanel");
            stop(SwingUtils.getTable(pp));
        }
    }

    private static void stopCookies(RequestContext ctx) {
        if (ctx.customCookiesPanel != null) {
            Object pp = SwingUtils.getObject(ctx.customCookiesPanel, "paramsPanel");
            stop(SwingUtils.getTable(pp));
        }
        if (ctx.cookiesPanel != null) {
            Object pp = SwingUtils.getObject(ctx.cookiesPanel, "paramsPanel");
            stop(SwingUtils.getTable(pp));
        }
    }

    private static void stopPreOp(RequestContext ctx) {
        if (ctx.customPreOpPanel != null) {
            stop(SwingUtils.getTable(ctx.customPreOpPanel));
        }
        if (ctx.preOpPanel != null) {
            stop(SwingUtils.getTable(ctx.preOpPanel));
        }
    }

    private static void stopPostOp(RequestContext ctx) {
        if (ctx.customPostOpPanel != null) {
            stop(SwingUtils.getTable(ctx.customPostOpPanel));
        }
        if (ctx.postOpPanel != null) {
            stop(SwingUtils.getTable(ctx.postOpPanel));
        }
    }

    private static void stop(JTable table) {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }
}


