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
        //using simple way
        //init mvc controller
        RequestMappingHandler.init();
        AIOServer server = new AIOServer();
        //set session serializer and deserializer
        server.setServerName("pandora_test_1");
        server.setSerialSessionSupport(new SerialSessionSupportSimpler());
        SerialSessionSupport.getSessionPool().put(server.getServerName(), server.getSessionMap());
        //start server
        server.start();
    }

    @Override
    public void start() {
        start(8080, getCapcity(), 1000);
    }

    @Override
    public void start(int port, int capacity, long expeltTime) {
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
            att.setServer(this);
            //load SESSION.ser if set before the start method is running
            if (getSerialSessionSupport() != null) {
                setSessionMap(getSerialSessionSupport().deserialSession(getServerName()));
            }
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "capacity:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "ms]",
                    this, port, capacity, expeltTime);
            serverSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
                @Override
                public void completed(AsynchronousSocketChannel client, Attachment att) {
                    Attachment newAtt = new Attachment();
                    SocketAddress clientAddr = null;
                    try {
                        clientAddr = client.getRemoteAddress();
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "accept=>completed" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
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
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", this, (System.currentTimeMillis() - start));
            Thread.currentThread().join();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
        }
        setRunning(false);
    }

}
