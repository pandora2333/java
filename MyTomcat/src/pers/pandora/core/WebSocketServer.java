package pers.pandora.core;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;
import pers.pandora.constant.WS;
import pers.pandora.mvc.RequestMappingHandler;
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

public class WebSocketServer extends Server {

    private final Map<String, WebSocketSession> clients = new ConcurrentHashMap<>();

    private int maxWSBits = 1024 * 1024;

    //Expose the interfaces, send tasks to the server regularly and push content to all clients
    public final Map<String, WebSocketSession> getClients() {
        return clients;
    }

    private String charset = HTTPStatus.DEFAULTENCODING;

    public static void main(String[] args) {
        //firstly,init BeanPool
        BeanPool beanPool = new BeanPool();
        beanPool.init(BeanPool.ROOTPATH);
        //secondly,build the WebSocketServer class instance and start up it
        //init ws-path config file
        RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
        requestMappingHandler.init(BeanPool.ROOTPATH);
        requestMappingHandler.setBeanPool(beanPool);
        WebSocketServer server = new WebSocketServer();
        server.setRequestMappingHandler(requestMappingHandler);
        server.setServerName("WebSocket");
        server.start();
        //lastly
        Server.mainLoop();
    }

    public final int getMaxWSBits() {
        return maxWSBits;
    }


    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    public final void setMaxWSBits(int maxWSBits) {
        this.maxWSBits = maxWSBits;
    }

    @Override
    public final void start() {
        start(8000, 1024 * 1024, 250, 0, 0, 100);
    }

    @Override
    public final void start(int port, int capcity, int maxKeepClient, long expelTime, long gcTime, long waitTime) {
        setCapcity(capcity);
        setPort(port);
        try {
            long start = System.currentTimeMillis();
            ExecutorService mainPool = Executors.newFixedThreadPool(getPoolSize());
            ExecutorService slavePool = Executors.newFixedThreadPool(getPoolSize());
            AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(mainPool);
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asyncChannelGroup);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, getReceiveBuffer());
            serverSocketChannel.bind(new InetSocketAddress(getHOST(), port));
            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, WebSocketSession>() {
                @Override
                public void completed(AsynchronousSocketChannel client, WebSocketSession att) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(getCapcity());
                    WebSocketSession webSocketSession = new WebSocketSession();
                    webSocketSession.setClient(client);
                    webSocketSession.setBuffer(byteBuffer);
                    client.read(byteBuffer, webSocketSession, new CompletionHandler<Integer, WebSocketSession>() {
                        //write message to client
                        private void writeMsg(byte[] msg, WebSocketSession attachment) {
                            if (msg == null || msg.length == 0) {
                                return;
                            }
                            //Avoid for concurrently reading in the same buffer by read-thread
                            ByteBuffer tmp = ByteBuffer.allocate(msg.length);
                            tmp.put(msg);
                            tmp.flip();
                            attachment.getClient().write(tmp);
                        }

                        @Override
                        public void completed(Integer result, WebSocketSession attachment) {
                            ByteBuffer buffer = attachment.getBuffer();
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
                                                        if (len > maxWSBits) {
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
                                        e.printStackTrace();
                                        this.failed(e, attachment);
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(LOG.LOG_PRE + "Not Get Client Remote IP:" + LOG.LOG_PRE, getServerName(), e);
                                this.failed(e, attachment);
                            }
                        }

                        @Override
                        public void failed(Throwable t, WebSocketSession attachment) {
                            close(attachment, this);
                        }
                    });
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
                    StringBuilder headInfo = new StringBuilder();
                    headInfo.append(HTTPStatus.HTTP1_1).append(HTTPStatus.BLANK).append(WS.CODE_101).append(HTTPStatus.BLANK).
                            append(WS.SWITCHING_PROTOCOLS).append(HTTPStatus.CRLF);
                    headInfo.append(WS.CONNECTION_KEY).append(HTTPStatus.CRLF);
                    headInfo.append(WS.UPGRADE_KEY).append(HTTPStatus.CRLF);
                    headInfo.append(WS.SEC_WEBSOCKET_ACCPET).append(HTTPStatus.BLANK).append(accept).append(HTTPStatus.CRLF);
                    headInfo.append(HTTPStatus.CRLF);
                    return headInfo.toString().getBytes();
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
