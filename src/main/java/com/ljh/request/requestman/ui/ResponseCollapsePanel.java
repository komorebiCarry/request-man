package com.ljh.request.requestman.ui;

import com.intellij.ide.ui.LafManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

/**
 * @author leijianhui
 * @Description 响应内容折叠面板，支持内容展开/收起与高亮显示。
 * @date 2025/06/19 09:36
 */
public class ResponseCollapsePanel extends JPanel {
    private final JButton toggleButton;
    private final JPanel contentPanel;
    private boolean expanded = false;
    private final String title;
    private JLabel statusLabel;

    public ResponseCollapsePanel(String title) {
        setLayout(new BorderLayout());
        this.title = title;
        // 标题栏面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        toggleButton = new JButton("▼ " + title);
        toggleButton.setFocusPainted(false);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setFont(toggleButton.getFont().deriveFont(Font.PLAIN, 14f));
        statusLabel = new JLabel();
        statusLabel.setFont(toggleButton.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setForeground(new Color(220, 220, 170));
        titlePanel.add(toggleButton, BorderLayout.WEST);
        titlePanel.add(statusLabel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.NORTH);
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setVisible(false); // 默认收起
        add(contentPanel, BorderLayout.CENTER);
        toggleButton.addActionListener(e -> {
            expanded = !expanded;
            contentPanel.setVisible(expanded);
            toggleButton.setText((expanded ? "▲ " : "▼ ") + title);
            revalidate();
        });
    }

    /**
     * 设置响应内容组件
     */
    public void setResponseComponent(Component comp) {
        contentPanel.removeAll();
        contentPanel.add(comp, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * 设置响应内容文本（自动用JTextArea包裹）
     */
    public void setResponseText(String text) {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setText(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        try {
            // 使用更稳定的方式检测主题，避免使用实验性API
//            String lafName = LafManager.getInstance().getCurrentUIThemeLookAndFeel().getName().toLowerCase();
            boolean isDarkTheme = isDarkTheme();
            String themeFile = isDarkTheme ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml" : "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
            InputStream themeStream = getClass().getResourceAsStream(themeFile);
            if (themeStream != null) {
                Theme theme = Theme.load(themeStream);
                theme.apply(area);
            } else {
                // 默认使用暗色主题样式
                area.setBackground(new Color(43, 43, 43));
                area.setForeground(new Color(169, 183, 198));
                area.setCurrentLineHighlightColor(new Color(60, 63, 65));
            }
        } catch (Exception ignore) {
            // 异常时使用默认暗色主题样式
            area.setBackground(new Color(43, 43, 43));
            area.setForeground(new Color(169, 183, 198));
            area.setCurrentLineHighlightColor(new Color(60, 63, 65));
        }
        RTextScrollPane scroll = new RTextScrollPane(area);
        scroll.setPreferredSize(new Dimension(10, 300));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        setResponseComponent(scroll);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * 外部调用：展开响应内容区
     */
    public void expand() {
        if (!expanded) {
            expanded = true;
            contentPanel.setVisible(true);
            toggleButton.setText("▲ " + title);
            revalidate();
        }
    }

    /**
     * 外部调用：收起响应内容区
     */
    public void collapse() {
        if (expanded) {
            expanded = false;
            contentPanel.setVisible(false);
            toggleButton.setText("▼ " + title);
            revalidate();
        }
    }

    /**
     * 设置HTTP状态码文本（显示在标题栏右侧）
     */
    public void setStatusText(String status) {
        statusLabel.setText(status != null ? status : "");
    }

    /**
     * 获取响应文本内容
     *
     * @return 响应文本，如果没有则返回空字符串
     */
    public String getResponseText() {
        if (contentPanel.getComponentCount() > 0) {
            Component comp = contentPanel.getComponent(0);
            if (comp instanceof RTextScrollPane) {
                RTextScrollPane rTextScrollPane = (RTextScrollPane) comp;
                return rTextScrollPane.getTextArea().getText();
            }
        }
        return "";
    }

    /**
     * 检测当前是否为暗色主题
     * 使用稳定的API替代实验性API
     *
     * @return 是否为暗色主题
     */
    private boolean isDarkTheme() {
        try {
            // 使用UIManager检测主题，这是更稳定的方式
            Color backgroundColor = UIManager.getColor("Panel.background");
            if (backgroundColor != null) {
                // 计算背景色的亮度，判断是否为暗色主题
                float[] hsb = Color.RGBtoHSB(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), null);
                return hsb[2] < 0.5; // 亮度小于0.5认为是暗色主题
            }
            
            // 备用方案：检查Look and Feel名称
            String lookAndFeel = UIManager.getLookAndFeel().getName().toLowerCase();
            return lookAndFeel.contains("darcula") || lookAndFeel.contains("dark") || lookAndFeel.contains("intellij");
        } catch (Exception e) {
            // 异常时默认使用暗色主题
            return true;
        }
    }
} 