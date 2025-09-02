
package com.ljh.request.requestman.ui;

import com.ljh.request.requestman.model.CustomApiInfo;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * @author ljh
 * @Description 自定义接口传输对象，用于拖拽操作
 * @date 2025/8/19 11:11
 */
public class CustomApiTransferable  implements Transferable {
    public static final DataFlavor CUSTOM_API_FLAVOR =
            new DataFlavor(CustomApiInfo.class, "CustomApiInfo");

    private final CustomApiInfo api;

    public CustomApiTransferable(CustomApiInfo api) {
        this.api = api;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{CUSTOM_API_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return CUSTOM_API_FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (CUSTOM_API_FLAVOR.equals(flavor)) {
            return api;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
