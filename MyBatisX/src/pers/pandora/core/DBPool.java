package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DBPool {

    private static Logger logger = LogManager.getLogger(DBPool.class.getName());
    //Connection pool initialization CONNECTIONS
    private int initalSize;
    //Database connection pool maximum CONNECTIONS
    private int maxSize;

    private AtomicInteger len = new AtomicInteger(0);
    //Database connection URI identifier
    private String url;
    private String user;
    private String password;
    //Database driven
    private String driver;
    //Maintain connection pool
    private BlockingQueue<PoolConnection> connections;

    public static final String INITALSIZE = "initalSize";

    public static final String MAXSIZE = "maxSize";

    public static final String TIMEOUT = "timeOut";

    public static final String URL = "url";

    public static final String USER = "user";

    public static final String PASSWORD = "password";

    public static final String DRIVER = "driver";

    private final Properties properties = new Properties();

    private DataBaseCoder dataBaseCoder;

    private String dbProperties;

    public DataBaseCoder getDataBaseCoder() {
        return dataBaseCoder;
    }

    public void setDataBaseCoder(DataBaseCoder dataBaseCoder) {
        this.dataBaseCoder = dataBaseCoder;
    }

    public String getDbProperties() {
        return dbProperties;
    }

    public int size(){
        return len.get();
    }

    public DBPool(String dbProperties) {
        this.dbProperties = dbProperties;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dbProperties);
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("config file" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    public boolean init(){
        try {
            initalSize = Integer.valueOf(properties.getProperty(INITALSIZE, null));
            maxSize = Integer.valueOf(properties.getProperty(MAXSIZE, null));
            url = properties.getProperty(URL);
            user = properties.getProperty(USER);
            password = properties.getProperty(PASSWORD);
            if(dataBaseCoder != null){
                url = dataBaseCoder.decodeUrl(url);
                user = dataBaseCoder.decodeUserName(user);
                password = dataBaseCoder.decodePassword(password);
            }
            driver = properties.getProperty(DRIVER);
        } catch (NumberFormatException e) {
            logger.error("format number" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        if (initalSize <= 0 || maxSize <= 0 || maxSize < initalSize) {
            logger.error("file config number" + LOG.LOG_POS, LOG.ERROR_DESC);
            return false;
        }
        connections = new ArrayBlockingQueue<>(initalSize);
        try {
            Class.forName(driver);
            PoolConnection connection;
            for (int i = 0; i < initalSize; i++) {
                connection = new PoolConnection();
                assert url != null;
                connection.setConnection(DriverManager.getConnection(url, user, password));
                connection.setBusy(false);
                connections.add(connection);
            }
        } catch (Exception e) {
            logger.error("init-connection" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            return false;
        }
        return true;
    }

    public PoolConnection getConnection() throws SQLException {
        return getConnection(Long.MAX_VALUE);
    }

    //Block acquisition until timeout
    public PoolConnection getConnection(long timeOut) throws SQLException {
        if (connections.remainingCapacity() == 0) {
            rePool();
        }
        PoolConnection connection = null;
        try {
            connection = connections.poll(timeOut,TimeUnit.MILLISECONDS);
            if(connection != null) {
                connection.setBusy(true);
            }
        } catch (InterruptedException e) {
            //ignore
        }
        return connection;
    }

    private synchronized void rePool() throws SQLException {
        assert connections != null;
        if(len.get() == maxSize){
            return;
        }
        int upSize = Math.min(len.get() << 1,maxSize);
        int remain = upSize - len.get();
        PoolConnection connection;
        for (int i = 0;i < remain;i++) {
            connection = new PoolConnection();
            connection.setBusy(false);
            connection.setConnection(DriverManager.getConnection(url, user, password));
            try {
                connections.put(connection);
            } catch (InterruptedException e) {
                //ignore
                i--;
            }
        }
        len.set(upSize);
    }
    //When it's closed, the pool won't work,it will be forced closure,and it means it's not secure
    public synchronized void close() throws SQLException {
        if (connections != null) {
            while(connections.remainingCapacity() > 0){
                Objects.requireNonNull(connections.poll()).getConnection().close();
            }
            connections = null;
            len = null;
        }
    }

    public void commit(PoolConnection connection) throws SQLException {
        if (connection != null) {
            if (!connection.getConnection().isClosed() && connection.isBusy()) {
                connection.setBusy(false);
                try {
                    connections.put(connection);
                } catch (InterruptedException e) {
                    //ignore
                    connection.getConnection().close();
                    len.decrementAndGet();
                }
            }
        }
    }
}
