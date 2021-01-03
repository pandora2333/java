package pers.pandora.core;

import pers.pandora.constant.WS;
import pers.pandora.vo.WSData;

import java.util.ArrayList;
import java.util.List;

public final class WebSocketSession extends Attachment {

    private List<WSData> datas = new ArrayList<>(1);

    private String reqUrl;

    private boolean closeSignal;

    private byte[] outPutContent;

    private byte outPutType = WS.TYPE_TEXT;

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

    public List<WSData> getDatas() {
        return datas;
    }

    void setCloseSignal(boolean closeSignal) {
        this.closeSignal = closeSignal;
    }

    public boolean isCloseSignal() {
        return closeSignal;
    }

    void clear() {
        //modCount != expectedModCount
        for (int i = datas.size() - 1; i >= 0; i--) {
            if (datas.get(i).isFinish()) {
                datas.remove(i);
            }
        }
    }

}
