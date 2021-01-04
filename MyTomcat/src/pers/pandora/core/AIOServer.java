package pers.pandora.core;

import pers.pandora.constant.LOG;
import pers.pandora.utils.XMLFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * //using simple way
 * //init BeanPool
 * BeanPool beanPool = new BeanPool();
 * //init aop config
 * beanPool.setAopPaths("pers.pandora.test");
 * beanPool.setAopProxyFactory(new JavassistAOPProxyFactory());
 * beanPool.init("pers.pandora.test","pers.pandora.servlet");
 * //init mvc-config file
 * RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
 * requestMappingHandler.init("pers.pandora.controller","pers.pandora.interceptor");
 * requestMappingHandler.setBeanPool(beanPool);
 * AIOServer server = new AIOServer();
 * //set session serializer and deserializer
 * server.setServerName("pandora_test_1");
 * server.setHotLoadJSP(false);
 * server.setIdWorker(new IdWorker());
 * setJsonParser(new SimpleJSONParser());
 * server.setRequestMappingHandler(requestMappingHandler);
 * server.setSerialSessionSupport(new SerialSessionSupportSimpler());
 * SerialSessionSupport.getSessionPool().put(server.getServerName(), server.getSessionMap());
 * //start server
 * server.start();
 * //main Thread stop
 * Server.mainLoop();
 */
public final class AIOServer extends Server {

    private AsynchronousServerSocketChannel asyncServerSocketChannel;

    @Override
    public void start() {
        start(getPort());
    }

    @Override
    public void start(int port) {
        setPort(port);
        //as file server? it defines up file size maybe over 1M
        //if 10m file up env,must be 100ms(think that network fluctuation env),maybe page load over 5s
        //the waitReceivedTime = browser sent request(include file data) need time + network fluctuation maybe need time
        try {
            //main thread pool should do to connect tcp socket from client
            mainPool = new ThreadPoolExecutor(getMainPoolMinSize(), getMainPoolMaxSize(),
                    getMainPoolKeepAlive(), TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
            //slave thread pool should do to I/O disk receiving client's datas
            slavePool = new ThreadPoolExecutor(getSlavePoolMinSize(), getSlavePoolMaxSize(),
                    getSlavePoolKeepAlive(), TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
            final AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(mainPool);
            final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            asyncServerSocketChannel = serverSocketChannel;
            //When work on TIME_WAIT status,quicky release the port
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            //set receive buffer,default value is 64kB
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, getTcpReceivedCacheSize());
            serverSocketChannel.bind(new InetSocketAddress(getHOST(), port), getBackLog());
            final Attachment att = new Attachment();
            final long start = System.currentTimeMillis();
            //init web.xml
            setContext(new XMLFactory().parse(getWebConfigPath()));
            att.setServer(this);
            //load SESSION.ser if set before the start method is running
            if (getSerialSessionSupport() != null) {
                setSessionMap(getSerialSessionSupport().deserialSession(getServerName()));
            }
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "receiveBuffer:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "ms" +
                            LOG.VERTICAL + "gcTime:" + LOG.LOG_PRE + "ms]",
                    getServerName(), port, getReceiveBuffer(), getExpelTime(), getGcTime());
            serverSocketChannel.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {
                @Override
                public void completed(AsynchronousSocketChannel client, Attachment att) {
                    final Attachment newAtt = new Attachment();
//                    SocketAddress clientAddr = null;
//                    try {
//                        clientAddr = client.getRemoteAddress();
//                    } catch (IOException e) {
//                        logger.error(LOG.LOG_PRE + "accept=>completed" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
//                    }
//                    logger.debug(LOG.LOG_PRE + "A new Connection:" + LOG.LOG_PRE, getServerName(), clientAddr);
                    newAtt.setClient(client);
                    newAtt.setServer(att.getServer());
                    newAtt.setReadBuffer(ByteBuffer.allocate(getReceiveBuffer()));
                    newAtt.setKeepTime(Instant.now());
                    newAtt.setWaitReceivedTime(att.getWaitReceivedTime());
                    newAtt.setWriteBuffer(ByteBuffer.allocateDirect(getResponseBuffer()));
                    slavePool.submit(() -> {
                        AIOServerlDispatcher dispatcher = new AIOServerlDispatcher();
                        try {
                            dispatcher.completed(client.read(newAtt.getReadBuffer()).get(), newAtt);
                        } catch (InterruptedException | ExecutionException e) {
                            //ignore
                            //Guaranteed by the thread pool match queue, concurrent access to the buffer will not occur
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
            if (getExpelTime() > 0) {
                execExpelThread();
            }
            if (getGcTime() > 0) {
                gc();
            }
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", getServerName(), (System.currentTimeMillis() - start));
        } catch (IOException | ClassNotFoundException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
        }
    }

}
