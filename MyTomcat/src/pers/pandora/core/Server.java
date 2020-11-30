package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Server {

    protected static Logger logger = LogManager.getLogger(Server.class);

    private boolean running;

    private int port;

    private String rootPath = "./WebRoot";

    private String resourceRootPath = "/static/";

    private String webConfigPath = rootPath + "/WEB-INF/web.xml";

    public String requestFileDir = resourceRootPath + "files/";
    //用于服务器扩展功能支持，比如Session序列化
    private String serverName;

    private int poolSize = 10;
    //上传文件缓冲区大小 (Byte)
    private int capcity = 2048 * 1024;

    //全局session管理,基于内存的生命周期
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(16);
    //设置了过期时间的session管理,优化线程扫描时间
    private Map<String, Session> invalidSessionMap = new ConcurrentHashMap<>(16);
    //Session序列化插件,不设置默认不支持Session序列化
    private SerialSessionSupport serialSessionSupport;

    private Map<String, String> context;

    private static final String HOST = "127.0.0.1";
    //下载文件缓冲区大小（Byte）
    private int fileBuffer = 2048;

    public void setSessionMap(Map<String, Session> sessionMap) {
        sessionMap.forEach((k, v) -> addSessionMap(k, v));
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public Map<String, Session> getInvalidSessionMap() {
        return invalidSessionMap;
    }

    public boolean addInvalidSessionMap(String key, Session session) {
        if (StringUtils.isNotEmpty(key)) {
            invalidSessionMap.put(key, session);
            return true;
        }
        return false;
    }

    public SerialSessionSupport getSerialSessionSupport() {
        return serialSessionSupport;
    }

    public void setSerialSessionSupport(SerialSessionSupport serialSessionSupport) {
        this.serialSessionSupport = serialSessionSupport;
    }

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

    public boolean addSessionMap(String key, Session session) {
        if (StringUtils.isNotEmpty(key) && session != null) {
            sessionMap.put(key, session);
            if (session.getIsValid()) {
                invalidSessionMap.put(key, session);
            }
            return true;
        }
        return false;
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

    public void setFileBuffer(int fileBuffer) {
        this.fileBuffer = fileBuffer;
    }

    public int getFileBuffer() {
        return fileBuffer;
    }

    public abstract void start();

    public abstract void start(int port, int capcity, long exvitTime);

    protected void execExpelThread(long expelTime) {
        //扫描驱逐后台线程
        final List<String> invalidKey = new ArrayList<>();
        final List<String> validKey = new ArrayList<>();
        Thread invalidResourceExecutor = new Thread(() -> {
            long startTime, endTime;
            while (true) {
                startTime = System.currentTimeMillis();
                invalidSessionMap.forEach((k, v) -> {
                    if (v.getIsValid()) {
                        if (v.invalidTime() == 0) {
                            invalidKey.add(k);
                        }
                    } else {
                        validKey.add(k);
                    }
                });
                invalidKey.stream().forEach(removeKey -> {
                    logger.info(LOG.LOG_PRE + "exec SessionID invalid:" + LOG.LOG_PRE, Thread.currentThread().getName(), removeKey);
                    sessionMap.remove(removeKey);
                    invalidSessionMap.remove(removeKey);
                });
                validKey.stream().forEach(addKey -> {
                    logger.info(LOG.LOG_PRE + "exec SessionID is valid:" + LOG.LOG_PRE, Thread.currentThread().getName(), addKey);
                    invalidSessionMap.remove(addKey);
                });
                invalidKey.clear();
                validKey.clear();
                endTime = System.currentTimeMillis();
                try {
                    //建议expelTime >= 1s  ms级别执行误差
                    Thread.sleep(expelTime - (endTime - startTime));
                } catch (InterruptedException e) {
                    logger.error(LOG.LOG_PRE + "execExpelThread" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
                }
            }
        });
        invalidResourceExecutor.setDaemon(true);//后台线程
        invalidResourceExecutor.start();
    }
}
