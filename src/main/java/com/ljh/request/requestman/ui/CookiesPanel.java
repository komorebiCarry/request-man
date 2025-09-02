package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.ApiParam;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leijianhui
 * @Description Cookie参数面板，使用统一的参数表格面板
 * @date 2025/01/27 10:30
 */
public class CookiesPanel extends JPanel {
    private static final Dimension PANEL_SIZE = new Dimension(600, 120);
    private final ParamsTablePanel paramsPanel;

    public CookiesPanel() {
        super(new BorderLayout());
        setPreferredSize(PANEL_SIZE);
        
        // 使用统一的参数表格面板，只支持字符串类型
        paramsPanel = new ParamsTablePanel(ParamsTablePanel.ParamUsage.COOKIES, new ArrayList<>());
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
     * 设置Cookie数据（兼容性方法）
     */
    public void setCookiesData(List<CookieItem> cookieItems) {
        if (cookieItems != null) {
            List<ApiParam> apiParams = new ArrayList<>();
            for (CookieItem item : cookieItems) {
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
     * 获取Cookie数据（兼容性方法）
     */
    public List<CookieItem> getCookiesData() {
        List<ApiParam> apiParams = getParams();
        List<CookieItem> cookieItems = new ArrayList<>();
        for (ApiParam param : apiParams) {
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                CookieItem item = new CookieItem();
                item.setName(param.getName());
                item.setValue(param.getValue());
                item.setType(param.getType());
                item.setDescription(param.getDescription());
                cookieItems.add(item);
            }
        }
        return cookieItems;
    }

    /**
     * 获取Cookie Map（兼容性方法）
     */
    public Map<String, String> getCookiesMap() {
        List<ApiParam> apiParams = getParams();
        Map<String, String> cookiesMap = new HashMap<>();
        for (ApiParam param : apiParams) {
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                cookiesMap.put(param.getName(), param.getValue());
            }
        }
        return cookiesMap;
    }

    /**
     * Cookie项
     */
    public static class CookieItem  implements Serializable {

        private static final long serialVersionUID = 4119823243745231883L;
        private String name;
        private String value;
        private String type;
        private String description;

        public CookieItem() {}

        public CookieItem(String name, String value, String type, String description) {
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
