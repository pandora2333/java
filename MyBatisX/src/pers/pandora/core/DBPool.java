package pers.pandora.core;

import pers.pandora.core.utils.PropUtils;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

/**
 * author:by pandora
 * version 1.1
 * up date:2018//11/20
 * encoding:utf8
 *
 */
public class DBPool {
   private int initalSize;//连接池初始化连接数
   private int maxSize;//数据库连接池最大连接数
   private long timeout;//连接获取超时阀值
   private String url;//数据库连接uri标识
   private String user;//用户名
   private String password;//密码
   private String driver;//数据库驱动
   private volatile PoolConnection[] connections;//维护连接池
   private volatile int request;//请求数判定，扩容判定
   private static DBPool dbpool = new DBPool();//单例模式，懒汉式
   private DBPool() {
      try {
         initalSize = Integer.valueOf(PropUtils.parse("initalSize"));
         maxSize = Integer.valueOf(PropUtils.parse("maxSize"));
         timeout = Long.valueOf(PropUtils.parse("timeout"));
         url = PropUtils.parse("url");
         user = PropUtils.parse("user");
         password = PropUtils.parse("password");
         driver = PropUtils.parse("driver");
      } catch (NumberFormatException e) {
         System.out.println("配置文件数值有误!");
      }
      if(initalSize<=0||maxSize<=0||maxSize<initalSize||timeout<=0){
         throw  new RuntimeException("配置文件数值有误!");
      }
      connections = new PoolConnection[initalSize];
      try {
         Class.forName(driver);
         for (int i = 0;i<connections.length;i++){
            if(connections[i]==null){
               connections[i] = new PoolConnection();
               connections[i].setConnection(DriverManager.getConnection(url,user,password));
               connections[i].setBusy(false);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   //非阻塞获取
   public PoolConnection getConnection() throws SQLException {
      return getConnection(timeout);
   }
   //阻塞获取，直到超时
   public synchronized PoolConnection getConnection(long millis) throws SQLException {
      if(initalSize<=0){
         rePool();
      }
      final long first = new Date().getTime();
      PoolConnection connection = null;
      request++;
      while(new Date().getTime()-first<=millis){
         for(int i = 0;i<connections.length;i++){
            if(connections[i]!=null&&connections[i].getConnection()!=null&&!connections[i].getConnection().isClosed()&&!connections[i].isBusy()){
               initalSize--;
               connection = connections[i];
               connections[i].setBusy(true);
               break;
            }
         }
      }
      return connection;
   }
   private void rePool() throws SQLException {
      if(maxSize-connections.length>=0 && request>connections.length<<1){
         PoolConnection[] temp =null;
         if((connections.length<<1+1)<maxSize){
            maxSize -= connections.length<<1+1;
            temp = new PoolConnection[connections.length<<1+1];
         }else{
            temp = new PoolConnection[maxSize];
         }
         if(temp!=null){
            int cursor = 0;
            for (PoolConnection connection:connections){
               if(connection!=null&&connection.getConnection()!=null&&!connection.getConnection().isClosed()){
                  temp[cursor] = connection;
               }else{
                  temp[cursor] = new PoolConnection();
                  temp[cursor].setConnection(DriverManager.getConnection(url,user,password));
                  temp[cursor].setBusy(false);
               }
               cursor++;
            }
            connections = temp;
         }
      }
   }

   public synchronized void close() throws SQLException {
      for(PoolConnection connection:connections){
         connection.getConnection().close();
      }
      connections = null;
      System.gc();
   }
   public static DBPool getDBPool(){
      return dbpool;
   }
   public void commit(PoolConnection connection) throws SQLException {//每次完成sql操作后要提交
      if (connection!=null) {
         if (!connection.getConnection().isClosed()&& connection.isBusy()){
            connection.setBusy(false);
         }else if(connection.getConnection().isClosed()){
            connection.setConnection(null);
         }
      }
   }
}
