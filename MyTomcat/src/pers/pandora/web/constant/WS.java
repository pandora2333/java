package pers.pandora.web.constant;

public class WS {

    public static final String WS_MAGIC_STR = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static final byte TYPE_NULL = -1;

    public static final byte TYPE_TEXT = 0x01;

    public static final byte TYPE_BINARY = 0x02;

    public static final byte TYPE_PING = 0x09;

    public static final byte TYPE_PONG = 0x0A;

    public static final byte TYPE_CLOSE = 0x08;

    public static final String SEC_WEBSOCKET_KEY =  "Sec-WebSocket-Key";

    public static final String CONNECTION_KEY =  "Connection: Upgrade";

    public static final String UPGRADE_KEY =  "Upgrade: WebSocket";

    public static final String SWITCHING_PROTOCOLS =  "Switching Protocols";

    public static final String SEC_WEBSOCKET_ACCPET = "Sec-WebSocket-Accept:";

    public static final int CODE_101 = 101;

    public static final String OVER_UP_DATA_SIZE = "If the maximum number of upload bits is exceeded, the connection is forced to close!";

    public static final String WELCOMNE = "Welcome to join us with the Pandora Server!";
}
