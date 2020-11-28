package pers.pandora.server;

import pers.pandora.vo.Attachment;
import pers.pandora.handler.AIOServerlHandler;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.XMLFactory;

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

public final class AIOServer extends Server {

    private AsynchronousServerSocketChannel asyncServerSocketChannel;

    public static void main(String[] args) {
        new AIOServer().start();
    }

    @Override
    public void start() {
        int minCore = 2 * Runtime.getRuntime().availableProcessors();
        start(8080, minCore, minCore + 5, 50, TimeUnit.MILLISECONDS, 1, TimeUnit.SECONDS, 1000);
    }

    @Override
    public void start(int port, int minCore, int maxCore, long keepAlive, TimeUnit timeUnit, long timeOut, TimeUnit timeOutUnit, long exvitTime) {
        setPort(port);
        setRunning(true);
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(getPoolSize());
            AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            asyncServerSocketChannel = serverSocketChannel;
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.bind(new InetSocketAddress(getHOST(), port));
            Attachment att = new Attachment();
            long start = System.currentTimeMillis();
            //初始化web.xml
            setContext(new XMLFactory().parse(getWebConfigPath()));
            //初始化mvc模式下controller
            RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
            requestMappingHandler.init(minCore, maxCore, keepAlive, timeUnit, timeOut, timeOutUnit);
            setRequestMappingHandler(requestMappingHandler);
            att.setServer(this);
            serverSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
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
                    asyncServerSocketChannel.accept(att, this);
                    newAtt.setClient(client);
                    newAtt.setReadMode(true);
                    newAtt.setServer(att.getServer());
                    newAtt.setBuffer(ByteBuffer.allocateDirect(getCapcity()));
                    client.read(newAtt.getBuffer(), newAtt, new AIOServerlHandler());
                }

                @Override
                public void failed(Throwable t, Attachment att) {
                    System.out.println("accept failed");
                }
            });
            execEvitThread(exvitTime);
            System.out.println("AIO server Start! in time:" + (System.currentTimeMillis() - start) + " ms");
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        setRunning(false);
    }

}
