package pers.pandora.core;

import java.sql.Connection;
public class PoolConnection {

    private boolean isBusy;

    private Connection connection;

    private byte transNew = -1;

    public byte getTransNew() {
        return transNew;
    }

    public void setTransNew(byte transNew) {
        this.transNew = transNew;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean isBusy) {
        this.isBusy = isBusy;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
