package pers.pandora.core;

public class SqlSessionFactory {

    private Configuration configuration;

    private MapperProxyClass mapperProxyClass;

    public SqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
        mapperProxyClass  = new MapperProxyClass();
        mapperProxyClass.setConfiguration(configuration);
    }

    public SqlSession createSqlSession(String mapper) {
        return new SqlSession(mapper, mapperProxyClass);
    }
}
