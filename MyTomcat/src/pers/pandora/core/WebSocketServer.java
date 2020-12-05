package pers.pandora.core;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.JSP;
import pers.pandora.constant.LOG;
import pers.pandora.utils.CodeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketServer extends Server {

    public static final String WS_MAGIC_STR = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static final byte TYPE_TEXT = 0x01;

    public static final byte TYPE_BINARY = 0x02;

    private Map<String, WebSocketSession> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        WebSocketServer server = new WebSocketServer();
        server.setServerName("websocket");
        server.start();
        Server.mainLoop();
    }

    @Override
    public void start() {
        start(8000, 1024 * 1024, 250, 0, 0, 0);
    }

    @Override
    public void start(int port, int capcity, int maxKeepClient, long expelTime, long gcTime, long waitReceivedTime) {
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
                        @Override
                        public void completed(Integer result, WebSocketSession attachment) {
                            ByteBuffer buffer = attachment.getBuffer();
                            buffer.flip();
                            try {
                                String ip = attachment.getClient().getRemoteAddress().toString();
                                if (ip != null && !clients.containsKey(ip)) {
                                    clients.put(ip, attachment);
                                    String msg = new String(buffer.array(), 0, buffer.limit());
                                    logger.info("received new ws connection:" + LOG.LOG_PRE, msg);
                                    byte[] linkMsg = buildWS(msg);
                                    ByteBuffer tmp = ByteBuffer.allocate(linkMsg.length);
                                    tmp.put(linkMsg);
                                    tmp.flip();
                                    attachment.getClient().write(tmp);
                                } else if (buffer.limit() > 0) {
                                    int i = buffer.position();
                                    byte first = (byte) (buffer.get(i) & 0x0f);
                                    int type = -1;
                                    if ((first & 0x0f) == TYPE_TEXT) {
                                        type = 0;
                                    } else if ((first & 0x0f) == TYPE_BINARY) {
                                        type = 1;
                                    } else if ((first & 0x08) > 0) {
                                        clients.remove(ip);
                                        close(att, this);
                                    }
                                    if (type >= 0) {
                                        byte second = buffer.get(++i);
                                        byte[] mask = null;
                                        if ((second & 0x80) > 0) {
                                            mask = new byte[4];
                                        }
                                        second &= 0x7f;
                                        long len = second & 0x7f;
                                        if (len >= 0x7d) {
                                            int size = len == 0x7e ? 2 : 8;
                                            len = 0;
                                            for (int j = 0; j < size; j++) {
                                                len = (len << 0x08) | (buffer.get(++i) & 0xff);
                                            }
                                        }
                                        if (mask != null) {
                                            for (int j = 0; j < 4; j++) {
                                                mask[j] = buffer.get(++i);
                                            }
                                        }
                                        len = Math.max(buffer.limit() - i - 1, 0);
                                        byte[] data = new byte[(int) len];
                                        for (int j = 0; ++i < buffer.limit() && j < len; j++) {
                                            data[j] = (byte) (buffer.get(i) ^ (mask[j % 4] & 0xFF));
                                        }
                                        if (type == 0) {
                                            logger.info("received new msg:" + LOG.LOG_PRE, new String(data));
                                        }
                                    }
                                }
                                buffer.clear();
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
                            t.printStackTrace();
                        }
                    });
                    serverSocketChannel.accept(att, this);
                }

                private byte[] buildWS(String msg) {

                    String key = JSP.NO_CHAR;
                    for (String s : msg.split(String.valueOf(HTTPStatus.CRLF), -1)) {
                        String[] sp = s.split(HTTPStatus.HEAD_INFO_SPLITER + HTTPStatus.BLANK, -1);
                        if (sp.length == 2 && sp[0].equals("Sec-WebSocket-Key")) {
                            key = sp[1].trim();
                            break;
                        }
                    }
                    String accept = CodeUtils.sha1AndBase64(key + WS_MAGIC_STR, null);
                    StringBuilder headInfo = new StringBuilder();
                    headInfo.append(HTTPStatus.HTTP1_1).append(HTTPStatus.BLANK).append(101).append(HTTPStatus.BLANK).append("Switching Protocols").append(HTTPStatus.CRLF);
                    headInfo.append("Connection: Upgrade").append(HTTPStatus.CRLF);
                    headInfo.append("Upgrade: WebSocket").append(HTTPStatus.CRLF);
                    headInfo.append("Sec-WebSocket-Accept: ").append(accept).append(HTTPStatus.CRLF);
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
}
