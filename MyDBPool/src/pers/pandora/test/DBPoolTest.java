package pers.pandora.test;

import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;
import pers.pandora.DBPool;
import pers.pandora.PoolConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Scanner;

public class DBPoolTest {
    @Test
    public void test() {
        DBPool dbpool = DBPool.getDBPool();
        long start = Instant.now().toEpochMilli();
        int count = 0;
        for (int i = 0; i < 10000; i++) {
            try {
                PoolConnection connection = dbpool.getConnection();
                if (connection != null) {
                    ResultSet rs = connection.queryForDB("select * from dept where deptno=" + i);
                    if (rs.next()) {
                        System.out.println(rs.getString("dname"));
                    }
                    //dbpool.commit(connection);
                } else {
                    count++;
                }
//                try {
//                    dbpool.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("耗时:" + (end - start));//187 //1102 //10179 //104205 //103898-》//654 //1124
        System.out.println("废弃连接:" + count);
        try {
            dbpool.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
