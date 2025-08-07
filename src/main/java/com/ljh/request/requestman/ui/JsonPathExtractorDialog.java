package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.util.JsonPathExtractor;
import com.ljh.request.requestman.util.JsonPathExtractor.JsonPathField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author leijianhui
 * @Description JSONPath提取器对话框，支持从JSON中提取字段路径。
 * @date 2025/01/29 16:30
 */
public class JsonPathExtractorDialog extends JDialog {

    private final JTextArea jsonTextArea;
    private final JTextField jsonPathField;
    private final JTextArea resultArea;
    private final JTable fieldsTable;
    private final DefaultTableModel tableModel;


    private String selectedJsonPath = "";
    private boolean confirmed = false;

    public JsonPathExtractorDialog(Frame owner, String initialJson) {
        super(owner, "JSONPath 提取工具", true);

        // 初始化组件
        jsonTextArea = new JTextArea();
        jsonPathField = new JTextField();
        resultArea = new JTextArea();

        // 设置表格
        String[] columnNames = {"字段路径", "类型", "值"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fieldsTable = new JTable(tableModel);

        // 初始化界面
        initComponents();
        initLayout();
        initEvents();

        // 设置初始JSON
        if (initialJson != null && !initialJson.trim().isEmpty()) {
            jsonTextArea.setText(initialJson);
            parseJson();
        }

        // 设置对话框属性
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * 初始化组件
     */
    private void initComponents() {
        // JSON文本区域
        jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonTextArea.setLineWrap(true);
        jsonTextArea.setWrapStyleWord(true);

        // JSONPath输入框
        jsonPathField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // 结果区域
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        // 表格设置
        fieldsTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        fieldsTable.setRowHeight(20);
        fieldsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        setLayout(new BorderLayout(10, 10));

        // 主面板
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // 左侧面板 - JSON输入
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("JSON"));

        JScrollPane jsonScrollPane = new JScrollPane(jsonTextArea);
        jsonScrollPane.setPreferredSize(new Dimension(350, 200));
        leftPanel.add(jsonScrollPane, BorderLayout.CENTER);

        // 右侧面板 - JSONPath和结果
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        // JSONPath表达式面板
        JPanel jsonPathPanel = new JPanel(new BorderLayout(5, 5));
        jsonPathPanel.setBorder(BorderFactory.createTitledBorder("JSONPath 表达式"));

        JLabel descLabel = new JLabel("<html>可以在左侧任意一个字段上点击来快速填写<br>如: $.data.api_token[0].token</html>");
        descLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        jsonPathPanel.add(descLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(jsonPathField, BorderLayout.CENTER);

        inputPanel.add(jsonPathField, BorderLayout.CENTER);

        jsonPathPanel.add(inputPanel, BorderLayout.CENTER);

        // 提取结果面板
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("提取结果"));

        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setPreferredSize(new Dimension(350, 150));
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        rightPanel.add(jsonPathPanel, BorderLayout.NORTH);
        rightPanel.add(resultPanel, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        // 字段列表面板
        JPanel fieldsPanel = new JPanel(new BorderLayout(5, 5));
        fieldsPanel.setBorder(BorderFactory.createTitledBorder("字段列表"));

        JScrollPane tableScrollPane = new JScrollPane(fieldsTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 200));
        fieldsPanel.add(tableScrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // 组装界面
        add(mainPanel, BorderLayout.CENTER);
        add(fieldsPanel, BorderLayout.SOUTH);
        add(buttonPanel, BorderLayout.SOUTH);

        // 设置边距
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    /**
     * 初始化事件
     */
    private void initEvents() {
        // JSON文本变化时重新解析
        jsonTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                parseJson();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                parseJson();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                parseJson();
            }
        });

        // JSONPath输入变化时重新提取
        jsonPathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                extractValue();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                extractValue();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                extractValue();
            }
        });

        // 表格双击事件
        fieldsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fieldsTable.getSelectedRow();
                    if (row >= 0) {
                        String path = (String) tableModel.getValueAt(row, 0);
                        jsonPathField.setText(path);
                        selectedJsonPath = path;
                    }
                }
            }
        });

        // JSON文本区域鼠标事件 - 实现悬停和点击提取JSONPath
        jsonTextArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // 点击时提取当前行的JSONPath
                try {
                    // 获取点击位置对应的偏移量
                    int offset = jsonTextArea.viewToModel2D(e.getPoint());
                    int line = jsonTextArea.getLineOfOffset(offset);
                    String jsonPath = extractJsonPathFromLine(line);
                    if (jsonPath != null) {
                        jsonPathField.setText(jsonPath);
                        selectedJsonPath = jsonPath;
                    }
                } catch (Exception ex) {
                    // 忽略异常
                }
            }
        });

        // 确定按钮
        JButton confirmButton = findButton("确定");
        if (confirmButton != null) {
            confirmButton.addActionListener(e -> {
                selectedJsonPath = jsonPathField.getText();
                confirmed = true;
                dispose();
            });
        }

        // 取消按钮
        JButton cancelButton = findButton("取消");
        if (cancelButton != null) {
            cancelButton.addActionListener(e -> {
                selectedJsonPath = ""; // 重置选中的JSONPath
                confirmed = false;
                dispose();
            });
        }
    }

    /**
     * 查找按钮
     */
    private JButton findButton(String text) {
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof JButton && text.equals(((JButton) subComp).getText())) {
                        return (JButton) subComp;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 解析JSON并更新字段列表
     */
    private void parseJson() {
        String json = jsonTextArea.getText();
        if (json == null || json.trim().isEmpty()) {
            tableModel.setRowCount(0);
            return;
        }

        try {
            List<JsonPathField> fields = JsonPathExtractor.extractAllPaths(json);
            updateFieldsTable(fields);
        } catch (Exception e) {
            tableModel.setRowCount(0);
        }
    }

    /**
     * 更新字段表格
     */
    private void updateFieldsTable(List<JsonPathField> fields) {
        tableModel.setRowCount(0);
        for (JsonPathField field : fields) {
            tableModel.addRow(new Object[]{
                    field.getPath(),
                    field.getType(),
                    field.getDisplayValue()
            });
        }
    }

    /**
     * 提取值
     */
    private void extractValue() {
        String json = jsonTextArea.getText();
        String jsonPath = jsonPathField.getText();

        if (json == null || json.trim().isEmpty() || jsonPath == null || jsonPath.trim().isEmpty()) {
            resultArea.setText("不匹配");
            return;
        }

        try {
            String result = JsonPathExtractor.extractValue(json, jsonPath);
            if (result != null) {
                resultArea.setText(result);
            } else {
                resultArea.setText("不匹配");
            }
        } catch (Exception e) {
            resultArea.setText("提取失败: " + e.getMessage());
        }
    }

    /**
     * 根据行号提取JSONPath
     */
    private String extractJsonPathFromLine(int lineNumber) {
        try {
            String[] lines = jsonTextArea.getText().split("\n");
            if (lineNumber < 0 || lineNumber >= lines.length) {
                return null;
            }

            String line = lines[lineNumber].trim();
            if (line.isEmpty()) {
                return null;
            }

            // 解析当前行，提取字段名
            String fieldName = extractFieldNameFromLine(line);
            if (fieldName == null) {
                return null;
            }

            // 构建JSONPath
            return buildJsonPathForLine(lineNumber, fieldName, lines);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从行中提取字段名
     */
    private String extractFieldNameFromLine(String line) {
        // 匹配 "fieldName": value 或 "fieldName": "value" 格式
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 根据行号和字段名构建JSONPath
     */
    private String buildJsonPathForLine(int lineNumber, String fieldName, String[] lines) {
        StringBuilder path = new StringBuilder("$");
        Stack<String> pathStack = new Stack<>();
        // 新增：追踪当前是否在数组中，并维护每层数组的当前下标
        Stack<Integer> arrayIndexStack = new Stack<>();
        // 记录上一行是否是 key
        String lastKeyCandidate = null;
        for (int i = 0; i <= lineNumber; i++) {
            String line = lines[i].trim();

            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }

            // 【重点】遇到数组开始，压入 key 和初始下标0
            if ((line.equals("[") || line.endsWith("[")) && lastKeyCandidate != null) {
                pathStack.push(lastKeyCandidate);
                arrayIndexStack.push(0);
                lastKeyCandidate = null;
                continue;
            }

            // 如果是对象或数组的开始
            if ((line.endsWith("{") || line.endsWith("[")) && line.contains(":")) {
                String parent = extractFieldNameFromLine(line);
                if (parent != null) {
                    pathStack.push(parent);
                }
                // 如果是数组起始符号，加下标栈初始化
                if (line.endsWith("[")) {
                    arrayIndexStack.push(0);
                }
                continue;
            }

            // 如果是只有 key:，没有跟随 { 或 [，缓存 key 候选
            if (line.matches("\"[^\"]+\"\\s*:\\s*")) {
                lastKeyCandidate = extractFieldNameFromLine(line);
                continue;
            } else {
                lastKeyCandidate = null;
            }

            // 结束一个对象或数组
            if (line.startsWith("}") || line.startsWith("]")) {
                if (!pathStack.isEmpty()) {
                    String popKey = pathStack.pop();
                    // 如果弹出的 key 是数组 key，则对应弹出数组下标
                    if (!arrayIndexStack.isEmpty()) {
                        // 注意：弹出数组下标栈
                        if (line.startsWith("]")) {
                            arrayIndexStack.pop();
                        }
                    }
                }
                continue;
            }

            // 【重点】遇到数组里的元素（即左大括号），则数组下标自增，同时把 [index] 放进路径栈
            if (line.equals("{")) {
                if (!arrayIndexStack.isEmpty()) {
                    int currentIndex = arrayIndexStack.pop();
                    // 将当前索引压回，表示下次元素索引+1
                    arrayIndexStack.push(currentIndex + 1);
                    // 这个是数组元素的索引，要加到路径栈
                    pathStack.push("[" + currentIndex + "]");
                }
                continue;
            }

            // 当前是目标字段行，构造路径
            if (i == lineNumber) {
                for (String parent : pathStack) {
                    if (parent.matches("\\[\\d+]")) {
                        path.append(parent);
                    } else {
                        path.append(".").append(parent);
                    }
                }
                path.append(".").append(fieldName);
                break;
            }
        }

        return path.toString();
    }

    /**
     * 获取选中的JSONPath
     */
    public String getSelectedJsonPath() {
        return selectedJsonPath;
    }

    /**
     * 检查用户是否确认了选择
     */
    public boolean isConfirmed() {
        return confirmed;
    }
} 