package pers.pandora.core;

public class SqlSessionFactory {

    private Configuration configuration;

    public SqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    public SqlSession createSqlSession(String mapper) {
        MapperProxyClass mapperProxyClass = new MapperProxyClass();
        mapperProxyClass.setConfiguration(configuration);
        mapperProxyClass.setDbPool(new DBPool(configuration.getDbPoolProperties()));
        return new SqlSession(mapper, mapperProxyClass);
    }
}
