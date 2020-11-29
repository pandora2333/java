package pers.pandora.core;

import pers.pandora.constant.LOG;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.XMLFactory;
import pers.pandora.vo.Attachment;

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
        start(8080, getCapcity(), minCore, minCore + 5, 50, TimeUnit.MILLISECONDS,
                5, TimeUnit.SECONDS, 1000);
    }

    @Override
    public void start(int port, int capacity, int minCore, int maxCore, long keepAlive, TimeUnit timeUnit,
                      long timeOut, TimeUnit timeOutUnit, long expeltTime) {
        setPort(port);
        setRunning(true);
        setCapcity(capacity);
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
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "capacity:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "minCore:" + LOG.LOG_PRE + LOG.VERTICAL + "maxCore:" + LOG.LOG_PRE + LOG.VERTICAL + "keepAlive:"
                            + LOG.LOG_PRE + LOG.VERTICAL + "timeOut:" + LOG.LOG_PRE + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "]",
                    this.getClass().getName(), port, capacity, minCore, maxCore, keepAlive, timeOut, expeltTime);
            serverSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
                @Override
                public void completed(AsynchronousSocketChannel client, Attachment att) {
                    Attachment newAtt = new Attachment();
                    SocketAddress clientAddr = null;
                    try {
                        clientAddr = client.getRemoteAddress();
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "accept=>completed" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
                    }

                    logger.info("A new Connection:" + LOG.LOG_PRE, clientAddr);
                    asyncServerSocketChannel.accept(att, this);
                    newAtt.setClient(client);
                    newAtt.setReadMode(true);
                    newAtt.setServer(att.getServer());
                    newAtt.setBuffer(ByteBuffer.allocateDirect(getCapcity()));
                    client.read(newAtt.getBuffer(), newAtt, new AIOServerlDispatcher());
                }

                @Override
                public void failed(Throwable t, Attachment att) {
                    try {
                        logger.error(LOG.LOG_PRE + "accept" + LOG.LOG_POS, att.getClient().getRemoteAddress(), LOG.EXCEPTION_DESC, t);
                    } catch (IOException e) {
                        logger.error("Not Get Client Remote IP:" + LOG.LOG_PRE, t);
                    }
                }
            });
            execExpelThread(expeltTime);
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", this.getClass().getName(), (System.currentTimeMillis() - start));
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
        }
        setRunning(false);
    }

}
