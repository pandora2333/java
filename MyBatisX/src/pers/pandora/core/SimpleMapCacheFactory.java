package pers.pandora.core;

import pers.pandora.constant.ENTITY;
import pers.pandora.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMapCacheFactory implements CacheFactory {

    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>(16);

    @Override
    public void put(String key, Object object) {
        if (!StringUtils.isNotEmpty(key)) {
            return;
        }
        String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
        assert ss.length == 2;
        //DCL
        if (!cache.containsKey(ss[0])) {
            synchronized (cache) {
                if (!cache.containsKey(ss[0])) {
                    cache.put(ss[0], new ConcurrentHashMap<>());
                }
            }
        }
        cache.get(ss[0]).put(ss[1], object);
    }

    @Override
    public Object get(String key) {
        if (!StringUtils.isNotEmpty(key)) {
            return null;
        }
        String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
        assert ss.length == 2;
        Map<String, Object> map = cache.get(ss[0]);
        return map != null ? map.get(ss[1]) : null;
    }

    @Override
    public boolean containsKey(String key) {
        return get(key) != null;
    }

    @Override
    public void removeKey(String key) {
        if (!StringUtils.isNotEmpty(key)) {
            return;
        }
        String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
        assert ss.length == 2;
        cache.remove(ss[0]);
    }

    @Override
    public String createKey(String tableName, String sql) {
        return tableName + ENTITY.SEMICOLON + sql;
    }
}
