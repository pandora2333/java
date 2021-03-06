package pers.pandora.web.controller;
import pers.pandora.common.web.WebSocket;
import pers.pandora.web.annotation.WebSocketMethod;
import pers.pandora.web.constant.WS;
import pers.pandora.web.constant.HTTPStatus;
import pers.pandora.web.core.WebSocketSession;

import java.nio.charset.Charset;
import java.util.Map;

@Deprecated
@WebSocket("/ws")
public class WSTestController {

    /**
     * only can use WebSocketSession and Map<String, WebSocketSession>
     *
     * @param webSocketSession
     * @param clients
     */
    @WebSocketMethod("/hello")
    public void test(WebSocketSession webSocketSession, Map<String, WebSocketSession> clients) {
        System.out.println("当前客户端连接情况:" + clients);//filter(v -> v.getType() == WS.TYPE_TEXT).
        System.out.println("收到信息:" + webSocketSession.getData().getType() + "|" + new String(webSocketSession.getData().getData()));
        if (webSocketSession.getData().getType() == WS.TYPE_PING) {
            webSocketSession.setOutPutType(WS.TYPE_PONG);
        } else if (webSocketSession.getData().getType() == WS.TYPE_PONG) {
            System.out.println("PONG!");
        } else {
            webSocketSession.setOutPutType(WS.TYPE_TEXT);
            webSocketSession.writeContent(("GET / HTTP/1.1\n" +
                    "Host: localhost:8080\n" +
                    "Connection: Upgrade\n" +
                    "Pragma: no-cache\n" +
                    "Cache-Control: no-cache\n" +
                    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36\n" +
                    "Upgrade: websocket\n" +
                    "Origin: https://www.cnblogs.com\n" +
                    "Sec-WebSocket-Version: 13\n" +
                    "Accept-Encoding: gzip, deflate, br\n" +
                    "Accept-Language: zh-CN,zh;q=0.9\n" +
                    "Sec-WebSocket-Key: nTncD+3a0tGzijp8+I224w==\n" +
                    "Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits").getBytes(Charset.forName(HTTPStatus.DEFAULTENCODING)));
        }

    }
}
