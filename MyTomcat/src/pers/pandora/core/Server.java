package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.IdWorker;
import pers.pandora.utils.StringUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class Server {

    protected static final Logger logger = LogManager.getLogger(Server.class.getName());

    private int port = 8080;

    private String rootPath = "./WebRoot";
    //JSP in the private directory needs to be forwarded to access
    private String secuiryDir = "/WEB-INF/";

    private String resourceRootPath = "/static/";

    private String webConfigPath = rootPath + secuiryDir + "web.xml";

    public String requestFileDir = resourceRootPath + "files/";

    public static final String OVERMAXUPBITS = "Maximum allowable transmission bit exceeded";

    public static final String DEFAULTSERVER = "PandoraWeb";
    //serer name，it use logs,session file,etc
    private String serverName = DEFAULTSERVER + System.currentTimeMillis();
    //main thread pool size
    private int mainPoolMinSize = Runtime.getRuntime().availableProcessors() + 1;

    private int mainPoolMaxSize = 2 * mainPoolMinSize;

    private long mainPoolKeepAlive = 60L;

    //slave thread pool size
    private int slavePoolMinSize = mainPoolMinSize;

    private int slavePoolMaxSize = mainPoolMaxSize;

    private long slavePoolKeepAlive = 60L;
    //up file buffer size (byte)
    private int receiveBuffer = 8192;
    //Allowed maximum number of pending tcp connections
    private int backLog = 50;
    //Waiting queue in thread pool
    private int queueSize = 200;

    private static final String HOST = "127.0.0.1";
    //hot load JSP default true
    private boolean hotLoadJSP = true;
    //download resource buffer size（byte）
    private int responseBuffer = 8192;
    //tcp receive buffer size, it should greater than or equal to receiveBuffer
    private int tcpReceivedCacheSize = Math.max(65536, receiveBuffer);
    //global session pool,base on memory
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(16);
    //set invalid time for the sessions,optimize the thread scanning
    private Map<String, Session> invalidSessionMap = new ConcurrentHashMap<>(16);
    //session serializer,default it is not supported
    private SerialSessionSupport serialSessionSupport;
    //set mvc-pattern paths
    private RequestMappingHandler requestMappingHandler;
    //JSON_TYPE parser
    private JSONParser jsonParser;

    private Map<String, String> context;
    //browser build the tcps
    private Map<String, Attachment> keepClients = new ConcurrentHashMap<>(16);
    //Allowed maximum number of transmitted information bits
    private long maxUpBits = 1024 * 1024 * 100;

    protected ExecutorService mainPool;

    protected ExecutorService slavePool;

    private long expelTime;

    private long gcTime;
    //SessionId Generator
    private IdWorker idWorker;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getMainPoolMaxSize() {
        return mainPoolMaxSize;
    }

    public int getSlavePoolMaxSize() {
        return slavePoolMaxSize;
    }

    public long getMainPoolKeepAlive() {
        return mainPoolKeepAlive;
    }

    public long getSlavePoolKeepAlive() {
        return slavePoolKeepAlive;
    }

    public void setMainPoolKeepAlive(long mainPoolKeepAlive) {
        this.mainPoolKeepAlive = mainPoolKeepAlive;
    }

    public void setMainPoolMaxSize(int mainPoolMaxSize) {
        this.mainPoolMaxSize = mainPoolMaxSize;
    }

    public void setSlavePoolKeepAlive(long slavePoolKeepAlive) {
        this.slavePoolKeepAlive = slavePoolKeepAlive;
    }

    public void setSlavePoolMaxSize(int slavePoolMaxSize) {
        this.slavePoolMaxSize = slavePoolMaxSize;
    }

    public int getBackLog() {
        return backLog;
    }

    public void setBackLog(int backLog) {
        this.backLog = backLog;
    }

    public String getSecuiryDir() {
        return secuiryDir;
    }

    public void setSecuiryDir(String secuiryDir) {
        this.secuiryDir = secuiryDir;
    }

    public void setMaxUpBits(long maxUpBits) {
        this.maxUpBits = maxUpBits;
    }

    public long getMaxUpBits() {
        return maxUpBits;
    }

    public JSONParser getJsonParser() {
        return jsonParser;
    }

    public void setJsonParser(JSONParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public IdWorker getIdWorker() {
        return idWorker;
    }

    public void setIdWorker(IdWorker idWorker) {
        this.idWorker = idWorker;
    }

    public long getGcTime() {
        return gcTime;
    }

    public void setGcTime(long gcTime) {
        this.gcTime = gcTime;
    }

    public long getExpelTime() {
        return expelTime;
    }

    public void setExpelTime(long expelTime) {
        this.expelTime = expelTime;
    }

    public int getSlavePoolMinSize() {
        return slavePoolMinSize;
    }

    public void setSlavePoolMinSize(int slavePoolMinSize) {
        this.slavePoolMinSize = slavePoolMinSize;
    }

    public boolean isHotLoadJSP() {
        return hotLoadJSP;
    }

    public void setHotLoadJSP(boolean hotLoadJSP) {
        this.hotLoadJSP = hotLoadJSP;
    }

    public void addClients(final String ip, final Attachment attachment) {
        if (StringUtils.isNotEmpty(ip) && attachment != null) {
            this.keepClients.put(ip, attachment);
        }
    }

    public RequestMappingHandler getRequestMappingHandler() {
        return requestMappingHandler;
    }

    public void setRequestMappingHandler(RequestMappingHandler requestMappingHandler) {
        this.requestMappingHandler = requestMappingHandler;
    }

    public Map<String, Attachment> getKeepClients() {
        return keepClients;
    }

    //You should call this method after you start the server
    protected static void mainLoop() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.error(LOG.LOG_PRE + "mainLoop" + LOG.LOG_POS, Thread.currentThread().getName(), LOG.EXCEPTION_DESC, e);
        }
    }

    public void setTcpReceivedCacheSize(int tcpReceivedCacheSize) {
        this.tcpReceivedCacheSize = tcpReceivedCacheSize;
    }

    public int getTcpReceivedCacheSize() {
        return tcpReceivedCacheSize;
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

    public boolean addInvalidSessionMap(final String key, final Session session) {
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

    public int getPort() {
        return port;
    }

    public int getMainPoolMinSize() {
        return mainPoolMinSize;
    }

    public void setReceiveBuffer(int receiveBuffer) {
        this.receiveBuffer = receiveBuffer;
    }

    public int getReceiveBuffer() {
        return receiveBuffer;
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

    public void setMainPoolMinSize(int mainPoolMinSize) {
        this.mainPoolMinSize = mainPoolMinSize;
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

    public boolean addSessionMap(final String key, final Session session) {
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

    public abstract void start(int port);

    protected void execExpelThread() {
        final Thread invalidResourceExecutor = new Thread(() -> {
            long startTime = 0, endTime = 0;
            while (true) {
                try {
                    //expelTime >= 1s ,it can control in ms time level
                    Thread.sleep(Math.max(0, expelTime - (endTime - startTime)));
                } catch (InterruptedException e) {
                    logger.error(LOG.LOG_PRE + "execExpelThread" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
                }
                final Instant now = Instant.now();
                startTime = now.getEpochSecond();
                invalidSessionMap.forEach((k, v) -> {
                    if (v.getMax_age() != null) {
                        if (now.compareTo(v.getMax_age()) >= 0) {
                            logger.info(LOG.LOG_PRE + "release invalid SessionID:" + LOG.LOG_PRE, getServerName(), k);
                            sessionMap.remove(k);
                            invalidSessionMap.remove(k);
                        }
                    } else {
                        logger.info(LOG.LOG_PRE + "add valid SessionID:" + LOG.LOG_PRE, getServerName(), k);
                        invalidSessionMap.remove(k);
                    }
                });
                endTime = System.currentTimeMillis();
            }
        });
        invalidResourceExecutor.setDaemon(true);
        invalidResourceExecutor.start();
    }

    public void gc() {
        final Thread invalidClientExecutor = new Thread(() -> {
            long startTime = 0, endTime = 0;
            while (true) {
                try {
                    //gcTime should depends on tcp keepalive backet  ,it can control in ms time level
                    Thread.sleep(Math.max(0, gcTime - (endTime - startTime)));
                } catch (InterruptedException e) {
                    logger.error(LOG.LOG_PRE + "gc" + LOG.LOG_POS, getServerName(), LOG.EXCEPTION_DESC, e);
                }
                final Instant now = Instant.now();
                startTime = now.getEpochSecond();
                keepClients.forEach((k, v) -> {
                    if (now.toEpochMilli() - v.getKeepTime().toEpochMilli() >= gcTime) {
                        v.setKeep(false);
                        close(v, this, k);
                    }
                });
                endTime = System.currentTimeMillis();
            }
        });
        invalidClientExecutor.setDaemon(true);
        invalidClientExecutor.start();
    }

    public void close(Attachment att, Object target, String ip) {
        try {
            if (!att.isKeep()) {
//                logger.debug(LOG.LOG_POS + " will be closed!", getServerName(), ip);
                if (StringUtils.isNotEmpty(ip)) {
                    keepClients.remove(ip);
                }
                att.getClient().close();
            }
        } catch (IOException e) {
            logger.error(LOG.LOG_POS + "close client" + LOG.LOG_POS, getServerName(), target, LOG.EXCEPTION_DESC, e);
        }
    }
}
