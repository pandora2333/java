package pers.pandora.server;

import pers.pandora.mvc.RequestMappingHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class Server {

    private boolean running;

    private int port;

    private String rootPath = "./WebRoot";

    private String webConfigPath = rootPath + "/WEB-INF/web.xml";

    public static final String REQUEST_FILE_DIR = "/files/";

    private int poolSize = 10;

    private int capcity = 2048 * 1024;

    //基于MVC模式管理
    private RequestMappingHandler requestMappingHandler;

    //全局session管理,基于内存的生命周期
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(16);

    private Map<String, String> context;

    private static final String HOST = "127.0.0.1";

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setCapcity(int capcity) {
        this.capcity = capcity;
    }

    public int getCapcity() {
        return capcity;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public Map<String, Session> getSessionMap() {
        return sessionMap;
    }

    public void setSessionMap(Map<String, Session> sessionMap) {
        this.sessionMap = sessionMap;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public static String getHOST() {
        return HOST;
    }

    public void setWebConfigPath(String webConfigPath) {
        this.webConfigPath = webConfigPath;
    }

    public String getWebConfigPath() {
        return webConfigPath;
    }

    public void setRequestMappingHandler(RequestMappingHandler requestMappingHandler) {
        this.requestMappingHandler = requestMappingHandler;
    }

    public RequestMappingHandler getRequestMappingHandler() {
        return requestMappingHandler;
    }

    public abstract void start();

    public abstract void start(int port, int minCore, int maxCore, long keepAlive, TimeUnit timeUnit, long timeOut, TimeUnit timeOutUnit, long exvitTime);

    protected void execEvitThread(long exvitTime) {
        //扫描驱逐后台线程
        Thread invalidResourceExecutor = new Thread(() -> {
            while (true) {
                List<String> invalidKey = new ArrayList<>();
                for (Map.Entry<String, Session> session : sessionMap.entrySet()) {
                    if (session.getValue().getIsValid()) {
                        if (session.getValue().invalidTime() == 0) {
                            invalidKey.add(session.getKey());
                        }
                    }
                }
                for (String removeKey : invalidKey) {
                    System.out.println(Thread.currentThread().getName() + " SessionID invalid:" + removeKey);
                    sessionMap.remove(removeKey);
                }
                try {
                    //最多每秒执行一次
                    Thread.sleep(exvitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        invalidResourceExecutor.setDaemon(true);//后台线程
        invalidResourceExecutor.start();
    }
}
