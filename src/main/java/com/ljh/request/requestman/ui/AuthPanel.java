package com.ljh.request.requestman.ui;

import javax.swing.*;
import java.awt.*;
import com.ljh.request.requestman.util.RequestManBundle;


/**
 * @author leijianhui
 * @Description 认证信息面板，支持多种认证方式的输入与切换。
 * @date 2025/06/17 19:48
 */
public class AuthPanel extends JPanel {
    private static final Dimension PARAM_PANEL_SIZE = new Dimension(600, 60);
    /**
     * 认证信息输入框
     */
    private final JTextField authField;
    private JComboBox<String> modeBox;
    private static final String[] MODE_OPTIONS = {RequestManBundle.message("auth.inherit"), RequestManBundle.message("auth.custom")};
    private String globalAuthValue = "";

    public AuthPanel() {
        super(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        modeBox = new JComboBox<>(MODE_OPTIONS);
        JLabel label = new JLabel(RequestManBundle.message("auth.info") + "：");
        authField = new JTextField();
        authField.setToolTipText(RequestManBundle.message("auth.tooltip"));
        topPanel.add(label, BorderLayout.WEST);
        topPanel.add(modeBox, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        add(authField, BorderLayout.CENTER);
        JLabel tip = new JLabel(RequestManBundle.message("auth.tooltip"));
        tip.setFont(new Font(null, Font.ITALIC, 12));
        tip.setForeground(new Color(120, 120, 120));
        add(tip, BorderLayout.SOUTH);
        modeBox.addActionListener(e -> updateAuthFieldState());
        updateAuthFieldState();
    }

    private void updateAuthFieldState() {
        if (modeBox.getSelectedIndex() == 0) { // 继承
            authField.setEnabled(false);
            authField.setText(globalAuthValue != null ? globalAuthValue : "");
        } else {
            authField.setEnabled(true);
        }
    }

    /**
     * 设置全局auth值（供设置页调用）
     */
    public void setGlobalAuthValue(String value) {
        this.globalAuthValue = value;
        if (modeBox.getSelectedIndex() == 0) {
            authField.setText(value != null ? value : "");
        }
    }

    /**
     * 获取认证信息输入框内容
     *
     * @return 认证信息字符串
     */
    public String getAuthValue() {
        if (modeBox.getSelectedIndex() == 0) {
            return globalAuthValue != null ? globalAuthValue : "";
        } else {
            return authField.getText();
        }
    }

    /**
     * 获取认证模式
     *
     * @return 认证模式索引
     */
    public int getAuthMode() {
        return modeBox.getSelectedIndex();
    }

    /**
     * 设置认证模式
     *
     * @param mode 认证模式索引
     */
    public void setAuthMode(int mode) {
        if (mode >= 0 && mode < modeBox.getItemCount()) {
            modeBox.setSelectedIndex(mode);
        }
    }

    /**
     * 设置认证信息输入框内容（用于持久化恢复）
     *
     * @param value 认证信息
     */
    public void setAuthValue(String value) {
        authField.setText(value != null ? value : "");
    }
} 