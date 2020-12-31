package pers.pandora.core;

public class SqlSessionFactory {

    private Configuration configuration;

    private MapperProxyHandler mapperProxyHandler;

    public void setCacheFactory(CacheFactory cacheFactory) {
        mapperProxyHandler.setCacheFactory(cacheFactory);
    }

    public SqlSessionFactory(final Configuration configuration) {
        this.configuration = configuration;
        mapperProxyHandler = new MapperProxyHandler();
        mapperProxyHandler.setConfiguration(configuration);
    }

    public SqlSession createSqlSession(String mapper) {
        return new SqlSession(mapper, mapperProxyHandler);
    }
}
