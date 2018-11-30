package pers.pandora.core;


public class SqlSession {
    private String mapper;
    public SqlSession(String mapper) {
        this.mapper = mapper;
    }
    public <T> T createMapper(Class<T> clazz){
        try {
            return Configuration.createMapperProxy(mapper,clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
