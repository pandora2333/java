package pers.pandora.core;

import pers.pandora.utils.IdWorker;
import pers.pandora.utils.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Session implements Serializable {

    private static final long serialVersionUID = 3604972003323896788L;

    //全局session唯一ID
    private String sessionID;
    //session会话过期时间
    private AtomicInteger max_age = new AtomicInteger(0);//默认Session级别过期
    //Session Id生成器
    private static final IdWorker idWorker = new IdWorker();

    private AtomicBoolean isValid = new AtomicBoolean(false);

    public Session() {
        sessionID = idWorker.nextSessionID();
    }

    public boolean getIsValid() {
        return isValid.get();
    }

    private Map<String, Object> attrbuites = new ConcurrentHashMap<>();

    public Map<String, Object> getAttrbuites() {
        return attrbuites;
    }

    public void addAttrbuites(String key, Object value) {
        if (StringUtils.isNotEmpty(key)) {
            attrbuites.put(key, value);
        }
    }

    //如果需要让Session失效，请设置过期时间后调用Request#addInvalidSession，不然Session不会失效
    public void setMax_age(int max_age) {
        this.max_age.set(max_age);
        if (max_age > 0) {
            isValid.set(true);
        } else {
            isValid.set(false);
        }
    }

    public int getMax_age() {
        return max_age.get();
    }

    public int invalidTime() {
        return max_age.decrementAndGet();
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void clearCache() {
        attrbuites.clear();
        max_age.set(0);
        isValid.set(false);
    }
}
