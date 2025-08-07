/*
 * Copyright (C) 2025-05-27. Xiamen C&D Information Technology co.,Ltd. All rights reserved.
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.See the License for the specific language governing permissions and limitations under the License.
 */
package com.ljh.request.requestman.ui;

import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI;
import com.ljh.request.requestman.model.Environment;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author ljh
 * @Description 自定义Darcula ComboBox UI，支持在弹出菜单中添加管理环境按钮
 * @date 2025/8/4 14:40
 */
public class MyDarculaComboBoxUI extends DarculaComboBoxUI {

    private JButton manageButton;

    @Override
    protected ComboPopup createPopup() {
        ComboPopup popup = super.createPopup();

        // 在弹出菜单显示时添加管理环境按钮
        SwingUtilities.invokeLater(() -> {
            addManageButtonToPopup(popup);
        });

        return popup;
    }

    /**
     * 向弹出菜单添加管理环境按钮
     */
    private void addManageButtonToPopup(ComboPopup popup) {
        try {
            // 获取弹出菜单的JList
            JList<?> list = popup.getList();
            if (list != null) {
                // 为JList添加工具提示，显示完整的环境名称
                list.setToolTipText(null); // 清除默认工具提示
                list.setToolTipText(""); // 启用工具提示功能

                // 添加鼠标监听器来动态设置工具提示
                list.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(java.awt.event.MouseEvent e) {
                        int index = list.locationToIndex(e.getPoint());
                        if (index >= 0 && index < list.getModel().getSize()) {
                            Object value = list.getModel().getElementAt(index);
                                    if (value instanceof Environment) {
            Environment env =
                    (Environment) value;
                                String tooltip = String.format("%s", env.getName());
                                list.setToolTipText(tooltip);
                            }
                        } else {
                            list.setToolTipText("");
                        }
                    }
                });

                // 获取JBViewport
                Container viewport = list.getParent();
                if (viewport instanceof com.intellij.ui.components.JBViewport) {
                    // 获取JScrollPane
                    Container scrollPane = viewport.getParent();
                    if (scrollPane instanceof JScrollPane) {

                        // 获取CustomComboPopup
                        Container customComboPopup = scrollPane.getParent();
                        if (customComboPopup instanceof CustomComboPopup) {
                            // 检查是否已经添加过按钮
                            for (Component comp : customComboPopup.getComponents()) {
                                if (comp == manageButton) {
                                    return; // 已经添加过了
                                }
                            }

                            // 创建管理环境按钮
                            manageButton = new JButton("⚙ 管理环境");
                            manageButton.setPreferredSize(new Dimension(150, 30));

                            // 设置按钮样式 - 移除边框和背景
                            manageButton.setBorderPainted(false);
                            manageButton.setContentAreaFilled(false);
                            manageButton.setOpaque(false);
                            manageButton.setFocusPainted(false);

                            // 设置字体和颜色
                            manageButton.setFont(manageButton.getFont().deriveFont(Font.PLAIN, 12));
                            manageButton.setForeground(new Color(120, 120, 120));

                            // 添加鼠标悬停效果
                            manageButton.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    manageButton.setForeground(new Color(80, 80, 80));
                                    manageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    manageButton.setForeground(new Color(120, 120, 120));
                                    manageButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                                }
                            });

                            // 创建包含按钮的面板 - 使用深色背景
                            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                            buttonPanel.setBackground(new Color(43, 43, 43)); // 更深的背景色，与下拉框背景一致
                            buttonPanel.setBorder(new EmptyBorder(2, 0, 2, 0)); // 最小上下边距
//                            buttonPanel.setPreferredSize(new Dimension(0, 32)); // 固定高度
                            buttonPanel.add(manageButton);

                            // 创建分隔线 - 使用深色
                            JSeparator separator = new JSeparator();
                            separator.setBackground(new Color(80, 80, 80)); // 深色分隔线
//                            separator.setPreferredSize(new Dimension(0, 1));

                            // 重新布局CustomComboPopup
                            customComboPopup.setLayout(new BorderLayout());
                            customComboPopup.removeAll();
                            customComboPopup.add(scrollPane, BorderLayout.CENTER);
                            customComboPopup.add(separator, BorderLayout.SOUTH);
                            customComboPopup.add(buttonPanel, BorderLayout.SOUTH);

                            // 重新验证和重绘
                            customComboPopup.revalidate();
                            customComboPopup.repaint();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果出现异常，忽略，不影响正常功能
        }
    }

    /**
     * 获取管理环境按钮
     */
    public JButton addManageEnvironmentButton() {
        return manageButton;
    }
}
