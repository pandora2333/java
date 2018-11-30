package pers.pandora.servlet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pers.pandora.utils.JspParser;
import pers.pandora.utils.MapContent;

public class Request {
    private String msg;
    private String method;
    private Map<String,MapContent> context;
    private Map<String,	List<Object>> params;
    private String reqUrl;
    private String mvcClass;
    private JspParser jspParser;
    public Request(String msg, Map<String,MapContent> context, String mvcClass,JspParser jspParser) {
        this.msg = msg;
        this.context = context;
        this.mvcClass = mvcClass;
        params = new ConcurrentHashMap<>();
        this.jspParser = jspParser;
    }

    public String getReqUrl() {
        return reqUrl;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, List<Object>> getParams() {
        return params;
    }

    public void setParams(Map<String, List<Object>> params) {
        this.params = params;
    }

    public  String handle() {//GET /login HTTP/1.1
        String reqToken = msg.substring(msg.indexOf("/"), msg.indexOf("HTTP")).trim();//GET /login HTTP/1.1
        String tempStr = reqToken;
        if(reqToken.contains("?")) {
        	tempStr = tempStr.replace(tempStr.substring(reqToken.indexOf("?")),"");
        }
        reqUrl = tempStr;//保存请求路径
        if(jspParser!=null){
            jspParser.parse("WebRoot" + reqUrl);
        }
        if (msg.startsWith("GET")) {
            method = "GET";
            if (reqToken.endsWith(".html") || reqToken.endsWith(".htm") || reqToken.endsWith(".jpg") || reqToken.endsWith(".png")) {//对静态资源处理
                return "static:" + reqToken;
            }
            parseParams(reqToken, "GET");
        } else if (msg.startsWith("POST")) {
            method = "POST";
            String param = msg.substring(msg.lastIndexOf("\n")).trim();
            parseParams(param, "POST");
        }
        if(!isMVC(msg)) {
            for (MapContent mapContent : context.values()) {
                if (mapContent.getUrls().contains(reqToken)) {
                    return mapContent.getClassName();
                }
            }
        }else {
            return mvcClass;
        }
        return null;
    }
    private void parseParams(String reqToken,String method) {
        if(method.equals("GET")) {
            if (reqToken.contains("?")) {
                msg = reqToken.substring(0, reqToken.indexOf("?")).trim();
                String[] temp = reqToken.substring(reqToken.indexOf("?") + 1).split("&");
                handleData(temp);
            } else {
                msg = reqToken.trim();
            }
        }else if(method.equals("POST")){
            String[] temp = reqToken.split("&");
            handleData(temp);
        }
    }
    private boolean isMVC(String msg){
        for(MapContent mapContent:context.values()){
            if(mapContent!=null&&mapContent.getClassName().trim().equals(mvcClass)){
                for(String url:mapContent.getUrls()){
                    if(url.contains("*")){
                        url = url.substring(url.indexOf("*")+1);
                    }
                    if(msg.contains(url)){// /*.do //".do"
                        return  true;
                    }
                }
            }
        }
        return false;
    }
    private void handleData(String[] temp){
        for (String str : temp) {
            String[] kv = str.split("=");
            if (kv != null && kv.length == 2) {
                if (params != null) {
                    List<Object> list = params.get(kv[0]);
                    if (list == null) {
                        list = new ArrayList<>();
                        list.add(kv[1]);
                        params.put(kv[0], list);
                    } else {
                        list.add(kv[1]);
                    }
                }
            }
        }
    }
}
