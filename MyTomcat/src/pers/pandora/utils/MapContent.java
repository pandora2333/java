package pers.pandora.utils;

import java.util.LinkedList;
import java.util.List;

public final class MapContent {
    private String className;
    private List<String> urls;
    public MapContent(){
        urls = new LinkedList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
