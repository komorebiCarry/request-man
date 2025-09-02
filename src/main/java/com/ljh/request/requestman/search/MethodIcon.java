package com.ljh.request.requestman.search;

import javax.swing.*;
import java.awt.*;

/**
 * HTTP方法图标渲染器，用于在API搜索列表中显示不同HTTP方法的彩色标签。
 * 支持GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS、TRACE等方法，
 * 每种方法使用不同的颜色进行区分，提升用户体验。
 *
 * @author leijianhui
 * @Description HTTP方法图标渲染器，用于在API搜索列表中显示不同HTTP方法的彩色标签。
 * @date 2025/06/19 21:10
 */
public class MethodIcon implements Icon {
    
    /**
     * HTTP方法名称
     */
    private final String method;
    
    /**
     * 图标背景颜色
     */
    private final Color color;

    /**
     * 构造函数
     *
     * @param method HTTP方法名称
     * @param color  图标背景颜色
     */
    public MethodIcon(String method, Color color) {
        this.method = method != null ? method.toUpperCase() : "";
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        int width = getIconWidth();
        int height = getIconHeight();
        // 保持原有的fillRoundRect绘制方法，不修改
        g2.fillRoundRect(x, y + 2, width, height - 4, 10, 10);
        g2.setColor(Color.WHITE);
        Font font = c.getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(font);
        FontMetrics fm = c.getFontMetrics(font);
        int strWidth = fm.stringWidth(method);
        int strX = x + (width - strWidth) / 2;
        int strY = y + height / 2 + fm.getAscent() / 2 - 2;
        g2.drawString(method, strX, strY);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return 52; // 统一宽度，保证所有方法块对齐
    }

    @Override
    public int getIconHeight() {
        return 20;
    }
    
    /**
     * 获取HTTP方法名称
     *
     * @return HTTP方法名称
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * 获取图标背景颜色
     *
     * @return 图标背景颜色
     */
    public Color getColor() {
        return color;
    }
}
