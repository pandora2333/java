package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.utils.StringUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

public class DBPool {

    private static Logger logger = LogManager.getLogger(DBPool.class);
    //Connection pool initialization CONNECTIONS
    private int initalSize;
    //Database connection pool maximum CONNECTIONS
    private int maxSize;
    //Connection get timeOut threshold
    private long timeOut;
    //Database connection URI identifier
    private String url;
    private String user;
    private String password;
    //Database driven
    private String driver;
    //Maintain connection pool
    private PoolConnection[] connections;
    //Request number determination, capacity expansion decision
    private int threshold;

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

    public DBPool(String dbProperties) {
        assert StringUtil.isNotEmpty(dbProperties);
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
            timeOut = Long.valueOf(properties.getProperty(TIMEOUT));
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
        if (initalSize <= 0 || maxSize <= 0 || maxSize < initalSize || timeOut <= 0) {
            logger.error("file config number" + LOG.LOG_POS, LOG.ERROR_DESC);
            return false;
        }
        connections = new PoolConnection[initalSize];
        try {
            Class.forName(driver);
            for (int i = 0; i < connections.length; i++) {
                if (connections[i] == null) {
                    connections[i] = new PoolConnection();
                    assert url != null;
                    connections[i].setConnection(DriverManager.getConnection(url, user, password));
                    connections[i].setBusy(false);
                }
            }
        } catch (Exception e) {
            logger.error("init-connection" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            return false;
        }
        return true;
    }

    public PoolConnection getConnection() throws SQLException {
        return getConnection(timeOut);
    }

    //Block acquisition until timeout
    public synchronized PoolConnection getConnection(long millis) throws SQLException {
        if (initalSize <= 0) {
            rePool();
        }
        long first = new Date().getTime();
        PoolConnection connection = null;
        threshold++;
        while (new Date().getTime() - first <= millis) {
            for (PoolConnection tcursor : connections) {
                if (tcursor != null && tcursor.getConnection() != null
                        && !tcursor.getConnection().isClosed() && !tcursor.isBusy()) {
                    initalSize--;
                    connection = tcursor;
                    tcursor.setBusy(true);
                    break;
                }
            }
        }
        return connection;
    }

    private void rePool() throws SQLException {
        int upSize = connections.length << 1;
        if (maxSize - connections.length >= 0 && threshold > upSize) {
            PoolConnection[] temp;
            upSize++;
            if (upSize < maxSize) {
                maxSize -= upSize;
                temp = new PoolConnection[upSize];
            } else {
                temp = new PoolConnection[maxSize];
            }
            int cursor = 0;
            for (PoolConnection connection : connections) {
                if (connection != null && connection.getConnection() != null && !connection.getConnection().isClosed()) {
                    temp[cursor] = connection;
                } else {
                    temp[cursor] = new PoolConnection();
                    temp[cursor].setConnection(DriverManager.getConnection(url, user, password));
                    temp[cursor].setBusy(false);
                }
                cursor++;
            }
            connections = temp;
        }
    }

    public synchronized void close() throws SQLException {
        if (connections != null) {
            for (PoolConnection connection : connections) {
                connection.getConnection().close();
            }
            connections = null;
        }
    }

    public void commit(PoolConnection connection) throws SQLException {
        if (connection != null) {
            if (!connection.getConnection().isClosed() && connection.isBusy()) {
                connection.setBusy(false);
            } else if (connection.getConnection().isClosed()) {
                connection.setConnection(null);
            }
        }
    }
}
