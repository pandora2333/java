package pers.pandora.core;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;
import pers.pandora.constant.WS;
import pers.pandora.utils.CodeUtils;
import pers.pandora.vo.BinaryWSData;
import pers.pandora.vo.TextWSData;
import pers.pandora.vo.WSData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * server.setCapcity(1024 * 1024);
 * server.start();
 * //lastly
 * Server.mainLoop();
 */
public class WebSocketServer extends Server {

    private final Map<String, WebSocketSession> clients = new ConcurrentHashMap<>(16);
    //Downtime when the maximum number of clients is maintained
    private long busyTime = 1000;
    //retry count
    private int retryCnt;
    //retry interval time
    private long retryTime;

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

    //Expose the interfaces, send tasks to the server regularly and push content to all clients
    public final Map<String, WebSocketSession> getClients() {
        return clients;
    }

    private String charset = HTTPStatus.DEFAULTENCODING;

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
    public final void start() {
        start(getPort());
    }

    @Override
    public final void start(int port) {
        setPort(port);
        try {
            long start = System.currentTimeMillis();
            ExecutorService mainPool = Executors.newFixedThreadPool(getMainPoolSize());
            ExecutorService slavePool = Executors.newFixedThreadPool(getSlavePoolSize());
            AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(mainPool);
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, getReceiveBuffer());
            serverSocketChannel.bind(new InetSocketAddress(getHOST(), port));
            logger.info(LOG.LOG_PRE + "start core params[port:" + LOG.LOG_PRE + LOG.VERTICAL + "capacity:" + LOG.LOG_PRE +
                            "byte" + LOG.VERTICAL + "maxKeepClients:" + LOG.LOG_PRE + LOG.VERTICAL + "expeltTime:" + LOG.LOG_PRE + "ms" +
                            +LOG.VERTICAL + "gcTime:" + LOG.LOG_PRE + "ms]",
                    getServerName(), port, getCapcity(), getMaxKeepClients(), getExpelTime(), getGcTime());
            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, WebSocketSession>() {
                @Override
                public void completed(AsynchronousSocketChannel client, WebSocketSession att) {
                    if (clients.size() < getMaxKeepClients()) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(getCapcity());
                        WebSocketSession webSocketSession = new WebSocketSession();
                        webSocketSession.setClient(client);
                        webSocketSession.setReadBuffer(byteBuffer);
                        webSocketSession.setWriteBuffer(ByteBuffer.allocateDirect(getResponseBuffer()));
                        client.read(byteBuffer, webSocketSession, new CompletionHandler<Integer, WebSocketSession>() {
                            //write message to client
                            private void writeMsg(byte[] msg, WebSocketSession attachment) {
                                if (msg == null || msg.length == 0) {
                                    return;
                                }
                                if(attachment.getWriteBuffer().capacity() < msg.length){
                                    attachment.setWriteBuffer(ByteBuffer.allocate(msg.length));
                                }
                                //Avoid for concurrently reading in the same buffer by read-thread
                                ByteBuffer tmp =  attachment.getWriteBuffer();
                                try {
                                    tmp.put(msg);
                                    //In multi-thread,it is easy to cause WritePending Exception,because of last one write operation was still running
                                    tmp.flip();
                                    attachment.getClient().write(tmp).get();
                                } catch (Exception e) {
                                    //ignore,retry again
                                    boolean ok = false;
                                    for(int i = 0;i < retryCnt && !ok;i++){
                                        ok = true;
                                        try {
                                            Thread.sleep(retryTime);
                                        } catch (InterruptedException ex) {
                                            //ignore
                                        }
                                        try{
                                            attachment.getClient().write(tmp);
                                        }catch (Exception exx){
                                            //ignore
                                            ok = false;
                                        }
                                    }
                                }
                                tmp.compact();
                            }
                            @Override
                            public void completed(Integer result, WebSocketSession attachment) {
                                ByteBuffer buffer = attachment.getReadBuffer();
                                buffer.flip();
                                try {
                                    String ip = attachment.getClient().getRemoteAddress().toString();
                                    if (ip != null && !clients.containsKey(ip)) {
                                        String msg = new String(buffer.array(), 0, buffer.limit());
                                        webSocketSession.setReqUrl(msg.substring(msg.indexOf(HTTPStatus.SLASH), msg.indexOf(HTTPStatus.HTTP)).trim());
                                        if (getRequestMappingHandler().getWsMappings().containsKey(webSocketSession.getReqUrl())) {
                                            //sync write
                                            writeMsg(buildWS(msg), attachment);
                                            //callBack method for exec some init-methods
                                            writeMsg(initWebSocketSession(attachment), attachment);
                                        } else {
                                            //no-mapping url path,just close it
                                            close(attachment, this);
                                            return;
                                        }
                                        clients.put(ip, attachment);
                                    } else {
                                        //TCP packet sticking / unpacking,the application layer protocol splitting
                                        //chrome's max bracket size is 128k
                                        int i = buffer.position();
                                        attachment.clear();
                                        while (i < buffer.limit()) {
                                            WSData data = null;
                                            if (attachment.getDatas().size() > 0) {
                                                data = attachment.getDatas().get(attachment.getDatas().size() - 1);
                                            }
                                            if (data != null) {
                                                i = readWSData(i, buffer, data);
                                                attachment.getDatas().add(data);
                                                data = null;
                                            }
                                            if (i + 1 < buffer.limit()) {
                                                byte first = buffer.get(i);
                                                boolean finish = false;
                                                if ((first & 0x80) > 0) {
                                                    finish = true;
                                                }
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
                                                    destroyWebSocketSession(attachment);
                                                    close(attachment, this);
                                                    return;
                                                }
                                                if (data != null) {
                                                    data.setFinish(finish);
                                                    byte second = buffer.get(++i);
                                                    byte[] mask = null;
                                                    if ((second & 0x80) > 0) {
                                                        mask = new byte[4];
                                                    }
                                                    second &= 0x7f;
                                                    //The maximum packet length should be controlled within the maximum range of long integers
                                                    long len = second & 0x7f;
                                                    if (len >= 0x7d) {
                                                        int size = len == 0x7e ? 2 : 8;
                                                        len = 0;
                                                        for (int j = 0; j < size; j++) {
                                                            len = (len << 0x08) | (buffer.get(++i) & 0xff);
                                                            if (len > getMaxUpBits()) {
                                                                writeMsg(WS.OVER_UP_DATA_SIZE.getBytes(Charset.forName(charset)), attachment);
                                                                close(attachment, this);
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
                                                    i = readWSData(i, buffer, data);
                                                    attachment.getDatas().add(data);
                                                }
                                            }
                                        }
                                        //sync request and exec callback method
                                        getRequestMappingHandler().execWSCallBack(webSocketSession, clients);
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
                                } catch (IOException e) {
                                    //IO concurrent error, forced interrupt
                                    this.failed(e, attachment);
                                }
                            }

                            @Override
                            public void failed(Throwable t, WebSocketSession attachment) {
                                close(attachment, this);
                            }
                        });
                    } else {
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
                        final List<Byte> buf = new LinkedList<>();
                        for (int j = 0; ++i < buffer.limit() && j < data.getRemain(); j++) {
                            byte cur = buffer.get(i);
                            if (data.getMask() != null) {
                                cur = (byte) (cur ^ (data.getMask()[j % 4] & 0xff));
                            }
                            buf.add(cur);
                        }
                        data.setRemain(data.getRemain() - buf.size());
                        data.setLength(buf.size());
                        byte[] tmp = new byte[buf.size()];
                        for (int j = 0; j < buf.size(); j++) {
                            tmp[j] = buf.get(j);
                        }
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
                    String accept = CodeUtils.sha1AndBase64(key + WS.WS_MAGIC_STR, null);
                    String headInfo = HTTPStatus.HTTP1_1 + HTTPStatus.BLANK + WS.CODE_101 + HTTPStatus.BLANK +
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
                        close(att, this);
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "Not Get Client Remote IP:" + LOG.LOG_PRE, getServerName(), t);
                    }
                    serverSocketChannel.accept(att, this);
                }
            });
            logger.info(LOG.LOG_PRE + "Start! in time:" + LOG.LOG_PRE + "ms", getServerName(), (System.currentTimeMillis() - start));
        } catch (IOException e) {
            logger.error(LOG.LOG_PRE + "start" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
        }
    }

    //give a @Override method for some destroy-methods when a WebSocketSession will be closed
    protected void destroyWebSocketSession(WebSocketSession attachment) {
        try {
            logger.info("Get a " + LOG.LOG_PRE + "closed request!", attachment.getClient().getRemoteAddress());
        } catch (IOException e) {
            //ignore
        }
    }

    protected final byte[] buildSendMsg(WebSocketSession webSocketSession) {
        List<Byte> buf = new ArrayList<>(8);
        buf.add((byte) (0x80 | webSocketSession.getOutPutType() & 0xff));
        //allow 4 bytes
        int len = webSocketSession.getOutPutContent().length;
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
