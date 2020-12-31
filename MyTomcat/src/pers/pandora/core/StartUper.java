package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.JSP;
import pers.pandora.constant.LOG;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.IdWorker;
import pers.pandora.utils.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Base on {serverName}.properties pattern
 * Note:The configuration file must be a full path
 */
public final class StartUper {

    private static final Logger logger = LogManager.getLogger(StartUper.class);

    public static final String AOPPATHS = "aopPaths";

    public static final String AOPPROXYFACTORY = "aopProxyFactory";

    public static final String DIPATHS = "diPaths";

    public static final String MVCPATHS = "mvcPaths";

    public static final String HOTLOADJSP = "hotLoadJSP";

    public static final String SERIALSESSIONSUPPORT = "serialSessionSupport";

    public static final String PORT = "port";

    public static final String ROOTPATH = "rootPath";

    public static final String RESOURCEROOTPATH = "resourceRootPath";

    public static final String WEBCONFIGPATH = "webConfigPath";

    public static final String REQUESTFILEDIR = "requestFileDir";

    public static final String MAINPOOLSIZE = "mainPoolSize";

    public static final String SLAVEPOOLSIZE = "slavePoolSize";

    public static final String CAPACTIY = "capcity";

    public static final String RESPONSEBUFFER = "responseBuffer";

    public static final String RECEIVEBUFFER = "receiveBuffer";

    public static final String MAXKEEPCLIENTS = "maxKeepClients";

    public static final String EXPELTIME = "expelTime";

    public static final String GCTIME = "gcTime";

    public static final String WS = "ws";

    public static final String FALSE = "false";

    public static final String WSCLASS = "wsClass";

    public static final String BUSYTIME = "busyTime";

    public static final String MAXUPBITS = "maxUpBits";

    public static final String CHARSET = "charset";

    public static final String LOADMINCORE = "loadMinCore";

    public static final String LOADMAXCORE = "loadMaxCore";

    public static final String LOADKEEPALIVE = "loadKeepAlive";

    public static final String LOADTIMEOUT = "loadTimeout";

    public static final String SESSIONIDGENERATOR = "sessionIDGenerator";

    public static final String JSONCLASS = "jsonClass";

    public static final String RETRYTIME = "retryTime";

    public static final String RETRYCNT = "retryCnt";

    public static final String OPENMSG = "openMsg";

    public static final String CLOSEMSG = "closeMsg";

    public static final String SECURITYDIR = "secuiryDir";

    private String[] paths;

    public StartUper(String... configPaths) {
        assert configPaths != null;
        assert configPaths.length > 0;
        this.paths = configPaths;
    }

    public String[] getPaths() {
        return paths;
    }

    //It should not run in a multi-thread environment
    public void start(final boolean blockingThread) {
        final List<Server> servers = new ArrayList<>(paths.length);
        Properties properties;
        java.io.File file;
        InputStream inputStream = null;
        String serverName;
        int index;
        for (String path : paths) {
            file = new File(path);
            if (!file.exists()) {
                continue;
            }
            properties = new Properties();
            try {
                inputStream = new FileInputStream(file);
                properties.load(inputStream);
                index = file.getName().indexOf(BeanPool.FILE_SPLITER);
                if (index < 0) {
                    index = file.getName().length();
                }
                serverName = file.getName().substring(0, index);
                servers.add(buildServer(properties, serverName));
            } catch (IOException e) {
                logger.error("start" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        servers.stream().filter(Objects::nonNull).forEach(Server::start);
        if (blockingThread) {
            Server.mainLoop();
        }
    }

    private Server buildServer(final Properties properties, final String serverName) {
        final String pattern = properties.getProperty(WS, FALSE);
        String value;
        Server server;
        final BeanPool beanPool = new BeanPool();
        final RequestMappingHandler requestMappingHandler = new RequestMappingHandler();
        value = properties.getProperty(AOPPATHS, null);
        final String separator = String.valueOf(JSP.JAVA_SPLITER);
        if (StringUtils.isNotEmpty(value)) {
            beanPool.setAopPaths(value.split(separator, -1));
        }
        value = properties.getProperty(AOPPROXYFACTORY, null);
        if (StringUtils.isNotEmpty(value)) {
            try {
                beanPool.setAopProxyFactory(ClassUtils.getClass(value, null, true));
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                logger.error("buildServer aopPaths:" + LOG.LOG_PRE + LOG.LOG_POS, value, LOG.EXCEPTION_DESC, e);
                return null;
            }
        }
        value = properties.getProperty(LOADMINCORE, null);
        if (StringUtils.isNotEmpty(value)) {
            int minCore = Integer.valueOf(value);
            requestMappingHandler.setMinCore(minCore);
            beanPool.setMinCore(minCore);
        }
        value = properties.getProperty(LOADMAXCORE, null);
        if (StringUtils.isNotEmpty(value)) {
            int maxCore = Integer.valueOf(value);
            requestMappingHandler.setMaxCore(maxCore);
            beanPool.setMaxCore(maxCore);
        }
        value = properties.getProperty(LOADKEEPALIVE, null);
        if (StringUtils.isNotEmpty(value)) {
            long keepAlive = Long.valueOf(value);
            requestMappingHandler.setKeepAlive(keepAlive);
            beanPool.setKeepAlive(keepAlive);
        }
        value = properties.getProperty(LOADTIMEOUT, null);
        if (StringUtils.isNotEmpty(value)) {
            long timeOut = Long.valueOf(value);
            requestMappingHandler.setTimeOut(timeOut);
            beanPool.setTimeOut(timeOut);
        }
        value = properties.getProperty(DIPATHS, null);
        if (StringUtils.isNotEmpty(value)) {
            beanPool.init(value.split(separator, -1));
        }
        value = properties.getProperty(MVCPATHS, null);
        if (StringUtils.isNotEmpty(value)) {
            requestMappingHandler.setBeanPool(beanPool);
            requestMappingHandler.init(value.split(separator, -1));
        }
        if (pattern.equals(FALSE)) {
            server = new AIOServer();
            value = properties.getProperty(SERIALSESSIONSUPPORT, null);
            if (StringUtils.isNotEmpty(value)) {
                try {
                    server.setSerialSessionSupport(ClassUtils.getClass(value, beanPool, true));
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    logger.error("buildServer serialSessionSupport:" + LOG.LOG_PRE + LOG.LOG_POS, value, LOG.EXCEPTION_DESC, e);
                    return null;
                }
            }
            value = properties.getProperty(SESSIONIDGENERATOR, null);
            if (StringUtils.isNotEmpty(value)) {
                try {
                    IdWorker idWoker = ClassUtils.getClass(value, beanPool, true);
                    server.setIdWorker(idWoker);
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    logger.error("buildServer sessionIDGenerator:" + LOG.LOG_PRE + LOG.LOG_POS, value, LOG.EXCEPTION_DESC, e);
                    return null;
                }
            }
            value = properties.getProperty(HOTLOADJSP, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setHotLoadJSP(Boolean.valueOf(value));
            }
            value = properties.getProperty(ROOTPATH, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setRootPath(value);
            }
            value = properties.getProperty(RESOURCEROOTPATH, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setResourceRootPath(value);
            }
            value = properties.getProperty(WEBCONFIGPATH, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setWebConfigPath(value);
            }
            value = properties.getProperty(REQUESTFILEDIR, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setRequestFileDir(value);
            }
            value = properties.getProperty(EXPELTIME, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setExpelTime(Long.valueOf(value));
            }
            value = properties.getProperty(GCTIME, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setGcTime(Long.valueOf(value));
            }
            value = properties.getProperty(SECURITYDIR, null);
            if (StringUtils.isNotEmpty(value)) {
                server.setSecuiryDir(value);
            }
        } else {
            value = properties.getProperty(WSCLASS, null);
            if (StringUtils.isNotEmpty(value)) {
                try {
                    server = ClassUtils.getClass(value, beanPool, true);
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    logger.error("buildServer wsClass:" + LOG.LOG_PRE + LOG.LOG_POS, value, LOG.EXCEPTION_DESC, e);
                    return null;
                }
            } else {
                server = new WebSocketServer();
            }
            value = properties.getProperty(BUSYTIME, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setBusyTime(Long.valueOf(value));
            }
            value = properties.getProperty(CHARSET, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setCharset(value);
            }
            value = properties.getProperty(RETRYTIME, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setRetryTime(Long.valueOf(value));
            }
            value = properties.getProperty(RETRYCNT, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setRetryCnt(Integer.valueOf(value));
            }
            value = properties.getProperty(OPENMSG, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setOpenMsg(Boolean.valueOf(value));
            }
            value = properties.getProperty(CLOSEMSG, null);
            if (StringUtils.isNotEmpty(value)) {
                ((WebSocketServer) server).setCloseMsg(Boolean.valueOf(value));
            }
        }
        //common configs
        server.setRequestMappingHandler(requestMappingHandler);
        server.setServerName(serverName);
        if (server.getSerialSessionSupport() != null) {
            SerialSessionSupport.getSessionPool().put(server.getServerName(), server.getSessionMap());
        }
        value = properties.getProperty(PORT, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setPort(Integer.valueOf(value));
        }
        value = properties.getProperty(MAINPOOLSIZE, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setMainPoolSize(Integer.valueOf(value));
        }
        value = properties.getProperty(SLAVEPOOLSIZE, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setSlavePoolSize(Integer.valueOf(value));
        }
        value = properties.getProperty(CAPACTIY, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setCapcity(Integer.valueOf(value));
        }
        value = properties.getProperty(RESPONSEBUFFER, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setResponseBuffer(Integer.valueOf(value));
        }
        value = properties.getProperty(RECEIVEBUFFER, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setReceiveBuffer(Integer.valueOf(value));
        }
        value = properties.getProperty(MAXKEEPCLIENTS, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setMaxKeepClients(Integer.valueOf(value));
        }
        value = properties.getProperty(MAXUPBITS, null);
        if (StringUtils.isNotEmpty(value)) {
            server.setMaxUpBits(Long.valueOf(value));
        }
        value = properties.getProperty(JSONCLASS, null);
        if (StringUtils.isNotEmpty(value)) {
            try {
                server.setJsonParser(ClassUtils.getClass(value, beanPool, true));
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                logger.error("buildServer jsonClass:" + LOG.LOG_PRE + LOG.LOG_POS, value, LOG.EXCEPTION_DESC, e);
                return null;
            }
        }
        return server;
    }
}
