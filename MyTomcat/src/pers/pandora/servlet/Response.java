package pers.pandora.servlet;

import pers.pandora.bean.Pair;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.server.Session;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Response {

    private String servlet;
    private String charset = "utf-8";
    //存储正文长度
    private int len;
    //正文
    private StringBuilder content;
    //资源类型
    private String type = "text/html";
    private RequestMappingHandler requestMappingHandler;
    //静态资源地址
    private String resource;
    private Session session;
    //HTTP Response Status
    private int code;
    private String filePath = "./WebRoot";
    public static final String PLAIN = "MODELANDVIEW_REQUEST_FORWARD_PLAIN";
    private static final char CRLF = '\n';
    private static final char BLANK = ' ';
    private static final String SERVER = "Server";
    private static final String DATE = "Date";
    private static final String CONTENTTYPE = "Content-type";
    private static final String CONTENTLENGTH = "Content-Length";
    private static final String SET_COOKIE = "Set-Cookie";
    private static final char RESPONSE_SPLITER = ':';

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private Map<String, String> heads;

    public Response() {
        heads = new HashMap<>();
    }

    public Map<String, String> getHeads() {
        return heads;
    }

    public boolean addHeads(String key, String value) {
        if (StringUtils.isNotEmpty(key)) {
            String t = key.toLowerCase();
            if (!t.equals(SERVER.toLowerCase()) && !t.equals(DATE.toLowerCase()) && !t.equals(CONTENTLENGTH.toLowerCase())
                    && !t.equals(CONTENTTYPE.toLowerCase())) {
                this.heads.put(key, value);
                return true;
            } else {
                System.out.println("server,date,content-type,content-length属于固定字段不能添加");
            }
        }
        return false;
    }

    public void setRequestMappingHandler(RequestMappingHandler requestMappingHandler) {
        this.requestMappingHandler = requestMappingHandler;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setType(String type) {
        if (type == null && servlet == null) {
            content = new StringBuilder("请求URI参数出错");
        }
        if (type != null) {
            this.type = type;
        }
    }

    public String getType() {
        return type;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    public void setServlet(String servlet) {
        if (StringUtils.isNotEmpty(servlet) && !servlet.contains(":")) {
            this.servlet = servlet;
        }
    }

    public String getServlet() {
        return servlet;
    }

    /**
     * 构建响应头
     */
    private StringBuilder createHeadInfo(List<Cookie> cookies) {
        StringBuilder headInfo = new StringBuilder();
        //1.http协议版本，状态代码，描述
        headInfo.append("HTTP/1.1").append(BLANK).append(code).append(BLANK);
        switch (code) {
            case 200:
                headInfo.append("OK");
                break;
            case 404:
                headInfo.append("Not Found");
                break;
            case 500:
                headInfo.append("Server Error");
                break;
            case 302:
                headInfo.append("Found");
                break;
            default:
                headInfo.append("Error Code");
        }
        headInfo.append(CRLF);
        //2.响应头
        headInfo.append("Server: Pandora Server/1.0.1").append(CRLF);
        headInfo.append("Date: ").append(new Date()).append(CRLF);
        headInfo.append("Content-type: " + type + ";charset=utf8").append(CRLF);
        //正文长度，字节长度
        headInfo.append("Content-Length: ").append(len).append(CRLF);
        for (Map.Entry<String, String> head : heads.entrySet()) {
            headInfo.append(head.getKey() + RESPONSE_SPLITER + head.getValue()).append(CRLF);
        }
        //构建Cookie头
        if (cookies != null && cookies.size() > 0) {
            for (Cookie cookie : cookies) {
                StringBuilder sb = new StringBuilder(SET_COOKIE);
                sb.append(RESPONSE_SPLITER).append(BLANK).append(cookie.getKey()).append(Request.COOKIE_KV_SPLITE).append(cookie.getValue())
                        .append(Request.COOKIE_SPLITER);
                sb.append(RESPONSE_SPLITER).append(BLANK).append("Version").append(Request.COOKIE_KV_SPLITE).append(cookie.getVersion())
                        .append(Request.COOKIE_SPLITER);
                if (cookie.isFlag()) {
                    if (StringUtils.isNotEmpty(cookie.getExpires())) {
                        sb.append(RESPONSE_SPLITER).append(BLANK).append("Expires").append(Request.COOKIE_KV_SPLITE).append(cookie.getExpires())
                                .append(Request.COOKIE_SPLITER);
                    }
                    if (cookie.getMax_age() >= 0) {
                        sb.append(RESPONSE_SPLITER).append(BLANK).append("Max-Age").append(Request.COOKIE_KV_SPLITE).append(cookie.getMax_age())
                                .append(Request.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getDoamin())) {
                        sb.append(RESPONSE_SPLITER).append(BLANK).append("Domain").append(Request.COOKIE_KV_SPLITE).append(cookie.getMax_age())
                                .append(Request.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getDoamin())) {
                        sb.append(RESPONSE_SPLITER).append(BLANK).append("Path").append(Request.COOKIE_KV_SPLITE).append(cookie.getPath())
                                .append(Request.COOKIE_SPLITER);
                    }
                    if (cookie.getSecure() > 0) {
                        sb.append(RESPONSE_SPLITER).append(BLANK).append("secure").append(Request.COOKIE_KV_SPLITE).append(cookie.getSecure())
                                .append(Request.COOKIE_SPLITER);
                    }
                }
                sb.append(CRLF);
                headInfo.append(sb);
            }
        }
        headInfo.append(CRLF);//分隔符
        return headInfo;
    }

    public String handle(String method, Request request) {
        if (content == null) {
            content = new StringBuilder();
        }
        //response session以request session为准
        session = request.getSession();
        handlePre(request);
        try {
            if (request != null && request.getParams().containsKey(PLAIN)) {
                type = "text/plain";
                content.append(request.getParams().get(Response.PLAIN).get(0));//保留JSON序列化
                len = content.toString().getBytes(charset).length;
                code = 200;
            } else if (StringUtils.isNotEmpty(servlet)) {
                Map<String, List<Object>> params = request.getParams();
                //初始化对象赋值只支持基本数据类型和String类型
                Servlet handler = ClassUtils.getClass(servlet, params);
                initRequstObjectList(request.getObjectList(), handler);
                if (handler != null) {
                    if (method.equals("GET")) {
                        content.append(handler.doGet(request, this));
                    } else if (method.equals("POST")) {
                        content.append(handler.doPost(request, this));
                    }
                    len = content.toString().getBytes(charset).length;
                    if (code <= 0) {
                        code = 200;
                    }
                } else {
                    handle_404_NOT_FOUND();
                }
            } else if (resource == null) {
                handle_404_NOT_FOUND();
            } else {
                File file = new File(filePath + resource);
                len += file.length();
                code = file.exists() ? 200 : 404;
            }
        } catch (Exception e) {
            System.out.println("当前编码不支持:" + charset);
        }
        handleAfter(request);
        return createHeadInfo(request.isUpdateCookie() ? request.getCookies() : null).append(content).toString();
    }

    private boolean handleAfter(Request request) {
        for (Pair<Integer, Interceptor> interceptor : requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().afterMethod(request, this)) {
                return false;
            }
        }
        return true;
    }

    private boolean handlePre(Request request) {
        for (Pair<Integer, Interceptor> interceptor : requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().preMethod(request, this)) {
                return false;
            }
        }
        return true;
    }

    private void handle_404_NOT_FOUND() throws UnsupportedEncodingException {
        content.append("页面找不到!");
        type = "text/plain";
        len = content.toString().getBytes(charset).length;
        code = 404;
    }

    private void initRequstObjectList(Map<String, Object> objectList, Servlet handler) {
        ClassUtils.initWithObjectList(objectList, handler);
    }


    void reset() {
        servlet = null;
        type = "text/html";
        resource = null;
        content = null;
        len = 0;
    }
}
