package com.ljh.request.requestman.ui.builder;

import javax.swing.*;
import java.awt.*;

/**
 * 顶部面板构建器，只负责拼装视觉元素与传入的现成控件/事件，不包含业务。
 * 通过上下文对象传入已创建的控件与回调，确保行为保持不变。
 *
 * @author leijianhui
 * @Description 构建顶部工具栏视图的无状态构建器
 * @date 2025/07/27 10:35
 */
public final class TopPanelBuilder {

    private TopPanelBuilder() {
    }

    /**
     * 顶部面板上下文，仅作为视图构建所需的控件载体。
     */
    public static final class TopPanelContext {
        public JButton apiSearchButton;
        public JButton modeSwitchButton;
        public JButton refreshOrAddButton;
        public JComboBox<?> apiComboBox;
        public JComponent locateButton;
        public JComponent environmentSelector;
        public JComponent performanceButton; // 允许为null
    }

    /**
     * 构建顶部面板。
     *
     * @param ctx 上下文，携带现成控件
     * @return 顶部面板
     */
    public static JPanel buildTopPanel(TopPanelContext ctx) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(ctx.apiSearchButton);
        panel.add(Box.createHorizontalStrut(8));

        panel.add(ctx.modeSwitchButton);
        panel.add(Box.createHorizontalStrut(8));

        ctx.refreshOrAddButton.setPreferredSize(new Dimension(36, 36));
        ctx.refreshOrAddButton.setMaximumSize(new Dimension(36, 36));
        ctx.refreshOrAddButton.setFocusPainted(false);
        ctx.refreshOrAddButton.setBorderPainted(true);
        panel.add(ctx.refreshOrAddButton);
        panel.add(Box.createHorizontalStrut(8));

        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.X_AXIS));
        comboPanel.add(ctx.apiComboBox);
        comboPanel.add(Box.createHorizontalStrut(4));
        comboPanel.add(ctx.locateButton);
        comboPanel.setMaximumSize(new Dimension(600, 36));
        panel.add(comboPanel);

        panel.add(Box.createHorizontalStrut(8));
        panel.add(ctx.environmentSelector);

        if (ctx.performanceButton != null) {
            panel.add(Box.createHorizontalStrut(8));
            panel.add(ctx.performanceButton);
        }

        return panel;
    }
}


