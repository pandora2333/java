package pers.pandora.orm.core;

import pers.pandora.common.utils.StringUtils;
import pers.pandora.orm.constant.ENTITY;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMapCacheFactory implements CacheFactory {

    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>(16);

    @Override
    public void put(final String key, final Object object) {
        if (!StringUtils.isNotEmpty(key)) {
            return;
        }
        final String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
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
    public Object get(final String key) {
        if (!StringUtils.isNotEmpty(key)) {
            return null;
        }
        final String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
        assert ss.length == 2;
        final Map<String, Object> map = cache.get(ss[0]);
        return map != null ? map.get(ss[1]) : null;
    }

    @Override
    public boolean containsKey(final String key) {
        return get(key) != null;
    }

    @Override
    public void removeKey(final String key) {
        if (!StringUtils.isNotEmpty(key)) {
            return;
        }
        final String[] ss = key.split(String.valueOf(ENTITY.SEMICOLON), -1);
        assert ss.length == 2;
        cache.remove(ss[0]);
    }

    @Override
    public String createKey(final String tableName, final String sql) {
        return tableName + ENTITY.SEMICOLON + sql;
    }
}
