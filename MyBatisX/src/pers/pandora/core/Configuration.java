package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.annotation.*;
import pers.pandora.constant.ENTITY;
import pers.pandora.constant.LOG;
import pers.pandora.utils.StringUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

//Mapper annotation injection class
public final class Configuration {

    private static Logger logger = LogManager.getLogger(Configuration.class);
    //Entity class and attribute corresponding table associated with field alias
    private final Map<String, String> alias = new ConcurrentHashMap<>(16);
    //Transactional Class
    private final Map<Class<?>, Object> transactions = new ConcurrentHashMap<>(16);

    private ThreadPoolExecutor executor;

    private List<Future<Boolean>> result;
    //Thread pool minimum number of cores
    private int minCore = Runtime.getRuntime().availableProcessors();
    //Thread pool maximum number of cores
    private int maxCore = minCore + 5;
    //Thread idle time
    private long keepAlive = 50;
    //Thread idle time unit
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    //Timeout waiting for class loading time
    private long timeOut = 5;
    //Timeout wait class load time unit
    private TimeUnit timeOutUnit = TimeUnit.SECONDS;

    private static final String CONFIGURATION_CLASS = "pers.pandora.core.Configuration";

    private DBPool dbPool;

    public void setDbPool(DBPool dbPool) {
        this.dbPool = dbPool;
    }

    public DBPool getDbPool() {
        return dbPool;
    }

    public Map<String, String> getAlias() {
        return alias;
    }

    public void initThreadPool(int minCore, int maxCore, long keepAlive, TimeUnit timeUnit, long timeout, TimeUnit timeOutUnit) {
        this.minCore = minCore;
        this.maxCore = maxCore;
        this.keepAlive = keepAlive;
        this.timeUnit = timeUnit;
        this.timeOutUnit = timeOutUnit;
        this.timeOut = timeout;
    }

    public int getMinCore() {
        return minCore;
    }

    public void setMinCore(int minCore) {
        this.minCore = minCore;
    }

    public int getMaxCore() {
        return maxCore;
    }

    public void setMaxCore(int maxCore) {
        this.maxCore = maxCore;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void init(String... paths) {
        if (paths == null || paths.length == 0) {
            logger.warn("No path loaded");
            return;
        }
        assert dbPool != null;
        executor = new ThreadPoolExecutor(minCore, maxCore, keepAlive, timeUnit, new LinkedBlockingQueue<>());
        result = new ArrayList<>(10);
        for (String path : paths) {
            scanFile(checkPath(path));
        }
        waitFutures(result, timeOut, timeOutUnit);
        executor.shutdownNow();
        executor = null;
        result = null;
    }

    private void waitFutures(final List<Future<Boolean>> result, final long timeOut, final TimeUnit timeOutUnit) {
        for (Future future : result) {
            try {
                future.get(timeOut, timeOutUnit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("waitFutures" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
            }
        }
    }

    private String checkPath(String path) {
        if (!path.startsWith(ENTITY.ROOTPATH)) {
            path = ENTITY.ROOTPATH + path;
        }
        return path.replaceAll(ENTITY.FILE_REGEX_SPLITER, String.valueOf(ENTITY.SLASH));
    }

    private void scanFile(final String path) {
        final File files = new File(path);
        if (files.exists()) {
            if (files.isDirectory()) {
                for (File file : Objects.requireNonNull(files.listFiles())) {
                    scanFile(file.getPath());
                }
            } else {
                if (files.getPath().endsWith(ENTITY.POINT + ENTITY.FILE_POS_MARK)) {
                    final String className = files.getPath().substring(4).replace(ENTITY.POINT + ENTITY.FILE_POS_MARK, LOG.NO_CHAR)
                            .replace(ENTITY.PATH_SPLITER_PATTERN, ENTITY.POINT);
                    if (!className.equals(CONFIGURATION_CLASS)) {
                        result.add(executor.submit(new IOTask(className)));
                    }
                }
            }
        }
    }

    private <T> void scanBean(final Class<T> tClass, final Field field, final Class template) {
        if (template == Table.class) {
            if (tClass.isAnnotationPresent(template)) {
                scanField(tClass);
            }
        } else {
            if (field != null) {
                try {
                    String fieldValue = null;
                    if (template == Id.class) {
                        Id id = field.getAnnotation(Id.class);
                        if (id != null) {
                            fieldValue = id.value();
                        }
                    } else {
                        final Column column = field.getAnnotation(Column.class);
                        if (column != null) {
                            fieldValue = column.value();
                        }
                    }
                    if (StringUtil.isNotEmpty(fieldValue)) {
                        alias.put(fieldValue, field.getName());
                    }
                } catch (Exception e) {
                    logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
                }
            }
        }
    }

    private void scanField(Class tClass) {
        try {
            for (Field field : tClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    scanBean(tClass, field, Id.class);
                } else if (field.isAnnotationPresent(Column.class)) {
                    scanBean(tClass, field, Column.class);
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }

    private class IOTask implements Callable<Boolean> {

        private String className;

        IOTask(String className) {
            this.className = className;
        }

        @Override
        public Boolean call() {
            try {
                scanBean(Class.forName(className, true, Thread.currentThread().getContextClassLoader()), null, Table.class);
            } catch (Exception e) {
                logger.error(LOG.LOG_PRE + "exec for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                        this, className, LOG.EXCEPTION_DESC, e.getCause());
                return false;
            }
            return true;
        }
    }
}
