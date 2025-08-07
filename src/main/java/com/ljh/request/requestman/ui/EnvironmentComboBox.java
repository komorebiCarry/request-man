package com.ljh.request.requestman.ui;

import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.Environment;
import com.ljh.request.requestman.util.ProjectSettingsManager;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * 自定义环境选择ComboBox，支持在弹出菜单中添加管理环境按钮。
 *
 * @author leijianhui
 * @Description 自定义环境选择ComboBox，支持环境切换和管理。
 * @date 2025/01/27 16:45
 */
public class EnvironmentComboBox extends JComboBox<Environment> {

    private final Project project;
    private final List<Environment> environments;
    private final ActionListener manageEnvironmentAction;

    // 标志位：是否正在更新模型，避免触发监听器
    private boolean isUpdatingModel = false;

    public EnvironmentComboBox(Project project, List<Environment> environments, ActionListener manageEnvironmentAction) {
        super();
        this.project = project;
        this.environments = environments;
        this.manageEnvironmentAction = manageEnvironmentAction;
        this.setUI(new MyDarculaComboBoxUI());
        initComponents();
        loadEnvironments();
    }

    private void initComponents() {
        // 设置渲染器
        setRenderer(new EnvironmentListCellRenderer());

        // 禁用编辑模式
        setEditable(false);

        // 添加弹出菜单监听器
        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // 弹出菜单显示时，添加管理环境按钮
                addManageEnvironmentButton();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // 弹出菜单隐藏时的处理
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // 弹出菜单取消时的处理
            }
        });

        // 添加选择监听器 - 使用标志位避免重复注册
        if (getActionListeners().length == 0) {
            addActionListener(e -> {
                // 如果正在更新模型，不执行保存操作
                if (!isUpdatingModel) {
                    Environment selected = getSelectedEnvironment();
                    if (selected != null) {
                        ProjectSettingsManager.setCurrentEnvironmentId(project, selected.getId());
                    }
                }
            });
        }
    }

    /**
     * 在弹出菜单中添加管理环境按钮
     */
    private void addManageEnvironmentButton() {
        // 获取弹出菜单的UI组件
        javax.swing.plaf.ComboBoxUI ui = getUI();
        if (ui instanceof MyDarculaComboBoxUI) {
            MyDarculaComboBoxUI basicUI = (MyDarculaComboBoxUI) ui;
            JButton manageButton = basicUI.addManageEnvironmentButton();
            if (manageButton != null) {
                // 检查是否已经添加过ActionListener
                ActionListener[] listeners = manageButton.getActionListeners();
                boolean hasListener = false;
                for (ActionListener listener : listeners) {
                    if (listener instanceof ManageEnvironmentActionListener) {
                        hasListener = true;
                        break;
                    }
                }

                if (!hasListener) {
                    manageButton.addActionListener(new ManageEnvironmentActionListener());
                }
            }
        }
    }

    /**
     * 管理环境按钮的ActionListener
     */
    private class ManageEnvironmentActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            hidePopup();
            if (manageEnvironmentAction != null) {
                manageEnvironmentAction.actionPerformed(new ActionEvent(EnvironmentComboBox.this, ActionEvent.ACTION_PERFORMED, "manageEnvironment"));
            }
        }
    }

    /**
     * 加载环境列表
     */
    private void loadEnvironments() {
        DefaultComboBoxModel<Environment> model = new DefaultComboBoxModel<>();

        // 设置更新标志，避免触发监听器
        isUpdatingModel = true;

        // 添加环境列表
        for (Environment env : environments) {
            model.addElement(env);
        }

        setModel(model);

        // 清除更新标志
        isUpdatingModel = false;

        // 设置当前选中的环境
        String currentId = ProjectSettingsManager.getCurrentEnvironmentId(project);
        if (currentId != null) {
            for (int i = 0; i < model.getSize(); i++) {
                Environment env = model.getElementAt(i);
                if (env.getId().equals(currentId)) {
                    setSelectedItem(env);
                    break;
                }
            }
        }
    }

    /**
     * 获取当前选中的环境
     */
    public Environment getSelectedEnvironment() {
        Object selected = getSelectedItem();
        return selected instanceof Environment ? (Environment) selected : null;
    }

    /**
     * 设置选中的环境
     */
    public void setSelectedEnvironment(Environment environment) {
        if (environment != null) {
            setSelectedItem(environment);
            ProjectSettingsManager.setCurrentEnvironmentId(project, environment.getId());
        }
    }

    /**
     * 刷新环境列表
     */
    public void refreshEnvironments(List<Environment> newEnvironments) {
        DefaultComboBoxModel<Environment> model = (DefaultComboBoxModel<Environment>) getModel();

        // 设置更新标志，避免触发监听器
        isUpdatingModel = true;

        model.removeAllElements();

        for (Environment env : newEnvironments) {
            model.addElement(env);
        }

        // 清除更新标志
        isUpdatingModel = false;
        // 重新设置当前选中的环境
        String currentId = ProjectSettingsManager.getCurrentEnvironmentId(project);
        if (currentId != null) {
            for (int i = 0; i < model.getSize(); i++) {
                Environment env = model.getElementAt(i);
                if (env.getId().equals(currentId)) {
                    setSelectedItem(env);
                    break;
                }
            }
        }
    }

    /**
     * 环境列表渲染器
     */
    private static class EnvironmentListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Environment) {
                Environment env = (Environment) value;

                // 检查是否是ComboBox的显示区域（index == -1）还是下拉列表
                if (index == -1) {
                    // ComboBox显示区域，只显示缩写
                    setText(""); // 清空文字
                    setIcon(new ColoredAbbreviationIcon(env.getAbbreviation(), getEnvironmentColor(env)));
                    setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                } else {
                    // 下拉列表，使用自定义Icon显示带边框的缩写
                    setText(" " + env.getName()); // 前面加空格，为Icon留位置
                    setIcon(new ColoredAbbreviationIcon(env.getAbbreviation(), getEnvironmentColor(env)));
                }

                // 设置背景色
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                } else {
                    setBackground(list.getBackground());
                }
            }

            return this;
        }

        /**
         * 根据环境名称获取对应的颜色
         */
        private Color getEnvironmentColor(Environment env) {
            String name = env.getName();

            // 使用哈希算法根据环境名称生成颜色
            int hashCode = name.hashCode();

            // 生成HSV颜色，确保颜色饱和度和亮度适中
            float hue = Math.abs(hashCode) % 360.0f / 360.0f; // 色相：0-1
            float saturation = 0.6f + (Math.abs(hashCode) % 40) / 100.0f; // 饱和度：0.6-1.0
            float value = 0.5f + (Math.abs(hashCode) % 30) / 100.0f; // 亮度：0.5-0.8

            Color color = Color.getHSBColor(hue, saturation, value);

            // 确保背景色足够深，与白色字体有足够的对比度
            // 计算颜色的亮度 (使用标准亮度公式)
            double brightness = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;

            // 如果亮度太高（接近白色），调整颜色使其更深
            if (brightness > 0.6) {
                // 将颜色调暗，确保与白色字体有足够对比度
                return new Color(
                        Math.max(0, (int) (color.getRed() * 0.6)),
                        Math.max(0, (int) (color.getGreen() * 0.6)),
                        Math.max(0, (int) (color.getBlue() * 0.6))
                );
            }

            return color;
        }
    }

    /**
     * 带边框的缩写Icon
     */
    private static class ColoredAbbreviationIcon implements Icon {
        private final String text;
        private final Color color;
        private final Font font;

        public ColoredAbbreviationIcon(String text, Color color) {
            this.text = text;
            this.color = color;
            this.font = new Font("Dialog", Font.BOLD, 12);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 设置字体
            g2d.setFont(font);

            // 获取文字尺寸
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();

            // 计算边框尺寸
            int padding = 4;
            int iconWidth = textWidth + padding * 2;
            int iconHeight = textHeight + padding * 2;
            int radius = 6; // 圆角半径

            // 绘制圆角背景
            g2d.setColor(color);
            g2d.fillRoundRect(x, y, iconWidth, iconHeight, radius, radius);

            // 绘制文字（白色）
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x + padding, y + padding + textHeight - fm.getDescent());

            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            FontMetrics fm = new Canvas().getFontMetrics(font);
            return fm.stringWidth(text) + 8; // 4px padding on each side
        }

        @Override
        public int getIconHeight() {
            FontMetrics fm = new Canvas().getFontMetrics(font);
            return fm.getAscent() + 8; // 4px padding on each side
        }
    }
}