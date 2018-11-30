package pers.pandora.server;

import pers.pandora.servlet.Dispatcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * by pandora
 * 2018.11.17
 * version:1.3
 * encoding:utf-8
 * ps:适用于edge,ie
 */
//主启动类
public class Server {
    private int port;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    public Server(){
        this(8080);
    }
    public Server(int port){
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
    private void start(ServerSocket server){
        while(running){
            try {
                Socket client = server.accept();
                new Thread(new Dispatcher(client)).start();
            } catch (IOException e) {
                System.out.println("client连接出现问题！");
            }

        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
