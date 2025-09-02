package com.ljh.request.requestman.util;

import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * 请求响应处理器，用于处理请求响应结果。
 * 该接口定义了处理HTTP请求响应的标准方法，包括成功响应、异常处理等。
 *
 * @author leijianhui
 * @Description 请求响应处理器，定义处理HTTP请求响应的标准方法
 * @date 2025/01/27 10:36
 */
public interface ResponseHandler {
    
    /**
     * 处理请求成功响应
     * 
     * @param status HTTP状态码
     * @param responseText 响应文本
     * @param responseBytes 响应字节数组
     * @param contentType 响应内容类型
     */
    void onSuccess(int status, String responseText, byte[] responseBytes, String contentType);
    
    /**
     * 处理请求异常
     * 
     * @param exception 异常信息
     */
    void onError(Exception exception);
    
    /**
     * 获取按钮组件，用于恢复按钮状态
     * 
     * @return 按钮组件
     */
    JButton getButton();
    
    /**
     * 获取响应面板，用于显示响应结果
     * 
     * @return 响应面板
     */
    JComponent getResponsePanel();
}

