package com.ljh.request.requestman.ui;

import cn.hutool.json.JSONUtil;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.CustomApiInfo;
import com.ljh.request.requestman.util.CustomApiStorage;
import com.ljh.request.requestman.util.LogUtil;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义API右键菜单管理器，负责管理自定义API列表的右键菜单操作。
 * 包括复制、导入、导出、删除等功能的右键菜单实现。
 *
 * @author leijianhui
 * @Description 自定义API右键菜单管理器，负责管理自定义API列表的右键菜单操作。
 * @date 2025/01/27 10:00
 */
public class CustomApiContextMenuManager {
    /**
     * 设置自定义接口列表的右键菜单
     */
    public static void setupCustomApiListContextMenu(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        JPopupMenu contextMenu = new JPopupMenu();

        // 复制菜单项（追加到末尾，名称拼接 Copy）
        JMenuItem copyMenuItem = new JMenuItem(RequestManBundle.message("contextmenu.copy"));
        copyMenuItem.addActionListener(e -> copySelectedCustomApis(requestManPanel, project));
        contextMenu.add(copyMenuItem);

        // 导入菜单项
        JMenuItem importMenuItem = new JMenuItem(RequestManBundle.message("contextmenu.import"));
        importMenuItem.addActionListener(e -> importCustomApis(requestManPanel, project));
        contextMenu.add(importMenuItem);

        // 添加分隔线
        contextMenu.addSeparator();

        // 导出菜单项
        JMenuItem exportMenuItem = new JMenuItem(RequestManBundle.message("contextmenu.export"));
        exportMenuItem.addActionListener(e -> exportSelectedCustomApis(requestManPanel, project));
        contextMenu.add(exportMenuItem);

        // 删除菜单项
        JMenuItem deleteMenuItem = new JMenuItem(RequestManBundle.message("contextmenu.delete"));
        deleteMenuItem.addActionListener(e -> deleteSelectedCustomApi(requestManPanel, project));
        contextMenu.add(deleteMenuItem);

        // 添加分隔线
        contextMenu.addSeparator();

        // 拖拽排序提示
        JMenuItem dragSortMenuItem = new JMenuItem(RequestManBundle.message("contextmenu.drag.sort"));
        dragSortMenuItem.setEnabled(false); // 设置为不可点击，仅作为提示
        contextMenu.add(dragSortMenuItem);

        // 添加右键菜单监听器
        customApiList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            private void showContextMenu(java.awt.event.MouseEvent e) {
                int index = customApiList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    // 如果点击的项不在选中范围内，则选中该项
                    if (!customApiList.isSelectedIndex(index)) {
                        customApiList.setSelectedIndex(index);
                    }
                }
                contextMenu.show(customApiList, e.getX(), e.getY());
            }
        });
    }

    /**
     * 复制选中的自定义接口到列表末尾，名称拼接 " Copy"
     */
    private static void copySelectedCustomApis(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        DefaultListModel<CustomApiInfo> customApiListModel = requestManPanel.getCustomApiListModel();
        int[] selectedIndices = customApiList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(requestManPanel, RequestManBundle.message("contextmenu.copy.select.first"), RequestManBundle.message("main.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<CustomApiInfo> copies = new ArrayList<>();
        for (int index : selectedIndices) {
            try {
                CustomApiInfo src = customApiListModel.getElementAt(index);
                String json = JSONUtil.toJsonStr(src);
                CustomApiInfo dup = JSONUtil.toBean(json, CustomApiInfo.class);
                String name = src.getName();
                if (name == null) {
                    name = "";
                }
                dup.setName(name + " Copy");
                copies.add(dup);
            } catch (Exception ex) {
                LogUtil.warn("复制自定义接口失败: " + ex.getMessage());
            }
        }

        if (copies.isEmpty()) {
            return;
        }

        int startIndex = customApiListModel.getSize();
        for (CustomApiInfo dup : copies) {
            customApiListModel.addElement(dup);
        }

        // 持久化并选中新追加的项
        CustomApiStorage.persistCustomApiList(project, customApiListModel);
        int[] newSelection = new int[copies.size()];
        for (int i = 0; i < copies.size(); i++) {
            newSelection[i] = startIndex + i;
        }
        customApiList.setSelectedIndices(newSelection);
        if (newSelection.length > 0) {
            customApiList.ensureIndexIsVisible(newSelection[newSelection.length - 1]);
        }
    }

    /**
     * 导入自定义接口
     */
    private static void importCustomApis(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        DefaultListModel<CustomApiInfo> customApiListModel = requestManPanel.getCustomApiListModel();
        // 获取当前所有接口列表用于导出时的参考
        List<CustomApiInfo> currentApis = new ArrayList<>();
        for (int i = 0; i < customApiListModel.getSize(); i++) {
            currentApis.add(customApiListModel.getElementAt(i));
        }

        ImportExportDialog dialog = new ImportExportDialog(project, true, currentApis, customApiListModel);
        if (dialog.showAndGet()) {
            // 导入成功后刷新界面
            customApiList.repaint();
            CustomApiStorage.persistCustomApiList(project, customApiListModel);
        }
    }

    /**
     * 导出选中的自定义接口
     */
    private static void exportSelectedCustomApis(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        DefaultListModel<CustomApiInfo> customApiListModel = requestManPanel.getCustomApiListModel();
        int[] selectedIndices = customApiList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(requestManPanel, RequestManBundle.message("contextmenu.export.select.first"), RequestManBundle.message("main.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<CustomApiInfo> selectedApis = new ArrayList<>();
        for (int index : selectedIndices) {
            selectedApis.add(customApiListModel.getElementAt(index));
        }

        ImportExportDialog dialog = new ImportExportDialog(project, false, selectedApis, customApiListModel);
        dialog.show();
    }

    /**
     * 删除选中的自定义接口
     */
    private static void deleteSelectedCustomApis(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        DefaultListModel<CustomApiInfo> customApiListModel = requestManPanel.getCustomApiListModel();
        int[] selectedIndices = customApiList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(requestManPanel, RequestManBundle.message("contextmenu.delete.select.first"), RequestManBundle.message("main.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        String message = selectedIndices.length == 1 ?
                RequestManBundle.message("contextmenu.delete.confirm.single") :
                RequestManBundle.message("contextmenu.delete.confirm.multiple", selectedIndices.length);

        int confirm = JOptionPane.showConfirmDialog(requestManPanel, message, RequestManBundle.message("common.confirm"), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // 从后往前删除，避免索引变化
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                customApiListModel.remove(selectedIndices[i]);
            }
            requestManPanel.showCustomApiDetail(null);
            CustomApiStorage.persistCustomApiList(project, customApiListModel);
        }
    }

    /**
     * 删除选中的自定义接口（单个删除，保留兼容性）
     */
    public static void deleteSelectedCustomApi(RequestManPanel requestManPanel, Project project) {
        JList<CustomApiInfo> customApiList = requestManPanel.getCustomApiList();
        DefaultListModel<CustomApiInfo> customApiListModel = requestManPanel.getCustomApiListModel();
        int idx = customApiList.getSelectedIndex();
        if (idx >= 0) {
            int confirm = JOptionPane.showConfirmDialog(requestManPanel, RequestManBundle.message("contextmenu.delete.confirm.single"), RequestManBundle.message("common.confirm"), JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                customApiListModel.remove(idx);
                requestManPanel.showCustomApiDetail(null);
                CustomApiStorage.persistCustomApiList(project, customApiListModel);
            }
        }
    }
}
