package pers.pandora.controller;
import pers.pandora.annotation.WebSocket;
import pers.pandora.annotation.WebSocketMethod;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.WS;
import pers.pandora.core.WebSocketSession;

import java.nio.charset.Charset;
import java.util.Map;
@Deprecated
@WebSocket("/ws")
public class WSTestController {

    /**
     * only can use WebSocketSession and Map<String, WebSocketSession>
     * @param webSocketSession
     * @param clients
     */
    @WebSocketMethod("/hello")
    public void test(WebSocketSession webSocketSession, Map<String, WebSocketSession> clients){
        System.out.println("当前客户端连接情况:" + clients);
        webSocketSession.getDatas().stream().filter(v -> v.getType() == WS.TYPE_TEXT).
                forEach(v -> System.out.println("收到信息:" + new String(v.getData())));
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
