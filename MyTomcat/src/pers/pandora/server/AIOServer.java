package pers.pandora.server;

import pers.pandora.bean.Attachment;
import pers.pandora.handler.AIOServerlHandler;
import pers.pandora.mvc.RequestMappingHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;

public final class AIOServer {

    private boolean running;

    private int port;

    private String host = "127.0.0.1";

    private int poolSize = 10;

    private int capcity = 2048 * 1024;

    private AsynchronousServerSocketChannel asyncServerSocketChannel;

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setCapcity(int capcity) {
        this.capcity = capcity;
    }

    public int getCapcity() {
        return capcity;
    }

    public static void main(String[] args) {
        new AIOServer().start();
    }

    public void start() {
        start(8080);
    }

    public void start(int port) {
        this.port = port;
        running = true;
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);
            AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);
            asyncServerSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            asyncServerSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            asyncServerSocketChannel.bind(new InetSocketAddress(host, port));
            running = true;
            Attachment att = new Attachment();
            att.setServer(asyncServerSocketChannel);
            RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
            requestMappingHandler.init();
            att.setRequestMappingHandler(requestMappingHandler);
            asyncServerSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
                @Override
                public void completed(AsynchronousSocketChannel client, Attachment att) {
                    Attachment newAtt = new Attachment();
                    SocketAddress clientAddr = null;
                    try {
                        clientAddr = client.getRemoteAddress();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("收到新的连接：" + clientAddr);
                    att.getServer().accept(att, this);
                    newAtt.setServer(asyncServerSocketChannel);
                    newAtt.setClient(client);
                    newAtt.setReadMode(true);
                    newAtt.setBuffer(ByteBuffer.allocateDirect(capcity));
                    newAtt.setRequestMappingHandler(att.getRequestMappingHandler());
                    client.read(newAtt.getBuffer(), newAtt, new AIOServerlHandler());
                }

                @Override
                public void failed(Throwable t, Attachment att) {
                    System.out.println("accept failed");
                }
            });
            System.out.println("AIO server Start!");
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        running = false;
    }

}
