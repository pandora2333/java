package pers.pandora.servlet;

import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Date;
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
    public static final String PLAIN = "plain";
    public static final String NULL = "";
    private static final String CRLF = "\n";
    private static final String BLANK = " ";

    public Response(String servlet, String type, String resource) {
        if (StringUtils.isNotEmpty(servlet) && !servlet.contains(":")) {
            this.servlet = servlet;
        } else {
            this.type = type;
            this.resource = resource;
        }
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
//        headInfo.append("Transfer-Encoding: ").append(charset).append(CRLF);
        headInfo.append(CRLF);//分隔符
    }

    public String handle(String method,Request request) {
        if (content == null) {
            content = new StringBuilder();
        }
        if (StringUtils.isNotEmpty(servlet)) {
            Servlet handler = ClassUtils.getClass(servlet, Servlet.class);
            if (method.equals("GET")) {
                content.append(handler.doGet(request, this));
            } else if (method.equals("POST")) {
                content.append(handler.doPost(request, this));
            }
            len = content.toString().getBytes().length;
            createHeadInfo(200);
        } else if (resource == null) {
            content.append("页面找不到!");
            len = content.toString().getBytes().length;
            createHeadInfo(404);
        } else {
            File file = new File(filePath + resource);
            len += file.length();
            createHeadInfo(file.exists() ? 200 : 404);
        }
        return headInfo.append(content).toString();
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
