package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.util.LogUtil;
import com.ljh.request.requestman.util.RequestManBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;

/**
 * JsonBodyStructurePanel 负责以树形结构展示 JSON 响应/请求的数据结构。
 * 支持对递归字段的占位渲染与按需动态展开，避免一次性构建无限深度的节点树。
 *
 * @author leijianhui
 * @Description JSON 结构体参数面板，支持递归字段的按需展开与结构展示。
 * @date 2025/06/19 10:30
 */
public class JsonBodyStructurePanel extends JPanel {
    /**
     * 是否处于自动展开阶段。用于避免程序化的 expand 触发动态生成逻辑。
     */
    private boolean isAutoExpanding = false;

    /**
     * 树组件引用。
     */
    private JTree tree;

    /**
     * 树展开监听器实例引用，便于在 cleanup 时移除。
     */
    private TreeWillExpandListener treeWillExpandListener;

    /**
     * 占位节点的显示文本。
     */
    private static final String PLACEHOLDER_LABEL = "placeholder";

    /**
     * 占位节点的类型显示。
     */
    private static final String PLACEHOLDER_TYPE = "object";

    /**
     * 占位节点的描述信息。
     */
    private static final String PLACEHOLDER_DESC = RequestManBundle.message("json.structure.placeholder.desc");

    /**
     * 字段显示标签的格式：字段名、类型、描述 三列对齐。
     */
    private static final String LABEL_FORMAT = "%-30s %-20s %s";

    /**
     * 根节点标题。
     */
    private static final String ROOT_TITLE = "根节点";

    public JsonBodyStructurePanel(List<ApiParam> bodyParamList) {
        setLayout(new BorderLayout());
        initTree(bodyParamList);
        installRenderers();
        installListeners();
        expandNonRecursiveNodes(tree);

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 初始化树与根节点。
     *
     * @param bodyParamList 根参数列表
     */
    private void initTree(List<ApiParam> bodyParamList) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_TITLE);
        if (bodyParamList != null) {
            for (ApiParam param : bodyParamList) {
                if (param == null) {
                    continue;
                }
                root.add(buildNode(param));
            }
        }
        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
    }

    /**
     * 安装渲染器。
     */
    private void installRenderers() {
        tree.setCellRenderer(new FieldTreeCellRenderer());
    }

    /**
     * 安装展开监听器。
     */
    private void installListeners() {
        treeWillExpandListener = new TreeExpandHandler();
        tree.addTreeWillExpandListener(treeWillExpandListener);
    }

    /**
     * 清理资源，避免内存泄漏。
     */
    public void cleanup() {
        try {
            if (tree != null && treeWillExpandListener != null) {
                tree.removeTreeWillExpandListener(treeWillExpandListener);
            }
            if (tree != null) {
                // 直接断开模型引用，使整棵树可被 GC
                tree.setModel(null);
            }
            treeWillExpandListener = null;
            tree = null;
            isAutoExpanding = false;
        } catch (Exception e) {
            LogUtil.error("清理JsonBodyStructurePanel时发生异常: " + e.getMessage());
        }
    }

    /**
     * 静态方法：清理指定容器中的所有 JsonBodyStructurePanel 实例。
     * 通过 BFS 广度遍历查找承载 panel 的容器并调用 cleanup。
     *
     * @param container 根容器
     */
    public static void cleanupAllInContainer(Container container) {
        if (container == null) {
            return;
        }
        try {
            cleanupStructurePanelsBFS(container);
        } catch (Exception e) {
            LogUtil.error("批量清理JsonBodyStructurePanel时发生异常: " + e.getMessage());
        }
    }

    /**
     * 广度优先搜索清理，避免递归过深。
     */
    private static void cleanupStructurePanelsBFS(Container rootContainer) {
        if (rootContainer == null) {
            return;
        }
        Queue<Container> queue = new LinkedList<>();
        Set<Container> visited = new HashSet<>();
        queue.offer(rootContainer);
        visited.add(rootContainer);
        while (!queue.isEmpty()) {
            Container container = queue.poll();
            if (container instanceof JPanel) {
                cleanupStructurePanelInPanel((JPanel) container);
            }
            Component[] components = container.getComponents();
            for (Component component : components) {
                if (component instanceof Container && !visited.contains(component)) {
                    queue.offer((Container) component);
                    visited.add((Container) component);
                }
            }
        }
    }

    /**
     * 清理单个 JPanel 中的 JsonBodyStructurePanel。
     */
    private static void cleanupStructurePanelInPanel(JPanel panel) {
        try {
            Object structurePanel = panel.getClientProperty("structurePanel");
            if (structurePanel instanceof JsonBodyStructurePanel) {
                ((JsonBodyStructurePanel) structurePanel).cleanup();
                panel.putClientProperty("structurePanel", null);
            }
        } catch (Exception e) {
            LogUtil.error("清理单个JsonBodyStructurePanel时发生异常: " + e.getMessage());
        }
    }

    /**
     * 构建一个参数对应的树节点。
     *
     * @param param 参数
     * @return 树节点
     */
    private DefaultMutableTreeNode buildNode(ApiParam param) {
        if (param == null) {
            return new DefaultMutableTreeNode();
        }
        DefaultMutableTreeNode node = getDefaultMutableTreeNode(param);

        if (param.getChildren() != null && !param.getChildren().isEmpty()) {
            List<ApiParam> children = param.getChildren();
            if ("array".equalsIgnoreCase(param.getDataType().name()) && children.size() == 1) {
                // 如果是数组且 children 只有一条，跳过一层直接取子集
                children = children.get(0).getChildren();
            }
            for (ApiParam child : children) {
                node.add(buildNode(child));
            }
            return node;
        }

        addPlaceholderIfNeeded(node, param);
        return node;
    }

    private static @NotNull DefaultMutableTreeNode getDefaultMutableTreeNode(ApiParam param) {
        String type = (param.getRawType() != null && !param.getRawType().isEmpty())
                ? param.getRawType()
                : (param.getDataType() != null ? param.getDataType().name().toLowerCase() : "string");
        String desc = (param.getDescription() != null && !param.getDescription().isEmpty()) ? param.getDescription() : "--";

        String label = String.format(LABEL_FORMAT, param.getName(), type, desc);
        return new DefaultMutableTreeNode(new NodeData(label, param));
    }

    /**
     * 当为递归字段且没有子节点时，添加占位子节点以展示展开箭头。
     */
    private void addPlaceholderIfNeeded(DefaultMutableTreeNode node, ApiParam param) {
        if (param == null || !param.isRecursive()) {
            return;
        }
        DefaultMutableTreeNode placeholderNode = new DefaultMutableTreeNode(
                new NodeData(String.format(LABEL_FORMAT, PLACEHOLDER_LABEL, PLACEHOLDER_TYPE, PLACEHOLDER_DESC), null)
        );
        node.add(placeholderNode);
    }

    /**
     * 判断是否为占位节点。
     */
    private boolean isPlaceholderNode(DefaultMutableTreeNode node) {
        if (node == null) {
            return false;
        }
        Object userObject = node.getUserObject();
        if (!(userObject instanceof NodeData nd)) {
            return false;
        }
        return nd.getApiParam() == null;
    }

    /**
     * 创建基本的字段节点结构：优先复制现有结构，无法复制时创建通用结构。
     */
    private void createBasicFieldNodes(DefaultMutableTreeNode parentNode, ApiParam parentParam) {
        if (parentParam == null) {
            createGenericObjectFields(parentNode);
            return;
        }
        if (parentParam.getChildren() != null && !parentParam.getChildren().isEmpty()) {
            copyExistingFieldStructure(parentNode, parentParam, parentParam.getChildren());
            return;
        }
        createGenericObjectFields(parentNode);
    }

    /**
     * 复制现有的字段结构，并回填到父节点的 ApiParam.children 中。
     */
    private void copyExistingFieldStructure(DefaultMutableTreeNode parentNode, ApiParam parentParam, List<ApiParam> existingFields) {
        List<ApiParam> clonedChildren = new ArrayList<>();
        for (ApiParam existingField : existingFields) {
            if (existingField == null) {
                continue;
            }
            ApiParam newField = new ApiParam();
            newField.setName(existingField.getName());
            newField.setType(existingField.getType());
            newField.setDescription(existingField.getDescription());
            newField.setDataType(existingField.getDataType());
            newField.setRawType(existingField.getRawType());
            newField.setRawCanonicalType(existingField.getRawCanonicalType());
            newField.setValue(existingField.getValue());
            newField.setContentType(existingField.getContentType());
            newField.setRecursive(existingField.isRecursive());

            if (parentParam.getChildren() != null && newField.isRecursive()) {
                newField.setChildren(parentParam.getChildren());
            }

            String fieldLabel = String.format(
                    LABEL_FORMAT,
                    newField.getName(),
                    (newField.getRawType() != null ? newField.getRawType() : PLACEHOLDER_TYPE),
                    (newField.getDescription() != null ? newField.getDescription() : "--")
            );
            DefaultMutableTreeNode fieldNode = new DefaultMutableTreeNode(new NodeData(fieldLabel, newField));

            if (newField.isRecursive()) {
                DefaultMutableTreeNode placeholderNode = new DefaultMutableTreeNode(
                        new NodeData(String.format(LABEL_FORMAT, PLACEHOLDER_LABEL, PLACEHOLDER_TYPE, PLACEHOLDER_DESC), null)
                );
                fieldNode.add(placeholderNode);
            }

            parentNode.add(fieldNode);
            clonedChildren.add(newField);
        }

        Object userObject = parentNode.getUserObject();
        if (userObject instanceof NodeData nodeData) {
            ApiParam apiParam = nodeData.getApiParam();
            if (apiParam != null) {
                apiParam.setChildren(clonedChildren);
            }
        }
    }

    /**
     * 创建通用的对象字段结构（兜底逻辑）。
     */
    private void createGenericObjectFields(DefaultMutableTreeNode parentNode) {
        String[] basicFields = {"id", "name", "description"};
        String[] fieldTypes = {"String", "String", "String"};
        String[] fieldDescs = {"标识", "名称", "描述"};
        for (int i = 0; i < basicFields.length; i++) {
            ApiParam fieldParam = new ApiParam();
            fieldParam.setName(basicFields[i]);
            fieldParam.setDataType(com.ljh.request.requestman.enums.ParamDataType.STRING);
            fieldParam.setRawType(fieldTypes[i]);
            fieldParam.setDescription(fieldDescs[i]);

            String fieldLabel = String.format(LABEL_FORMAT, basicFields[i], fieldTypes[i], fieldDescs[i]);
            DefaultMutableTreeNode fieldNode = new DefaultMutableTreeNode(new NodeData(fieldLabel, fieldParam));
            parentNode.add(fieldNode);
        }
    }

    /**
     * 获取上一级节点的 ApiParam 对象。
     */
    private ApiParam getParentApiParam(DefaultMutableTreeNode currentNode) {
        if (currentNode == null) {
            return null;
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) currentNode.getParent();
        if (parentNode == null) {
            return null;
        }
        Object parentUserObject = parentNode.getUserObject();
        if (!(parentUserObject instanceof NodeData parentNodeData)) {
            return null;
        }
        return parentNodeData.getApiParam();
    }

    /**
     * 递归展开非递归字段的节点，跳过递归字段。
     */
    private void expandNonRecursiveNodes(JTree tree) {
        isAutoExpanding = true;
        try {
            for (int i = 0; i < tree.getRowCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(i).getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof NodeData nodeData) {
                    ApiParam param = nodeData.getApiParam();
                    if (param != null && param.isRecursive()) {
                        continue;
                    }
                }
                tree.expandRow(i);
            }
        } finally {
            isAutoExpanding = false;
        }
    }

    /**
     * 树节点渲染器：渲染 NodeData 的 label 文本。
     */
    private static class FieldTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof NodeData nodeData) {
                    label.setText(nodeData.getLabel());
                }
            }
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            return label;
        }
    }

    /**
     * 树节点用户数据：承载展示 label 与 ApiParam。
     */
    private static class NodeData {
        private final String label;
        private final ApiParam apiParam;

        NodeData(String label, ApiParam apiParam) {
            this.label = label;
            this.apiParam = apiParam;
        }

        String getLabel() {
            return label;
        }

        ApiParam getApiParam() {
            return apiParam;
        }
    }

    /**
     * 树展开监听器实现，负责在用户手动展开递归节点时动态生成虚拟子节点。
     */
    private class TreeExpandHandler implements TreeWillExpandListener {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            handleTreeWillExpand(event);
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            // 不处理折叠
        }
    }

    /**
     * 展开事件处理：若为递归节点且仅有占位子节点，则移除占位并按需构建真实结构。
     */
    private void handleTreeWillExpand(TreeExpansionEvent event) {
        if (isAutoExpanding) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof NodeData nodeData)) {
            return;
        }
        ApiParam param = nodeData.getApiParam();
        if (param == null || !param.isRecursive() || node.getChildCount() != 1) {
            return;
        }
        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getChildAt(0);
        if (!isPlaceholderNode(firstChild)) {
            return;
        }
        node.remove(0);
        ApiParam parentParam = getParentApiParam(node);
        if (parentParam != null) {
            createBasicFieldNodes(node, parentParam);
            return;
        }
        createBasicFieldNodes(node, param);
    }
} 