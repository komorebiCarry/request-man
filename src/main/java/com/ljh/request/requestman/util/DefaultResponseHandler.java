package com.ljh.request.requestman.util;

import com.ljh.request.requestman.ui.ResponseCollapsePanel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * 默认响应处理器，实现ResponseHandler接口。
 * 该类提供了标准的HTTP响应处理逻辑，包括状态显示、响应内容展示、异常处理等。
 *
 * @author leijianhui
 * @Description 默认响应处理器，提供标准的HTTP响应处理逻辑
 * @date 2025/01/27 10:38
 */
public class DefaultResponseHandler implements ResponseHandler {
    
    /**
     * 按钮组件，用于控制按钮状态
     */
    private final JButton button;
    
    /**
     * 响应面板，用于显示响应结果
     */
    private final ResponseCollapsePanel responsePanel;
    
    /**
     * 构造函数
     * 
     * @param button 按钮组件
     * @param responsePanel 响应面板
     */
    public DefaultResponseHandler(JButton button, ResponseCollapsePanel responsePanel) {
        this.button = button;
        this.responsePanel = responsePanel;
    }

    @Override
    public void onSuccess(int status, String responseText, byte[] responseBytes, String contentType) {
        // 构建状态消息
        String statusMsg = RequestManBundle.message("main.http.status", status,
                status == 200 ? RequestManBundle.message("main.http.ok") : RequestManBundle.message("main.http.fail"));
        
        // 格式化响应文本
        String formattedResponse = formatResponseText(responseText);
        
        // 更新响应面板
        responsePanel.setStatusText(statusMsg);
        responsePanel.setResponseText(formattedResponse);
        responsePanel.expand();
    }

    @Override
    public void onError(Exception exception) {
        // 设置错误信息
        responsePanel.setStatusText("");
        responsePanel.setResponseText(RequestManBundle.message("common.request.error") + exception.getMessage());
        responsePanel.expand();
        
        // 显示错误对话框
        JOptionPane.showMessageDialog(responsePanel,
            RequestManBundle.message("common.request.error") + exception.getMessage(), RequestManBundle.message("common.error"), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public JButton getButton() {
        return button;
    }

    @Override
    public JComponent getResponsePanel() {
        return responsePanel;
    }

    /**
     * 格式化响应内容，支持JSON/XML/HTML美化
     * 
     * @param text 原始响应文本
     * @return 格式化后的响应文本
     */
    private String formatResponseText(String text) {
        if (isJson(text)) {
            try {
                return cn.hutool.json.JSONUtil.formatJsonStr(text);
            } catch (Exception ignore) {
                // 格式化失败时返回原文本
            }
        } else if (isXml(text)) {
            try {
                return cn.hutool.core.util.XmlUtil.format(text);
            } catch (Exception ignore) {
                // 格式化失败时返回原文本
            }
        } else if (isHtml(text)) {
            try {
                return org.jsoup.Jsoup.parse(text).outerHtml();
            } catch (Exception ignore) {
                // 格式化失败时返回原文本
            }
        }
        return text;
    }

    /**
     * 判断字符串是否为JSON格式
     * 
     * @param text 待判断的字符串
     * @return 是否为JSON格式
     */
    private boolean isJson(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    /**
     * 判断字符串是否为XML格式
     * 
     * @param text 待判断的字符串
     * @return 是否为XML格式
     */
    private boolean isXml(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return t.startsWith("<") && t.endsWith(">") && t.contains("<?xml");
    }

    /**
     * 判断字符串是否为HTML格式
     * 
     * @param text 待判断的字符串
     * @return 是否为HTML格式
     */
    private boolean isHtml(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim().toLowerCase();
        return t.startsWith("<!doctype html") || t.startsWith("<html");
    }
}

