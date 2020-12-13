package pers.pandora.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PoolConnection {

    private boolean isBusy;

    private Connection connection;

    private int transNew = -1;

    public int getTransNew() {
        return transNew;
    }

    public void setTransNew(int transNew) {
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
