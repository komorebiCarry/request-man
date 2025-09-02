package com.ljh.request.requestman.search;

import com.intellij.ide.util.PropertiesComponent;

import javax.swing.*;
import java.awt.*;

/**
 * 字体管理器，负责RequestMan内与搜索列表相关的字体获取与刷新。
 * 统一管理字体缓存、默认字号策略与变更通知，避免分散在各处造成维护复杂。
 *
 * @author leijianhui
 * @Description 统一管理搜索列表的字体获取与刷新。
 * @date 2025/06/19 21:30
 */
public final class FontManager {

    /**
     * 当前列表字体缓存
     */
    private static volatile Font currentListFont = getIdeFont();

    private FontManager() {
    }

    /**
     * 刷新字体缓存，并通知变更。
     */
    public static void refreshFont() {
        currentListFont = getIdeFont();
        notifyFontChanged();
    }

    /**
     * 获取当前缓存的列表字体。
     *
     * @return 列表字体
     */
    public static Font getCurrentFont() {
        return currentListFont;
    }

    /**
     * 获取默认字体大小（优先取IDE编辑器或标签字号，并增加偏移）。
     *
     * @return 默认字体大小
     */
    public static int getDefaultFontSize() {
        Font editorFont = UIManager.getFont("EditorPane.font");
        if (editorFont != null) {
            return editorFont.getSize() + 6;
        }
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.getSize() + 6;
        }
        return 18;
    }

    /**
     * 基于IDE字体与用户配置计算列表字体。
     *
     * @return 计算后的字体
     */
    public static Font getIdeFont() {
        int fontSize = getDefaultFontSize();
        try {
            String fontSizeStr = PropertiesComponent.getInstance()
                    .getValue("requestman.searchFontSize", String.valueOf(fontSize));
            fontSize = Integer.parseInt(fontSizeStr);
        } catch (Exception e) {
            // 使用默认值
        }

        Font editorFont = UIManager.getFont("EditorPane.font");
        if (editorFont != null) {
            return editorFont.deriveFont((float) fontSize);
        }
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.deriveFont((float) fontSize);
        }
        return new Font("Dialog", Font.PLAIN, fontSize);
    }

    /**
     * 字体变更通知（占位，便于后续扩展事件机制）。
     */
    private static void notifyFontChanged() {
        // 目前不做主动广播；需要刷新界面时由调用方重新设置字体或重建组件
    }
}
