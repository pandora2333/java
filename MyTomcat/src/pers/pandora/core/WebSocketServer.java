package pers.pandora.core;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;
import pers.pandora.constant.WS;
import pers.pandora.utils.CodeUtils;
import pers.pandora.vo.BinaryWSData;
import pers.pandora.vo.TextWSData;
import pers.pandora.vo.WSData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * //firstly,init BeanPool
 * BeanPool beanPool = new BeanPool();
 * beanPool.init(BeanPool.ROOTPATH);
 * //init ws-path config file
 * RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
 * requestMappingHandler.init(BeanPool.ROOTPATH);
 * requestMappingHandler.setBeanPool(beanPool);
 * WebSocketServer server = new WebSocketServer();
 * server.setRequestMappingHandler(requestMappingHandler);
 * server.setServerName("WebSocket");
 * setJsonParser(new SimpleJSONParser());
 * server.setPort(8000);
 * server.setReceiveBuffer(1024 * 1024);
 * server.start();
 * //lastly
 * Server.mainLoop();
 */
public class WebSocketServer extends Server {

    protected final Map<String, WebSocketSession> clients = new ConcurrentHashMap<>(16);
    //Downtime when the maximum number of clients is maintained
    protected long busyTime = 1000;
    //retry count
    protected int retryCnt;
    //retry interval time
    protected long retryTime;

    protected boolean openMsg;

    protected boolean closeMsg;

    protected int maxKeepClients;

    protected String charset = HTTPStatus.DEFAULTENCODING;

    public int getMaxKeepClients() {
        return maxKeepClients;
    }

    public void setMaxKeepClients(int maxKeepClients) {
        this.maxKeepClients = maxKeepClients;
    }

    //Expose the interfaces, send tasks to the server regularly and push content to all clients
    public Map<String, WebSocketSession> getClients() {
        return clients;
    }

    public boolean isCloseMsg() {
        return closeMsg;
    }

    public boolean isOpenMsg() {
        return openMsg;
    }

    public void setOpenMsg(boolean openMsg) {
        this.openMsg = openMsg;
    }

    public void setCloseMsg(boolean closeMsg) {
        this.closeMsg = closeMsg;
    }

    public int getRetryCnt() {
        return retryCnt;
    }

    public void setRetryCnt(int retryCnt) {
        this.retryCnt = retryCnt;
    }

    public long getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(long retryTime) {
        this.retryTime = retryTime;
    }

    public long getBusyTime() {
        return busyTime;
    }

    public void setBusyTime(long busyTime) {
        this.busyTime = busyTime;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    @Override
    public void start() {
        start(getPort());
    }

    @Override
    public final void start(int port) {
        setPort(port);
        try {
            final long start = System.currentTimeMillis();
            //main thread pool should do to connect tcp socket from client
            mainPool = new ThreadPoolExecutor(mainPoolMinSize, mainPoolMaxSize,
                    mainPoolKeepAlive, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueSize));
            //slave thread pool should do to I/O disk receiving client's datas
            slavePool = new ThreadPoolExecutor(slavePoolMinSize, slavePoolMaxSize,
                    slavePoolKeepAlive, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueSize));
            final AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(mainPool);
            final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            if (tcpReceivedCacheSize > 0) {
                serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpReceivedCacheSize);
            }
            serverSocketChannel.bind(new InetSocketAddress(HOST, port), backLog);
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "receiveBuffer:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "maxKeepClients:" + LOG.LOG_PRE + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "ms" +
                            LOG.VERTICAL + "gcTime:" + LOG.LOG_PRE + "ms]",
                    serverName, port, receiveBuffer, maxKeepClients, expelTime, gcTime);
            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, WebSocketSession>() {
                @Override
                public void completed(AsynchronousSocketChannel client, WebSocketSession att) {
                    if (clients.size() < getMaxKeepClients()) {
                        if (tcpSendCacheSize > 0) {
                            try {
                                client.setOption(StandardSocketOptions.SO_SNDBUF, tcpSendCacheSize);
                            } catch (IOException e) {
                                //ignore
                                //Because it is set before writing data, no exception will occur
                            }
                        }
                        final ByteBuffer byteBuffer = ByteBuffer.allocate(receiveBuffer);
                        final WebSocketSession webSocketSession = new WebSocketSession();
                        webSocketSession.setClient(client);
                        webSocketSession.setReadBuffer(byteBuffer);
                        webSocketSession.setWriteBuffer(ByteBuffer.allocate(sendBuffer));
                        client.read(byteBuffer, webSocketSession, new CompletionHandler<Integer, WebSocketSession>() {
                            //write message to client
                            private void writeMsg(byte[] msg, WebSocketSession attachment) {
                                if (msg == null || msg.length == 0) {
                                    return;
                                }
                                if (attachment.getWriteBuffer().capacity() < msg.length) {
                                    attachment.setWriteBuffer(ByteBuffer.allocateDirect(msg.length));
                                }
                                //Avoid for concurrently reading in the same buffer by read-thread
                                final ByteBuffer by = attachment.getWriteBuffer();
                                try {
                                    by.put(msg);
                                    //In multi-thread,it is easy to cause WritePending Exception,because of last one write operation was still running
                                    by.flip();
                                    client.write(by).get();
                                } catch (Exception e) {
                                    //ignore,retry again
                                    boolean ok = false;
                                    for (int i = 0; i < retryCnt && !ok; i++) {
                                        ok = true;
                                        try {
                                            Thread.sleep(retryTime);
                                        } catch (InterruptedException ex) {
                                            //ignore
                                        }
                                        try {
                                            client.write(by);
                                        } catch (Exception exx) {
                                            //ignore
                                            ok = false;
                                        }
                                    }
                                }
                                by.clear();
                            }

                            @Override
                            public void completed(Integer result, WebSocketSession attachment) {
                                final ByteBuffer buffer = attachment.getReadBuffer();
                                buffer.flip();
                                String ip = null;
                                try {
                                    ip = client.getRemoteAddress().toString();
                                } catch (IOException e) {
                                    //ignore
                                }
                                if (ip != null && !clients.containsKey(ip)) {
                                    String msg;
                                    try {
                                        msg = URLDecoder.decode(new String(buffer.array(), 0, buffer.limit()), charset);
                                    } catch (UnsupportedEncodingException e) {
                                        logger.error("url decode fail" + LOG.LOG_PRE + LOG.LOG_POS, LOG.EXCEPTION_DESC, charset, e.getMessage());
                                        writeMsg(e.getMessage().getBytes(Charset.forName(charset)), attachment);
                                        close(attachment, this, ip);
                                        return;
                                    }
                                    webSocketSession.setReqUrl(msg.substring(msg.indexOf(HTTPStatus.SLASH), msg.indexOf(HTTPStatus.HTTP)).trim());
                                    if (requestMappingHandler.getWsMappings().containsKey(webSocketSession.getReqUrl())) {
                                        //sync write
                                        writeMsg(buildWS(msg), attachment);
                                        //callBack method for exec some init-methods
                                        if (openMsg) {
                                            writeMsg(initWebSocketSession(attachment), attachment);
                                        }
                                    } else {
                                        //no-mapping url path,just close it
                                        close(attachment, this, null);
                                        return;
                                    }
                                    clients.put(ip, attachment);
                                } else {
                                    //TCP packet sticking / unpacking,the application layer protocol splitting
                                    //chrome's max bracket size is 128k
                                    int i = buffer.position();
                                    attachment.clear();
                                    WSData data;
                                    byte first, second;
                                    boolean finish;
                                    byte[] mask;
                                    long len;
                                    final List<WSData> datas = attachment.getDatas();
                                    while (i < buffer.limit()) {
                                        data = null;
                                        if (attachment.getDatas().size() > 0) {
                                            data = datas.get(attachment.getDatas().size() - 1);
                                            datas.clear();
                                        }
                                        if (data != null) {
                                            i = readWSData(i, buffer, data);
                                            datas.add(data);
                                            data = null;
                                        }
                                        if (i + 1 < buffer.limit()) {
                                            first = buffer.get(i);
                                            finish = (first & 0x80) > 0;
                                            first &= 0x0f;
                                            if ((first & 0x0f) == WS.TYPE_TEXT) {
                                                data = new TextWSData();
                                                data.setType(WS.TYPE_TEXT);
                                            } else if ((first & 0x0f) == WS.TYPE_BINARY) {
                                                data = new BinaryWSData();
                                                data.setType(WS.TYPE_BINARY);
                                            } else if ((first & 0x08) > 0) {
                                                attachment.setCloseSignal(true);
                                                clients.remove(ip);
                                                //callBack method for exec some destroy-methods
                                                if (closeMsg) {
                                                    destroyWebSocketSession(attachment);
                                                }
                                                close(attachment, this, ip);
                                                return;
                                            }
                                            if (data != null) {
                                                data.setFinish(finish);
                                                second = buffer.get(++i);
                                                mask = null;
                                                if ((second & 0x80) > 0) {
                                                    mask = new byte[4];
                                                }
                                                second &= 0x7f;
                                                //The maximum packet length should be controlled within the maximum range of long integers
                                                len = second & 0x7f;
                                                if (len >= 0x7d) {
                                                    int size = len == 0x7e ? 2 : 8;
                                                    len = 0;
                                                    for (int j = 0; j < size; j++) {
                                                        len = (len << 0x08) | (buffer.get(++i) & 0xff);
                                                        if (len > getMaxUpBits()) {
                                                            writeMsg(WS.OVER_UP_DATA_SIZE.getBytes(Charset.forName(charset)), attachment);
                                                            close(attachment, this, ip);
                                                            return;
                                                        }
                                                    }
                                                }
                                                //Security. However, it is not to prevent data leakage, but to prevent proxy cache pollution attacks in earlier versions of the protocol
                                                if (mask != null) {
                                                    for (int j = 0; j < 4; j++) {
                                                        mask[j] = buffer.get(++i);
                                                    }
                                                    data.setMask(mask);
                                                }
                                                data.setRemain(len);
                                                data.setLength(len);
                                                i = readWSData(i, buffer, data);
                                                attachment.getDatas().add(data);
                                            }
                                        }
                                    }
                                    //sync request and exec callback method
                                    requestMappingHandler.execWSCallBack(webSocketSession, clients);
                                    //sync write-task,because of a request for information, only one transmission
                                    if (webSocketSession.getOutPutContent() != null) {
                                        writeMsg(buildSendMsg(webSocketSession), attachment);
                                        //clear-operation
                                        webSocketSession.writeContent(null);
                                        webSocketSession.setOutPutType(WS.TYPE_TEXT);
                                    }
                                }
                                buffer.clear();
                                //async read-task
                                slavePool.submit(() -> {
                                    try {
                                        this.completed(attachment.getClient().read(buffer).get(), attachment);
                                    } catch (InterruptedException | ExecutionException e) {
                                        //ignore
                                    }
                                });
                            }

                            @Override
                            public void failed(Throwable t, WebSocketSession attachment) {
                                //ignore
                            }
                        });
                    } else {
                        //too many tcp connections
                        if (client.isOpen()) {
                            try {
                                client.close();
                            } catch (IOException e) {
                                //ignore
                            }
                        }
                        try {
                            Thread.sleep(busyTime);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                    serverSocketChannel.accept(att, this);
                }

                private int readWSData(int i, ByteBuffer buffer, WSData data) {
                    if (data != null && buffer != null) {
                        final int len = (int) Math.min(buffer.limit() - i - 1, data.getRemain());
                        int pLen = 0;
                        final byte[] tmp = new byte[(data.getData() != null ? pLen = data.getData().length : 0) + len];
                        if (pLen > 0) {
                            System.arraycopy(data.getData(), 0, tmp, 0, pLen);
                        }
                        for (int j = 0; ++i < buffer.limit() && j < data.getRemain(); j++) {
                            tmp[j + pLen] = buffer.get(i);
                            if (data.getMask() != null) {
                                tmp[j + pLen] = (byte) (tmp[j + pLen] ^ (data.getMask()[j % 4] & 0xff));
                            }
                        }
                        data.setRemain(data.getRemain() - len);
                        data.setData(tmp);
                    }
                    return i;
                }

                private byte[] buildWS(String msg) {
                    //Currently, only header SEC_WEBSOCKET_KEY verification is supported
                    String key = LOG.NO_CHAR;
                    for (String s : msg.split(String.valueOf(HTTPStatus.CRLF), -1)) {
                        String[] sp = s.split(HTTPStatus.HEAD_INFO_SPLITER + HTTPStatus.BLANK, -1);
                        if (sp.length == 2 && sp[0].equals(WS.SEC_WEBSOCKET_KEY)) {
                            key = sp[1].trim();
                            break;
                        }
                    }
                    final String accept = CodeUtils.sha1AndBase64(key + WS.WS_MAGIC_STR, null);
                    final String headInfo = HTTPStatus.HTTP1_1 + HTTPStatus.BLANK + WS.CODE_101 + HTTPStatus.BLANK +
                            WS.SWITCHING_PROTOCOLS + HTTPStatus.CRLF +
                            WS.CONNECTION_KEY + HTTPStatus.CRLF +
                            WS.UPGRADE_KEY + HTTPStatus.CRLF +
                            WS.SEC_WEBSOCKET_ACCPET + HTTPStatus.BLANK + accept + HTTPStatus.CRLF +
                            HTTPStatus.CRLF;
                    return headInfo.getBytes(Charset.forName(charset));
                }

                @Override
                public void failed(Throwable t, WebSocketSession att) {
                    try {
                        logger.error(LOG.LOG_PRE + "accept" + LOG.LOG_POS, att.getClient().getRemoteAddress(), LOG.EXCEPTION_DESC, t);
                        close(att, this, null);
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "Not Get Client Remote IP:" + LOG.LOG_PRE, serverName, t);
                    }
                    serverSocketChannel.accept(att, this);
                }
            });
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", serverName, (System.currentTimeMillis() - start));
        } catch (IOException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, serverName, LOG.EXCEPTION_DESC, e);
        }
    }

    //give a @Override method for some destroy-methods when a WebSocketSession will be closed
    protected void destroyWebSocketSession(WebSocketSession attachment) {
        try {
            logger.debug("Get a " + LOG.LOG_PRE + "closed request!", attachment.getClient().getRemoteAddress());
        } catch (IOException e) {
            //ignore
        }
    }

    protected final byte[] buildSendMsg(WebSocketSession webSocketSession) {
        final List<Byte> buf = new ArrayList<>(8);
        buf.add((byte) (0x80 | webSocketSession.getOutPutType() & 0xff));
        //allow 4 bytes
        final int len = webSocketSession.getOutPutContent().length;
        if (len > 125) {
            if (len > 65535) {
                buf.add((byte) 0x7f);
                buf.add((byte) ((len >> 0x18) & 0xff));
                buf.add((byte) ((len >> 0x10) & 0xff));
            } else {
                buf.add((byte) 0x7e);
            }
            buf.add((byte) ((len >> 0x08) & 0xff));
            buf.add((byte) (len & 0xff));
        } else {
            buf.add((byte) (len & 0xff));
        }
        byte[] data = new byte[buf.size() + len];
        for (int i = 0; i < data.length; i++) {
            if (i >= buf.size()) {
                data[i] = webSocketSession.getOutPutContent()[i - buf.size()];
            } else {
                data[i] = buf.get(i);
            }
        }
        return data;
    }

    //give a @Override method for some init-methods when a WebSocketSession is successfully built
    protected byte[] initWebSocketSession(WebSocketSession attachment) {
        attachment.writeContent(WS.WELCOMNE.getBytes(Charset.forName(HTTPStatus.DEFAULTENCODING)));
        return buildSendMsg(attachment);
    }
}
