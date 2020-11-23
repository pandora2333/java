package pers.pandora.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pers.pandora.utils.JspParser;
import pers.pandora.utils.MapContent;

public final class Request {
    private String msg;
    private String method;
    private Map<String, MapContent> context;
    private Map<String, List<Object>> params;
    private String reqUrl;
    private String mvcClass;
    private JspParser jspParser;
    private String filePath = Dispatcher.ROOTPATH + "files/";
    private String fileName;
    private String charset = "utf-8";
    private byte[] fileData;

    public static final String FILEMARK = "Content-Type: multipart/form-data; boundary=";
    public static final String FILENAMEMARK = "Content-Disposition: form-data;";
    public static final String CONTENTLENGTH = "Content-Length:";
    public static final String CONTENTTYPE = "Content-Type:";
    public static final String FILENAME = "filename=";
    public static final char ZERO = '0';
    public static final char FILENAMETAIL = '\"';
    public static final char CRLF = '\n';
    public static final String FILEDESCMARK = "--";
    public static final String JSP = ".jsp";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String BLANK = " ";
    public static final String spliter = "\\:";
    public static final String JSON = "json";
    public static final String HTTP1_1 = "HTTP/1.1";
    public static final String HTTP = "HTTP";
    public static final String LINE = "_";

    public Request(String msg, Map<String, MapContent> context, String mvcClass, JspParser jspParser) {
        this.msg = msg;
        this.context = context;
        this.mvcClass = mvcClass;
        params = new ConcurrentHashMap<>();
        this.jspParser = jspParser;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public void setJspParser(JspParser jspParser) {
        this.jspParser = jspParser;
    }

    public JspParser getJspParser() {
        return jspParser;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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


    public void saveFileData() {
        if (fileData == null || fileData.length == 0) {
            throw new RuntimeException("无文件上传!");
        }
        java.io.File path = new java.io.File(filePath);
        if (!path.exists()) {
            path.mkdirs();
        }
        try {
            Files.write(Paths.get(filePath + fileName), fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String handle() {//GET /login HTTP/1.1
        String reqToken = msg.substring(msg.indexOf("/"), msg.indexOf(HTTP)).trim();//GET /login HTTP/1.1
        String tempStr = reqToken;
        if (reqToken.contains("?")) {
            tempStr = tempStr.replace(tempStr.substring(reqToken.indexOf("?")), "");
        }
        reqUrl = tempStr;//保存请求路径
        if (jspParser != null) {
            jspParser.parse(Dispatcher.ROOTPATH2 + reqUrl);
        }
        if (msg.startsWith(GET)) {
            method = GET;
            String type = judgeStatic(reqToken);
            if (type != null) {//对静态资源处理
                return type + reqToken;
            }
            parseParams(reqToken, GET);
        } else if (msg.startsWith(POST)) {
            method = POST;
            String param = msg.substring(msg.lastIndexOf(CRLF)).trim();
            parseParams(param, POST);
        }
        if (!isMVC(msg)) {
            for (MapContent mapContent : context.values()) {
                if (mapContent.getUrls().contains(reqToken)) {
                    return mapContent.getClassName();
                }
            }
        } else {
            return mvcClass;
        }
        return null;
    }

    private String judgeStatic(String reqToken) {
        if (reqToken.endsWith(".html") || reqToken.endsWith(".htm")) return "text/html:";
        if (reqToken.endsWith(".jpg") || reqToken.endsWith(".png") || reqToken.endsWith(".jpeg")) return "image/png:";
        return null;
    }

    private void parseParams(String reqToken, String method) {
        if (method.equals("GET")) {
            if (reqToken.contains("?")) {
                msg = reqToken.substring(0, reqToken.indexOf("?")).trim();
                String[] temp = reqToken.substring(reqToken.indexOf("?") + 1).split("&");
                handleData(temp);
            } else {
                msg = reqToken.trim();
            }
        } else if (method.equals("POST")) {
            String[] temp = reqToken.split("&");
            handleData(temp);
        }
    }

    private boolean isMVC(String msg) {
        for (MapContent mapContent : context.values()) {
            if (mapContent != null && mapContent.getClassName().trim().equals(mvcClass)) {
                for (String url : mapContent.getUrls()) {
                    if (url.contains("*")) {
                        url = url.substring(url.indexOf("*") + 1);
                    }
                    if (msg.contains(url)) {// /*.do //".do"
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handleData(String[] temp) {
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

    public void clear() {
        msg = null;
        method = null;
        context = null;
        params = null;
        reqUrl = null;
        mvcClass = null;
        jspParser = null;
        filePath = Dispatcher.ROOTPATH + "files/";
        fileName = null;
        charset = "utf-8";
        fileData = null;
    }
}
