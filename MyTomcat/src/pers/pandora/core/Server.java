package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.utils.StringUtils;
import pers.pandora.vo.Attachment;

import java.io.IOException;
import java.time.Instant;
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
    //serer name，it use logs,session file,etc
    private String serverName = "PandoraWeb" + System.currentTimeMillis();

    private int poolSize = Runtime.getRuntime().availableProcessors() + 1;
    //up file buffer size (byte)
    private int capcity = 10 * 1024 * 1024;

    private static final String HOST = "127.0.0.1";
    //download resource buffer size（byte）
    private int responseBuffer = capcity / 10;
    //server receive buffer size, it should greater than or equal to capcity
    private int receiveBuffer = capcity;
    //global session pool,base on memory
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(16);
    //set invalid time for the sessions,optimize the thread scanning
    private Map<String, Session> invalidSessionMap = new ConcurrentHashMap<>(16);
    //session serializer,default it is not supported
    private SerialSessionSupport serialSessionSupport;

    private Map<String, String> context;

    protected static void mainLoop() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.error(LOG.LOG_PRE + "mainLoop" + LOG.LOG_POS, Thread.currentThread().getName(), LOG.EXCEPTION_DESC, e);
        }
    }

    public void setReceiveBuffer(int receiveBuffer) {
        this.receiveBuffer = receiveBuffer;
    }

    public int getReceiveBuffer() {
        return receiveBuffer;
    }

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
            if (session.getMax_age() != null) {
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

    public void setResponseBuffer(int responseBuffer) {
        this.responseBuffer = responseBuffer;
    }

    public int getResponseBuffer() {
        return responseBuffer;
    }

    public abstract void start();

    public abstract void start(int port, int capcity, long expelTime, long waitReceivedTime);

    protected void execExpelThread(long expelTime) {
        final List<String> invalidKey = new ArrayList<>();
        final List<String> validKey = new ArrayList<>();
        Thread invalidResourceExecutor = new Thread(() -> {
            long startTime = 0, endTime = 0;
            while (true) {
                try {
                    //expelTime >= 1s ,it can control in ms time level
                    Thread.sleep(Math.max(0, expelTime - (endTime - startTime)));
                } catch (InterruptedException e) {
                    logger.error(LOG.LOG_PRE + "execExpelThread" + LOG.LOG_POS, this.getServerName(), LOG.EXCEPTION_DESC, e);
                }
                Instant now = Instant.now();
                startTime = now.getEpochSecond();
                invalidSessionMap.forEach((k, v) -> {
                    if (v.getMax_age() != null) {
                        if (now.compareTo(v.getMax_age()) >= 0) {
                            invalidKey.add(k);
                        }
                    } else {
                        validKey.add(k);
                    }
                });
                invalidKey.stream().forEach(removeKey -> {
                    logger.info(LOG.LOG_PRE + "release invalid SessionID:" + LOG.LOG_PRE, this.getServerName(), removeKey);
                    sessionMap.remove(removeKey);
                    invalidSessionMap.remove(removeKey);
                });
                validKey.stream().forEach(addKey -> {
                    logger.info(LOG.LOG_PRE + "add valid SessionID:" + LOG.LOG_PRE, this.getServerName(), addKey);
                    invalidSessionMap.remove(addKey);
                });
                invalidKey.clear();
                validKey.clear();
                endTime = System.currentTimeMillis();
            }
        });
        invalidResourceExecutor.setDaemon(true);
        invalidResourceExecutor.start();
    }

    public synchronized void close(Attachment att, Object target) {
        try {
            if (att.getClient().isOpen()) {
                logger.info(LOG.LOG_POS + "will be closed!", this.getServerName(), att.getClient().getRemoteAddress());
                att.getClient().close();
            }
        } catch (IOException e) {
            logger.error(LOG.LOG_POS + "close client" + LOG.LOG_POS, this.getServerName(), target, LOG.EXCEPTION_DESC, e);
        }
    }
}
