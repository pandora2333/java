package pers.pandora.web.core;



import pers.pandora.om.core.BeanPool;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * step1:set serverName
 * step2:server#setSerializeSessionSupport add setSerializeSessionSupport instance
 * step3:SerializeSessionSupport#getSessionPool addKey serverName and addValue Server#getSessionMap reference
 * step4:build a client or test class to use the next methods
 * notes:you can define your own parser by the interface
 */
public abstract class SerializeSessionSupport {
    //the same JVM strategy
    private static final Map<String, Map<String,Session>> SESSIONPOOL = new ConcurrentHashMap<>(4);
    //exclude Session
    private static final Set<String> EXCLUDESESSIONS = new CopyOnWriteArraySet<>();
    //create session file
    public static final String SESSIONFILE_POS = "SESSION.ser";
    //session file dir
    public static final String SESSIONPATH = BeanPool.ROOTPATH;

    public static Map<String, Map<String, Session>> getSessionPool() {
        return SESSIONPOOL;
    }

    public static Set<String> getExcludeSessions() {
        return EXCLUDESESSIONS;
    }

    public abstract void serializeSession(String serverName) throws IOException;

    public abstract Map<String,Session> deserializeSession(String serverName) throws IOException, ClassNotFoundException;
    //mark the no-deleted session
    public abstract void excliudeSession(String serverName,String sessionID);
    //delete the marks for exclude sessions
    public abstract void invalidSession(String serverName,String sessionID);
}
