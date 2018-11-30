package pers.pandora;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PoolConnection {
	private boolean isBusy;
	private Connection connection;
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
	public ResultSet queryForDB(String sql) throws SQLException{
		 Statement state = getConnection().createStatement();
		 ResultSet rs = state.executeQuery(sql);
		return rs;
	}

}
