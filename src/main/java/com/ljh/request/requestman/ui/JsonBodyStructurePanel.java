package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * @author leijianhui
 * @Description JSON结构体参数面板，支持树形结构展示与编辑。
 * @date 2025/06/17 16:32
 */
public class JsonBodyStructurePanel extends JPanel {
    public JsonBodyStructurePanel(List<ApiParam> bodyParamList) {
        setLayout(new BorderLayout());
        // 构建根节点
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("根节点");
        if (bodyParamList != null) {
            for (ApiParam param : bodyParamList) {
                root.add(buildNode(param));
            }
        }
        // JTree
        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // 自定义渲染器，三列内容对齐
        tree.setCellRenderer(new FieldTreeCellRenderer());
        // 默认展开所有节点
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        // 横向滚动
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 递归构建树节点
     */
    private DefaultMutableTreeNode buildNode(ApiParam param) {
        String type = param.getRawType() != null && !param.getRawType().isEmpty() ? param.getRawType() : (param.getDataType() != null ? param.getDataType().name().toLowerCase() : "string");
        String desc = (param.getDescription() != null && !param.getDescription().isEmpty()) ? param.getDescription() : "--";
        // 字段名、类型、注释用空格对齐
        String label = String.format("%-30s %-20s %s", param.getName(), type, desc);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        if (param.getChildren() != null && !param.getChildren().isEmpty()) {
            List<ApiParam> children = param.getChildren();
            if ("array".equals(param.getDataType().name().toLowerCase()) && children.size() == 1) {
                // 如果是数组且children只有一条，跳过直接取子集
                children = children.getFirst().getChildren();
            }
            for (ApiParam child : children) {
                node.add(buildNode(child));
            }
        }
        return node;
    }

    /**
     * 自定义渲染器，设置字体等
     */
    static class FieldTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            return label;
        }
    }
} 