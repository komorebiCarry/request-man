package com.ljh.request.requestman.ui.builder;

import com.ljh.request.requestman.ui.*;
import com.ljh.request.requestman.util.RequestManBundle;

import javax.swing.*;
import java.awt.*;

/**
 * 扫描模式相关的请求行与参数Tab的无状态视图拼装器。
 * 仅负责把已经初始化好的控件进行布局与添加，不包含任何事件和业务逻辑。
 *
 * @author leijianhui
 * @Description 扫描模式视图拼装器，仅做UI拼装
 * @date 2025/07/27 10:55
 */
public final class RequestPanelsBuilder {

    private RequestPanelsBuilder() {
    }

    /**
     * 请求行控件载体。
     */
    public static final class RequestLineComponents {
        public JLabel methodLabel;
        public JTextField urlField;
        public JPanel splitSendPanel;
        public JButton scanSaveButton;
    }

    /**
     * 组装请求行面板。
     *
     * @param c 载体（控件已初始化/已绑事件）
     * @return 请求行面板
     */
    public static JPanel assembleRequestLine(RequestLineComponents c) {
        JPanel requestLine = new JPanel();
        requestLine.setLayout(new BoxLayout(requestLine, BoxLayout.X_AXIS));
        requestLine.add(c.methodLabel);
        requestLine.add(c.urlField);
        requestLine.add(c.splitSendPanel);
        requestLine.add(c.scanSaveButton);
        return requestLine;
    }

    /**
     * 参数Tab控件载体。
     */
    public static final class ParamTabsComponents {
        public ParamsTablePanel paramsPanel;
        public BodyPanel bodyPanel;
        public HeadersPanel headersPanel;
        public CookiesPanel cookiesPanel;
        public AuthPanel authPanel;
        public PreOpPanel preOpPanel;
        public PostOpPanel postOpPanel;
    }

    /**
     * 组装参数Tab。
     *
     * @param c 载体（各Tab面板已创建/业务已设置）
     * @return TabbedPane
     */
    public static JTabbedPane assembleParamTabs(ParamTabsComponents c) {
        JTabbedPane paramTab = new JTabbedPane();
        paramTab.addTab("Params", c.paramsPanel);
        paramTab.addTab("Body", c.bodyPanel);
        paramTab.addTab("Headers", c.headersPanel);
        paramTab.addTab("Cookies", c.cookiesPanel);
        paramTab.addTab("Auth", c.authPanel);
        paramTab.addTab(RequestManBundle.message("tab.preop"), c.preOpPanel);
        paramTab.addTab(RequestManBundle.message("tab.postop"), c.postOpPanel);
        return paramTab;
    }
}


