package pers.pandora.core;

import pers.pandora.constant.LOG;
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
        //using simple way
        //init BeanPool
        BeanPool.init();
        //init mvc controller
        RequestMappingHandler.init();
        AIOServer server = new AIOServer();
        //set session serializer and deserializer
        server.setServerName("pandora_test_1");
        server.setSerialSessionSupport(new SerialSessionSupportSimpler());
        SerialSessionSupport.getSessionPool().put(server.getServerName(), server.getSessionMap());
        //start server
        server.start();
        //main Thread stop
        Server.mainLoop();
    }

    @Override
    public void start() {
        start(8080, getCapcity(), 250, 1000, 30000, 80);
    }

    @Override
    public void start(int port, int capacity, int maxKeepClients, long expeltTime, long gcTime, long waitReceivedTime) {
        setPort(port);
        setRunning(true);
        setCapcity(capacity);
        setMaxKeepClients(maxKeepClients);
        //as file server? it defines up file size maybe over 1M
        //if 10m file up env,must be 100ms(think that network fluctuation env),maybe page load over 5s
        //the waitReceivedTime = browser sent request(include file data) need time + network fluctuation maybe need time
        try {
            //main thread pool should do to connect tcp socket from client
            mainPool = Executors.newFixedThreadPool(getPoolSize());
            //slave thread pool should do to I/O disk receiving client's datas
            slavePool = Executors.newFixedThreadPool(2 * getPoolSize() + 1);
            AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(mainPool);
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            asyncServerSocketChannel = serverSocketChannel;
            //When work on TIME_WAIT status,quicky release the port
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            //set receive buffer,default value is 64kB
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, getReceiveBuffer());
            serverSocketChannel.bind(new InetSocketAddress(getHOST(), port));
            Attachment att = new Attachment();
            long start = System.currentTimeMillis();
            //init web.xml
            setContext(new XMLFactory().parse(getWebConfigPath()));
            att.setServer(this);
            att.setWaitReceivedTime(waitReceivedTime);
            //load SESSION.ser if set before the start method is running
            if (getSerialSessionSupport() != null) {
                setSessionMap(getSerialSessionSupport().deserialSession(getServerName()));
            }
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "capacity:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "maxKeepClients:" + LOG.LOG_PRE + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "ms" +
                            +LOG.VERTICAL + "gcTime:" + LOG.LOG_PRE + "ms" + LOG.VERTICAL + "waitReceivedTime:" + LOG.LOG_PRE + "ms]",
                    getServerName(), port, capacity, maxKeepClients, expeltTime, gcTime, waitReceivedTime);
            serverSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
                @Override
                public void completed(AsynchronousSocketChannel client, Attachment att) {
                    Attachment newAtt = new Attachment();
                    SocketAddress clientAddr = null;
                    try {
                        clientAddr = client.getRemoteAddress();
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "accept=>completed" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
                    }
                    logger.info(LOG.LOG_PRE + "A new Connection:" + LOG.LOG_PRE, getServerName(), clientAddr);
                    newAtt.setClient(client);
                    newAtt.setServer(att.getServer());
                    newAtt.setBuffer(ByteBuffer.allocate(getCapcity()));
                    newAtt.setKeepAlive(true);
                    newAtt.setWaitReceivedTime(att.getWaitReceivedTime());
                    //long connection or short connection
                    if (getKeepClients().size() < getMaxKeepClients()) {
                        addClients(clientAddr.toString(), newAtt);
                    }
                    slavePool.submit(() -> {
                        AIOServerlDispatcher dispatcher = new AIOServerlDispatcher();
                        try {
                            //for the size is over 1M files to wait a time for receiving all datas,the time should determined by bandwidth
                            Thread.sleep(waitReceivedTime);
                            dispatcher.completed(client.read(newAtt.getBuffer()).get(), newAtt);
                        } catch (InterruptedException | ExecutionException e) {
                            dispatcher.failed(e, newAtt);
                        }
                    });
                    asyncServerSocketChannel.accept(att, this);
                }

                @Override
                public void failed(Throwable t, Attachment att) {
                    try {
                        logger.error(LOG.LOG_PRE + "accept" + LOG.LOG_POS, att.getClient().getRemoteAddress(), LOG.EXCEPTION_DESC, t);
                        close(att, this);
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "Not Get Client Remote IP:" + LOG.LOG_PRE, getServerName(), t);
                    }
                    asyncServerSocketChannel.accept(att, this);
                }
            });
            execExpelThread(expeltTime);
            gc(gcTime);
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", getServerName(), (System.currentTimeMillis() - start));
        } catch (IOException | ClassNotFoundException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
        }
    }

}
