package pers.pandora.core;

public class SqlSessionFactory {

    public static  SqlSession createSqlSession(String mapper){
        return new SqlSession(mapper);
    }
}
