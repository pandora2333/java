package pers.pandora.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Session {

    private Map<String,Object> attrbuites = new ConcurrentHashMap<>();

    public Map<String, Object> getAttrbuites() {
        return attrbuites;
    }

    public void setAttrbuites(Map<String, Object> attrbuites) {
        this.attrbuites = attrbuites;
    }
    public void clear(){
        attrbuites = new ConcurrentHashMap<>();
    }
}
