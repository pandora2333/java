package pers.pandora.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import pers.pandora.constant.JSP;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.server.Session;
import pers.pandora.utils.JspParser;
import pers.pandora.utils.StringUtils;

public final class Request {

    private String method;

    private Dispatcher dispatcher;

    private Map<String, List<Object>> params;

    private String reqUrl;

    private JspParser jspParser;

    private String charset = HTTPStatus.DEFAULTENCODING;

    private Map<String, Tuple<String, String, byte[]>> uploadFiles;//fileVarName -> {fileName,fileType,byte[] data}

    private Map<String, String> heads;

    private Map<String, Object> objectList;

    private List<Cookie> cookies;

    private boolean isMultipart;

    private Session session;


    public Map<String, Tuple<String, String, byte[]>> getUploadFiles() {
        return uploadFiles;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void addCookies(Cookie cookie) {
        if (cookie != null) {
            cookies.add(cookie);
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setUploadFiles(Map<String, Tuple<String, String, byte[]>> uploadFiles) {
        this.uploadFiles = uploadFiles;
    }

    public void setMultipart(boolean multipart) {
        isMultipart = multipart;
    }

    public boolean isMultipart() {
        return isMultipart;
    }

    public void setHeads(Map<String, String> heads) {
        this.heads = heads;
    }

    public Map<String, String> getHeads() {
        return heads;
    }

    public void setObjectList(Map<String, Object> objectList) {
        this.objectList = objectList;
    }

    public Map<String, Object> getObjectList() {
        return objectList;
    }

    public Request(Dispatcher dispatcher) {
        params = new HashMap<>();
        jspParser = new JspParser();
        uploadFiles = new HashMap<>();
        cookies = new ArrayList<>();
        this.dispatcher = dispatcher;
    }

    public void setJspParser(JspParser jspParser) {
        this.jspParser = jspParser;
    }

    public JspParser getJspParser() {
        return jspParser;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
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


    public void saveFileData(String fileVarName) {
        if (!StringUtils.isNotEmpty(fileVarName) || !uploadFiles.containsKey(fileVarName)) {
            throw new RuntimeException("无文件上传!");
        }
        String filePath = dispatcher.server.getRootPath() + dispatcher.server.REQUEST_FILE_DIR;
        java.io.File path = new java.io.File(filePath);
        if (!path.exists()) {
            path.mkdirs();
        }
        Tuple<String, String, byte[]> file = uploadFiles.get(fileVarName);
        if(file.getV() != null && StringUtils.isNotEmpty(file.getK1())) {
            //若文件名为""会造成文件管理器拒绝创建文件的异常
            try {
                Files.write(Paths.get(filePath + file.getK1()), file.getV());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String handle(String msg) {//GET /login HTTP/1.1
        //处理request head
        msg = handleHeadInfo(msg);
        String reqToken = msg.substring(msg.indexOf(HTTPStatus.SLASH), msg.indexOf(HTTPStatus.HTTP)).trim();//GET /login HTTP/1.1
        String tempStr = reqToken;
        if (reqToken.contains(String.valueOf(HTTPStatus.GET_PARAMTER_MARK))) {
            tempStr = tempStr.replace(tempStr.substring(reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK)), JSP.NO_CHAR);
        }
        if (msg.startsWith(HTTPStatus.GET)) {
            method = HTTPStatus.GET;
            String type = judgeStatic(reqToken);
            if (type != null) {//对静态资源处理
                return type + reqToken;
            }
            parseParams(reqToken, HTTPStatus.GET);
        } else if (msg.startsWith(HTTPStatus.POST)) {
            method = HTTPStatus.POST;
            if (!isMultipart) {
                String param = msg.substring(msg.lastIndexOf(HTTPStatus.CRLF)).trim();
                parseParams(param, HTTPStatus.POST);
            }
        }
        reqUrl = tempStr;//保存请求路径
        if (reqUrl.contains(HTTPStatus.JSP)) {
            return jspParser.parse(dispatcher.server.getRootPath() + reqUrl, dispatcher);
        }
        if (isMVC(reqUrl)) {
            return RequestMappingHandler.MVC_CLASS;
        }
        return dispatcher.server.getContext().get(reqUrl);

    }

    private String handleHeadInfo(String msg) {
        boolean initSession = false;
        Map<String, String> heads = new HashMap<>();
        StringBuilder other = new StringBuilder();
        String[] sp;
        for (String s : msg.split(String.valueOf(HTTPStatus.CRLF), -1)) {
            sp = s.split(HTTPStatus.HEAD_INFO_SPLITER + HTTPStatus.BLANK);
            if (sp.length == 2) {
                if (sp[0].toLowerCase().equals(HTTPStatus.COOKIE_MARK)) {
                    initCookies(sp[1]);
                    initSession = true;
                }
                heads.put(sp[0], sp[1].trim());
            } else {
                other.append(s.trim());
            }
        }
        if (!initSession && (session == null || !checkSessionInvalid(session.getSessionID()))) {
            initSession();
        }
        this.heads = heads;
        return other.toString();
    }

    private void initSession() {
        session = new Session();
        Cookie cookie = new Cookie();
        cookie.setKey(HTTPStatus.SESSION_MARK);
        cookie.setValue(session.getSessionID());
        cookie.setNeedUpdate(true);
        dispatcher.server.getSessionMap().put(session.getSessionID(), session);
        cookies.add(cookie);
    }

    private void initCookies(String cookie_str) {
        boolean initSession = false;
        if (StringUtils.isNotEmpty(cookie_str)) {
            Cookie cookie = null;
            String[] ss;
            for (String tmp : cookie_str.split(HTTPStatus.COOKIE_SPLITER)) {
                ss = tmp.split(HTTPStatus.COOKIE_KV_SPLITE);
                if (ss.length == 2) {
                    cookie = new Cookie();
                    if (ss[0].equals(HTTPStatus.SESSION_MARK)) {
                        if (checkSessionInvalid(ss[1])) {
                            session = dispatcher.server.getSessionMap().get(ss[1]);
                        } else {
                            //惰性删除过期session
                            dispatcher.server.getSessionMap().remove(ss[1]);
                            session = new Session();
                            ss[1] = session.getSessionID();
                            dispatcher.server.getSessionMap().put(ss[1], session);
                            cookie.setNeedUpdate(true);
                        }
                        initSession = true;
                    }
                    cookie.setKey(ss[0]);
                    cookie.setValue(ss[1]);
                    cookies.add(cookie);
                }
            }
        }
        if (!initSession) {
            initSession();
        }
    }

    private boolean checkSessionInvalid(String sessionID) {
        if (!dispatcher.server.getSessionMap().containsKey(sessionID)) {
            return false;
        }
        Session session = dispatcher.server.getSessionMap().get(sessionID);
        return !session.getIsValid() || session.getMax_age() > 0;
    }

    private String judgeStatic(String reqToken) {
        if (reqToken.endsWith(HTTPStatus.HTML_MARK) || reqToken.endsWith(HTTPStatus.HTM_MARK)) {
            return HTTPStatus.TEXT_HTML + HTTPStatus.COLON;
        }
        if (reqToken.endsWith(HTTPStatus.JPG) || reqToken.endsWith(HTTPStatus.PNG) || reqToken.endsWith(HTTPStatus.JPEG)) {
            return HTTPStatus.IMAGE_TYPE + HTTPStatus.COLON;
        }
        return null;
    }

    private void parseParams(String reqToken, String method) {
        String[] temp = null;
        if (method.equals(HTTPStatus.GET)) {
            if (reqToken.contains(String.valueOf(HTTPStatus.GET_PARAMTER_MARK))) {
                reqToken.substring(0, reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK)).trim();
                temp = reqToken.substring(reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK) + 1).split(String.valueOf(HTTPStatus.PARAMETER_SPLITER));
            } else {
                reqToken.trim();
            }
        } else if (method.equals(HTTPStatus.POST)) {
            temp = reqToken.split(String.valueOf(HTTPStatus.PARAMETER_SPLITER));
        }
        handleData(temp);
    }

    private boolean isMVC(String reqUrl) {
        for (Map.Entry<String, String> entry : dispatcher.server.getContext().entrySet()) {
            if (entry.getValue().equals(RequestMappingHandler.MVC_CLASS)) {// /.*.do //.do
                if (entry.getKey().equals(reqUrl) || reqUrl.matches(entry.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleData(String[] temp) {
        if (temp == null) {
            return;
        }
        String[] kv;
        List<Object> list;
        for (String str : temp) {
            kv = str.split(HTTPStatus.COOKIE_KV_SPLITE);
            if (kv != null && kv.length == 2) {
                if (params != null) {
                    list = params.get(kv[0]);
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

    void reset() {
        method = null;
        params.clear();
        reqUrl = null;
        uploadFiles.clear();
        cookies.clear();
    }
}
