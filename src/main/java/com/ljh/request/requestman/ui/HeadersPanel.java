package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;
import com.ljh.request.requestman.ui.ParamsTablePanel;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leijianhui
 * @Description 请求头参数面板，使用统一的参数表格面板
 * @date 2025/01/27 10:30
 */
public class HeadersPanel extends JPanel {
    private static final Dimension PANEL_SIZE = new Dimension(600, 120);
    private final ParamsTablePanel paramsPanel;

    public HeadersPanel() {
        super(new BorderLayout());
        setPreferredSize(PANEL_SIZE);
        
        // 使用统一的参数表格面板，只支持字符串类型
        paramsPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.HEADERS, new ArrayList<>());
        add(paramsPanel, BorderLayout.CENTER);
    }

    /**
     * 获取所有参数
     */
    public List<ApiParam> getParams() {
        return paramsPanel.getParams();
    }

    /**
     * 设置参数数据（用于回显）
     */
    public void setParams(List<ApiParam> paramList) {
        paramsPanel.setParams(paramList);
    }

    /**
     * 获取表格引用
     */
    public JTable getTable() {
        return paramsPanel.getTable();
    }

    /**
     * 设置请求头数据（兼容性方法）
     */
    public void setHeadersData(List<HeaderItem> headerItems) {
        if (headerItems != null) {
            List<ApiParam> apiParams = new ArrayList<>();
            for (HeaderItem item : headerItems) {
                ApiParam param = new ApiParam();
                param.setName(item.getName());
                param.setValue(item.getValue());
                param.setType(item.getType());
                param.setDescription(item.getDescription());
                apiParams.add(param);
            }
            setParams(apiParams);
        }
    }

    /**
     * 获取请求头数据（兼容性方法）
     */
    public List<HeaderItem> getHeadersData() {
        List<ApiParam> apiParams = getParams();
        List<HeaderItem> headerItems = new ArrayList<>();
        for (ApiParam param : apiParams) {
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                HeaderItem item = new HeaderItem();
                item.setName(param.getName());
                item.setValue(param.getValue());
                item.setType(param.getType());
                item.setDescription(param.getDescription());
                headerItems.add(item);
            }
        }
        return headerItems;
    }

    /**
     * 获取请求头Map（兼容性方法）
     */
    public Map<String, String> getHeadersMap() {
        List<ApiParam> apiParams = getParams();
        Map<String, String> headersMap = new HashMap<>();
        for (ApiParam param : apiParams) {
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                headersMap.put(param.getName(), param.getValue());
            }
        }
        return headersMap;
    }

    /**
     * 请求头项
     */
    public static class HeaderItem  implements Serializable {

        private static final long serialVersionUID = 2473960574224179533L;
        private String name;
        private String value;
        private String type;
        private String description;

        public HeaderItem() {}

        public HeaderItem(String name, String value, String type, String description) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
