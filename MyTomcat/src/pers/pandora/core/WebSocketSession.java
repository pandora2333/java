package pers.pandora.core;

public class WebSocketSession extends Attachment{

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
