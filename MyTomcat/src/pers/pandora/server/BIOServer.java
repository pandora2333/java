package pers.pandora.server;

import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.servlet.Dispatcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * by pandora
 * 2018.11.17
 * version:1.3
 * encoding:utf-8
 */
//主启动类
@Deprecated
public final class BIOServer {
    private int port;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    public BIOServer(){
        this(8080);
    }
    public BIOServer(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("MyTomcat启动成功...");
        } catch (IOException e) {
            System.out.println("server启动异常!可能是端口已被占用");
            running = false;
        }finally {
            start(serverSocket);
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    private void start(ServerSocket server){
        RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
        requestMappingHandler.init();
        while(running){
            try {
                Socket client = server.accept();
                Dispatcher dispatcher = new Dispatcher(client);
                dispatcher.setRequestMappingHandler(requestMappingHandler);
                new Thread(dispatcher).start();
            } catch (IOException e) {
                System.out.println("client连接出现问题！");
            }

        }
    }

    public static void main(String[] args) {
        new BIOServer();
    }
}
