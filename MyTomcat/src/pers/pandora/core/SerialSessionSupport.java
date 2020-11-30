package pers.pandora.core;

import pers.pandora.mvc.RequestMappingHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * step1:set serverName
 * step2:server#setSerialSessionSupport add setSerialSessionSupport instance
 * step3:SerialSessionSupport#getSessionPool addKey serverName and addValue Server#getSessionMap reference
 * step4:build a client or test class to use the next methods
 * notes:you can define your own parser by the interface
 */
public abstract class SerialSessionSupport {
    //同机(Java虚拟机)策略
    private static final Map<String, Map<String,Session>> SESSIONPOOL = new ConcurrentHashMap<>(16);
    //Exclude Session
    private static final Set<String> EXCLUDESESSIONS = new CopyOnWriteArraySet<>();
    //生成Session文件
    public static final String SESSIONFILE_POS = "SESSION.ser";
    //session放置目录
    public static final String SESSIONPATH = RequestMappingHandler.ROOTPATH;

    public static Map<String, Map<String, Session>> getSessionPool() {
        return SESSIONPOOL;
    }

    public static Set<String> getExcludeSessions() {
        return EXCLUDESESSIONS;
    }

    //Session序列化
    public abstract void serialSession(String serverName) throws IOException;

    //Session反序列化
    public abstract Map<String,Session> deserialSession(String serverName) throws IOException, ClassNotFoundException;

    //标记不需要删除的Session
    public abstract void excliudeSession(String serverName,String sessionID);

    //删除标记Session
    public abstract void invalidSession(String serverName,String sessionID);
}
