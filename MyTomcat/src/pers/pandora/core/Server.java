package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.mvc.RequestMappingHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class Server {

    protected Logger logger = LogManager.getLogger(this.getClass());

    private boolean running;

    private int port;

    private String rootPath = "./WebRoot";

    private String resourceRootPath = "/static/";

    private String webConfigPath = rootPath + "/WEB-INF/web.xml";

    public String requestFileDir = resourceRootPath + "files/";

    private int poolSize = 10;
    //上传文件缓冲区大小 (Byte)
    private int capcity = 2048 * 1024;

    //基于MVC模式管理
    private RequestMappingHandler requestMappingHandler;

    //全局session管理,基于内存的生命周期
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(16);

    private Map<String, String> context;

    private static final String HOST = "127.0.0.1";
    //下载文件缓冲区大小（Byte）
    private int fileBuffer = 2048;

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

    public void setResourceRootPath(String resourceRootPath) {
        this.resourceRootPath = resourceRootPath;
    }

    public String getResourceRootPath() {
        return resourceRootPath;
    }

    public void setRequestFileDir(String requestFileDir) {
        this.requestFileDir = requestFileDir;
    }

    public String getRequestFileDir() {
        return requestFileDir;
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

    public void setFileBuffer(int fileBuffer) {
        this.fileBuffer = fileBuffer;
    }

    public int getFileBuffer() {
        return fileBuffer;
    }

    public RequestMappingHandler getRequestMappingHandler() {
        return requestMappingHandler;
    }

    public abstract void start();

    public abstract void start(int port, int capcity, int minCore, int maxCore, long keepAlive, TimeUnit timeUnit,
                               long timeOut, TimeUnit timeOutUnit, long exvitTime);

    protected void execExpelThread(long expelTime) {
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
                    logger.info(LOG.LOG_PRE + "exec SessionID invalid:" + LOG.LOG_PRE, Thread.currentThread().getName(), removeKey);
                    sessionMap.remove(removeKey);
                }
                try {
                    //最多每秒执行一次
                    Thread.sleep(expelTime);
                } catch (InterruptedException e) {
                    logger.error(LOG.LOG_PRE + "execExpelThread" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
                }
            }
        });
        invalidResourceExecutor.setDaemon(true);//后台线程
        invalidResourceExecutor.start();
    }
}
