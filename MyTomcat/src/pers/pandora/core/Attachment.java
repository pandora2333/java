package pers.pandora.core;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class Attachment {

    private AsynchronousSocketChannel client;

    private Server server;

    private long waitReceivedTime;

    private volatile boolean keepAlive;

    private ByteBuffer buffer;

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

    public boolean isKeepAlive() {
        return keepAlive;
    }

    protected void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    protected ByteBuffer getBuffer() {
        return buffer;
    }

    protected void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
