package pers.pandora.web.vo;

public abstract class WSData {

    protected boolean finish;

    protected long length;

    protected long remain;

    protected byte[] mask;

    protected byte[] data;

    protected int type = -1;

    public long getRemain() {
        return remain;
    }

    public void setRemain(long remain) {
        this.remain = remain;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getType() {
        return type;
    }

    public long getLength() {
        return length;
    }

    public byte[] getMask() {
        return mask;
    }

    public void setMask(byte[] mask) {
        this.mask = mask;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isFinish() {
        return finish;
    }
}
