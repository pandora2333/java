package pers.pandora.web.core;
import pers.pandora.common.utils.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Session implements Serializable {

    private static final long serialVersionUID = 3604972003323896788L;

    //global session only ID
    private String sessionID;
    //expire time after max_age,default time is session level
    private Instant max_age = null;

    private static final String DEFAULTFORMAT = "yyyy-MM-dd HH:mm:ss";

    public Session(final String sessionID) {
        this.sessionID = sessionID;
    }

    private Map<String, Object> attrbuites = new ConcurrentHashMap<>();

    private Cookie sessionCookie;

    public Cookie getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(Cookie sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public Map<String, Object> getAttrbuites() {
        return attrbuites;
    }

    public void addAttrbuites(final String key, final Object value) {
        if (StringUtils.isNotEmpty(key)) {
            attrbuites.put(key, value);
        }
    }

    //if session should invalid,please exec the method and exec Request#addInvalidSession as the same time
    //it set max_age > 0,it will be invalid
    public void setMax_age(int max_age) {
        assert  sessionCookie != null;
        if (max_age > 0) {
            this.max_age = Instant.now();
            this.max_age = this.max_age.plusSeconds(max_age);
            sessionCookie.setMax_age(max_age);
        } else {
            this.max_age = null;
            sessionCookie.setMax_age(-1);
        }
        sessionCookie.setNeedUpdate(true);
    }

    public String getMax_ageByFormatter(String format) {
        if (max_age == null) {
            return null;
        }
        if(!StringUtils.isNotEmpty(format)){
            format = DEFAULTFORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return formatter.format(max_age);
    }

    public Instant getMax_age() {
        return max_age;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void clearCache() {
        attrbuites.clear();
        max_age = null;
    }
}
