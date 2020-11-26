package pers.pandora.servlet;

import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.JspParser;
import pers.pandora.utils.StringUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Response {

    private String servlet;
    //存储头信息
    private StringBuilder headInfo;
    private String charset = "utf-8";
    //存储正文长度
    private int len;
    //正文
    private StringBuilder content;
    //资源类型
    private String type = "text/html";
    //静态资源地址
    private String resource;
    private String filePath = "./WebRoot";
    public static final String PLAIN = "MODELANDVIEW_REQUEST_FORWARD_PLAIN";
    public static final String NULL = "";
    private static final String CRLF = "\n";
    private static final String BLANK = " ";
    private static final String SERVER = "Server";
    private static final String DATE = "Date";
    private static final String CONTENTTYPE = "Content-type";
    private static final String CONTENTLENGTH = "Content-Length";
    private static final char RESPONSE_SPLITER = ':';

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
            if (!t.equals(SERVER.toLowerCase()) && !t.equals(DATE.toLowerCase()) &&
                    !t.equals(CONTENTLENGTH.toLowerCase()) && !t.equals(CONTENTTYPE.toLowerCase())) {
                this.heads.put(key, value);
                return true;
            } else {
                System.out.println("server,date,content-type,content-length属于固定字段不能添加");
            }
        }
        return false;
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
        this.type = type;
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
    private void createHeadInfo(int code) {
        headInfo = new StringBuilder();
        //1.http协议版本，状态代码，描述
        headInfo.append("HTTP/1.1").append(BLANK).append(code).append(BLANK);
        switch (code) {
            case 200:
                headInfo.append("OK");
                break;
            case 404:
                headInfo.append("not found");
                break;
            case 500:
                headInfo.append("server error");
                break;
            default:
                headInfo.append("error code");
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
//        headInfo.append("Transfer-Encoding: ").append(charset).append(CRLF);
        headInfo.append(CRLF);//分隔符
    }

    public String handle(String method, Request request) {
        if (content == null) {
            content = new StringBuilder();
        }
        try {
            if (request != null && request.getParams().containsKey(PLAIN)) {
                type = "text/plain";
                content.append(request.getParams().get(Response.PLAIN).get(0));//保留JSON序列化
                len = content.toString().getBytes(charset).length;
                createHeadInfo(200);
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
                    createHeadInfo(200);
                } else {
                    handle_404_NOT_FOUND();
                }
            } else if (resource == null) {
                handle_404_NOT_FOUND();
            } else {
                File file = new File(filePath + resource);
                len += file.length();
                createHeadInfo(file.exists() ? 200 : 404);
            }
        } catch (Exception e) {
            System.out.println("当前编码不支持:" + charset);
        }
        return headInfo.append(content).toString();
    }

    private void handle_404_NOT_FOUND() throws UnsupportedEncodingException {
        content.append("页面找不到!");
        type = "text/plain";
        len = content.toString().getBytes(charset).length;
        createHeadInfo(404);
    }

    private void initRequstObjectList(Map<String, Object> objectList, Servlet handler) {
        ClassUtils.initWithObjectList(objectList, handler);
    }


    void reset() {
        servlet = null;
        type = "text/html";
        charset = "utf-8";
        filePath = Dispatcher.ROOTPATH2;
        resource = null;
        headInfo = null;
        content = null;
        len = 0;
    }
}
