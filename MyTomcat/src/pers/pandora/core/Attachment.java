package pers.pandora.core;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Instant;

public class Attachment {

    private AsynchronousSocketChannel client;

    private Server server;

    private long waitReceivedTime;

    private volatile Instant keepTime;

    private ByteBuffer readBuffer;

    private ByteBuffer writeBuffer;

    private Instant createTime;

    private boolean isKeep;

    public boolean isKeep() {
        return isKeep;
    }

    public void setKeep(boolean keep) {
        isKeep = keep;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public void setWriteBuffer(ByteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    protected long getWaitReceivedTime() {
        return waitReceivedTime;
    }

    protected void setWaitReceivedTime(long waitReceivedTime) {
        this.waitReceivedTime = waitReceivedTime;
    }

    protected Server getServer() {
        return server;
    }

    protected void setServer(Server server) {
        this.server = server;
    }

    protected AsynchronousSocketChannel getClient() {
        return client;
    }

    protected void setClient(AsynchronousSocketChannel client) {
        this.client = client;
    }

    public Instant getKeepTime() {
        return keepTime;
    }

    public void setKeepTime(Instant keepTime) {
        this.keepTime = keepTime;
    }

    protected ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    protected void setReadBuffer(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }
}
