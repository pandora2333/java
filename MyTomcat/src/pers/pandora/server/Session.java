package pers.pandora.server;

import pers.pandora.utils.IdWorker;
import pers.pandora.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Session {

    //全局session唯一ID
    private String sessionID;
    //session会话过期时间
    private AtomicInteger max_age = new AtomicInteger(-1);//默认不过期
    //Session Id生成器
    private static IdWorker idWorker = new IdWorker();

    public Session(){
        sessionID = idWorker.nextSessionID();
    }

    private Map<String,Object> attrbuites = new ConcurrentHashMap<>();

    public Map<String, Object> getAttrbuites() {
        return attrbuites;
    }

    public void addAttrbuites(String key,Object value) {
        if(StringUtils.isNotEmpty(key)){
            attrbuites.put(key,value);
        }
    }

    public void setMax_age(int max_age) {
        if(max_age > 0) {
            this.max_age.set(max_age);
        }
    }

    public int getMax_age() {
        return max_age.get();
    }

    public int invalidTime(){
        return max_age.decrementAndGet();
    }
    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void invalid(){
        attrbuites.clear();
        max_age.set(-1);
        sessionID = null;
    }
}
