package pers.pandora.orm.core;

public interface CacheFactory {

    void put(String key,Object object);

    Object get(String key);

    boolean containsKey(String key);

    void removeKey(String key);

    String createKey(String tableName,String sql);
}
