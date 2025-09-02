package com.ljh.request.requestman.ui.builder;

import com.ljh.request.requestman.ui.*;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义模式右侧编辑区域的无状态拼装器。
 * 仅负责把已初始化的控件进行布局添加，不包含任何业务与监听逻辑。
 *
 * @author leijianhui
 * @Description 自定义模式编辑区视图拼装器，仅做UI拼装
 * @date 2025/07/27 11:20
 */
public final class CustomEditPanelsBuilder {

    private CustomEditPanelsBuilder() {
    }

    /**
     * 顶部一行控件载体。
     */
    public static final class TopRowComponents {
        public JLabel nameLabel;
        public JTextField nameField;
        public JComponent nameStarLabel; // 允许为空
        public JComponent extraComponent; // 如 Params 面板或发送面板
    }

    /**
     * 组装顶部一行。
     */
    public static JPanel assembleTopRow(TopRowComponents c) {
        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        topRow.add(c.nameLabel);
        topRow.add(Box.createHorizontalStrut(8));
        topRow.add(c.nameField);
        if (c.nameStarLabel != null) {
            topRow.add(Box.createHorizontalStrut(6));
            topRow.add(c.nameStarLabel);
        }
        if (c.extraComponent != null) {
            topRow.add(Box.createHorizontalStrut(16));
            topRow.add(c.extraComponent);
        }
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return topRow;
    }

    /**
     * 将 URL 与 METHOD 相关控件按顺序添加至容器。
     */
    public static void assembleUrlAndMethod(JPanel container,
                                            JLabel urlLabel, JTextField urlField,
                                            JLabel methodLabel, JComboBox<String> methodBox) {
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(urlLabel);
        urlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        urlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(urlField);

        methodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(methodLabel);
        methodBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        methodBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(methodBox);
    }

    /**
     * 自定义模式Tabs载体。
     */
    public static final class CustomTabsComponents {
        public ParamsTablePanel customParamsPanel;
        public EditableBodyPanel customBodyPanel;
        public HeadersPanel customHeadersPanel;
        public CookiesPanel customCookiesPanel;
        public AuthPanel customAuthPanel;
        public PreOpPanel customPreOpPanel;
        public PostOpPanel customPostOpPanel;

        public CustomTabsComponents(RequestManPanel requestManPanel) {
            this.customParamsPanel = requestManPanel.getCustomParamsPanel();
            this.customBodyPanel = requestManPanel.getCustomBodyPanel();
            this.customHeadersPanel = requestManPanel.getCustomHeadersPanel();
            this.customCookiesPanel = requestManPanel.getCustomCookiesPanel();
            this.customAuthPanel = requestManPanel.getCustomAuthPanel();
            this.customPreOpPanel = requestManPanel.getCustomPreOpPanel();
            this.customPostOpPanel = requestManPanel.getCustomPostOpPanel();
        }
    }

    /**
     * 组装自定义模式Tab区域。
     */
    public static JTabbedPane assembleCustomTabs(CustomTabsComponents c) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Params", c.customParamsPanel);
        tabbedPane.addTab("Body", c.customBodyPanel);
        tabbedPane.addTab("Headers", c.customHeadersPanel);
        tabbedPane.addTab("Cookies", c.customCookiesPanel);
        tabbedPane.addTab("Auth", c.customAuthPanel);
        tabbedPane.addTab(RequestManBundle.message("tab.preop"), c.customPreOpPanel);
        tabbedPane.addTab(RequestManBundle.message("tab.postop"), c.customPostOpPanel);
        return tabbedPane;
    }

    /**
     * 组装底部按钮行。
     */
    public static JPanel assembleButtonRow(JButton saveBtn, JButton deleteBtn) {
        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        btnPanel.add(deleteBtn);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btnPanel;
    }
}


