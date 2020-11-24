package pers.pandora.servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Cookie {

    private Map<String,String> cookies = new ConcurrentHashMap<>();

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public void clear(){
        cookies = new ConcurrentHashMap<>();
    }
}
