package pers.pandora.web.core;

import pers.pandora.web.constant.WS;
import pers.pandora.web.vo.WSData;

public final class WebSocketSession extends Attachment {

    private WSData data;

    private String reqUrl;

    private boolean closeSignal;

    private byte[] outPutContent;

    private byte outPutType = WS.TYPE_NULL;

    public byte[] getOutPutContent() {
        return outPutContent;
    }

    public void setOutPutType(byte outPutType) {
        this.outPutType = outPutType;
    }

    public byte getOutPutType() {
        return outPutType;
    }

    void setReqUrl(String reqUrl) {
        this.reqUrl = reqUrl;
    }

    public String getReqUrl() {
        return reqUrl;
    }

    public void writeContent(byte[] outPutContent) {
        this.outPutContent = outPutContent;
    }

    public WSData getData() {
        return data;
    }

    void setCloseSignal(boolean closeSignal) {
        this.closeSignal = closeSignal;
    }

    public boolean isCloseSignal() {
        return closeSignal;
    }

    public void setData(WSData data) {
        this.data = data;
    }
}
