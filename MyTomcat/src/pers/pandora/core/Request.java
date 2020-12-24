package pers.pandora.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.JspParser;
import pers.pandora.utils.StringUtils;

public final class Request {

    private static final Logger logger = LogManager.getLogger(Request.class);

    private String method;

    private Dispatcher dispatcher;

    private Map<String, List<Object>> params;

    private String reqUrl;

    private JspParser jspParser;

    private String charset = HTTPStatus.DEFAULTENCODING;
    //fileVarName -> {fileName,fileType,byte[] data}
    private Map<String, Tuple<String, String, byte[]>> uploadFiles;
    //up file need save in local paths
    private Map<String, String> filePaths;

    private Map<String, String> heads;
    //support mvc model
    private Map<String, Object> objectList;
    //support path parameters for restful param pattern
    private List<String> pathParams;

    private List<Cookie> cookies;

    private boolean isMultipart;

    private Session session;

    private String fileDesc;

    private boolean json;

    private JSONParser jsonParser;

    private boolean redirect;

    private Map<String, Map<Integer, Map<String, String>>> listTypeParams;

    public Map<String, Map<Integer, Map<String, String>>> getListTypeParams() {
        return listTypeParams;
    }

    public void setReqUrl(String reqUrl) {
        this.reqUrl = reqUrl;
    }

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public JSONParser getJsonParser() {
        return jsonParser;
    }

    public void setJsonParser(JSONParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public boolean isJson() {
        return json;
    }

    public String getFileDesc() {
        return fileDesc;
    }

    public void setFileDesc(String fileDesc) {
        this.fileDesc = fileDesc;
    }

    public List<String> getPathParams() {
        return pathParams;
    }

    public void setPathParams(List<String> pathParams) {
        this.pathParams = pathParams;
    }

    public boolean addInvalidSession(String key) {
        if (StringUtils.isNotEmpty(key)) {
            Session session = dispatcher.server.getSessionMap().get(key);
            if (session != null) {
                return dispatcher.server.addInvalidSessionMap(key, session);
            }
        }
        return false;
    }

    public String getServerName() {
        return dispatcher.server.getServerName();
    }

    public Map<String, String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(Map<String, String> filePaths) {
        this.filePaths = filePaths;
    }

    public void addFilePath(String fileVarName, String filePath) {
        if (StringUtils.isNotEmpty(fileVarName) && StringUtils.isNotEmpty(filePath)) {
            filePaths.put(fileVarName, filePath);
        }
    }

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
        params = new HashMap<>(4);
        jspParser = new JspParser();
        uploadFiles = new HashMap<>(4);
        cookies = new ArrayList<>(1);
        heads = new HashMap<>(16);
        this.dispatcher = dispatcher;
        filePaths = new HashMap<>(4);
        listTypeParams = new HashMap<>(4);
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


    public void saveFileData(String fileVarName, String fileName) {
        if (!StringUtils.isNotEmpty(fileVarName) || !uploadFiles.containsKey(fileVarName)) {
            logger.warn(LOG.LOG_PRE + "saveFileData" + LOG.LOG_PRE + "NO DATAS!", dispatcher.server.getServerName(), LOG.ERROR_DESC);
            return;
        }
        String filePath = filePaths.get(fileVarName);
        if (!StringUtils.isNotEmpty(filePath)) {
            filePath = dispatcher.server.getRootPath() + dispatcher.server.requestFileDir;
        }
        java.io.File path = new java.io.File(filePath);
        if (!path.exists()) {
            path.mkdirs();
        }
        Tuple<String, String, byte[]> file = uploadFiles.get(fileVarName);
        if (file.getV() != null && StringUtils.isNotEmpty(file.getK1())) {
            //if file name is "",it will cause a excetion that file manager refuse to create the file
            if (!StringUtils.isNotEmpty(fileName)) {
                fileName = file.getK1();
            }
            try {
                //base on mapped memeroy  file
                FileChannel outChannel = FileChannel.open(Paths.get(filePath + fileName),
                        StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
                MappedByteBuffer outMappedBuf = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, file.getV().length);
                outMappedBuf.put(file.getV());
                outChannel.close();
            } catch (IOException e) {
                logger.error(LOG.LOG_PRE + "I/O write " + LOG.LOG_PRE + LOG.LOG_POS,
                        dispatcher.server.getServerName(), fileName, LOG.EXCEPTION_DESC, e);
            }
        }
    }

    public String handle(String msg) {
        try {
            msg = URLDecoder.decode(msg, charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("decode URL" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            dispatcher.response.setCode(HTTPStatus.CODE_404);
            return null;
        }
        if (msg.startsWith(HTTPStatus.OPTIONS)) {
            method = HTTPStatus.OPTIONS;
            return HTTPStatus.OPTIONS;
        }
        int i = msg.indexOf(HTTPStatus.SLASH),j = msg.indexOf(HTTPStatus.HTTP);
        if(i > j){
            return null;
        }
        String reqToken = msg.substring(i, j).trim();
        json = Objects.equals(heads.get(HTTPStatus.CONTENTTYPE), HTTPStatus.JSON_TYPE);
        int index = reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK);
        if (index > 0) {
            reqUrl = reqToken.substring(0, index);
            if (json) {
                try {
                    addJSON(URLDecoder.decode(reqToken.substring(index + 1), charset));
                } catch (UnsupportedEncodingException e) {
                    dispatcher.response.setCode(HTTPStatus.CODE_400);
                }
            }
        } else {
            reqUrl = reqToken;
        }
        if (msg.startsWith(HTTPStatus.POST)) {
            method = HTTPStatus.POST;
            index = msg.indexOf(HTTPStatus.CRLF);
            if (index > 0) {
                String param = msg.substring(index).trim();
                if (json) {
                    addJSON(param);
                } else if (!isMultipart) {
                    parseParams(param, false);
                }
            }
        } else if (msg.startsWith(HTTPStatus.GET)) {
            method = HTTPStatus.GET;
            String type = judgeStatic(reqUrl);
            if (type != null) {
                //static resource
                return type + reqUrl;
            }
        } else if (msg.startsWith(HTTPStatus.PUT)) {
            method = HTTPStatus.PUT;
        } else if (msg.startsWith(HTTPStatus.DELETE)) {
            method = HTTPStatus.DELETE;
        } else {
            logger.warn(HTTPStatus.CODE_400_BAD_REQUEST + LOG.LOG_PRE, msg);
            dispatcher.response.setCode(HTTPStatus.CODE_400);
            return null;
        }
        parseParams(reqToken, true);
        if (reqUrl.contains(HTTPStatus.JSP)) {
            Tuple<String, String, String> parse = jspParser.parse(dispatcher.server.getRootPath() + reqUrl, dispatcher.server.isHotLoadJSP());
            if (parse != null) {
                dispatcher.addUrlMapping(parse.getK2(), parse.getV());
                dispatcher.response.setType(HTTPStatus.TEXT_HTML);
                return parse.getK1();
            }
        }
        if (isMVC(reqUrl)) {
            return RequestMappingHandler.MVC_CLASS;
        }
        return dispatcher.server.getContext().get(reqUrl);

    }

    private void addJSON(String param) {
        if (jsonParser == null || !StringUtils.isNotEmpty(param)) {
            return;
        }
        List<Object> tmp = new ArrayList<>(1);
        tmp.add(param);
        params.put(HTTPStatus.JSON_TYPE, tmp);
    }

    void initSession() {
        session = new Session(getSessionID());
        Cookie cookie = new Cookie();
        cookie.setKey(HTTPStatus.SESSION_MARK);
        cookie.setValue(session.getSessionID());
        cookie.setNeedUpdate(true);
        session.setSessionCookie(cookie);
        //sessionID will be add cookie, and it's path set the root path
        cookie.setPath(String.valueOf(HTTPStatus.SLASH));
        dispatcher.server.addSessionMap(session.getSessionID(), session);
        cookies.add(cookie);
    }

    //It ensures that the sessionID is never duplicated
    String getSessionID() {
        String sessionID;
        do {
            sessionID = dispatcher.server.getIdWorker().nextSessionID();
        } while (dispatcher.server.getSessionMap().containsKey(sessionID));
        return sessionID;
    }

    void initCookies(String cookie_str) {
        boolean initSession = false;
        if (StringUtils.isNotEmpty(cookie_str)) {
            Cookie cookie;
            String[] ss;
            for (String tmp : cookie_str.split(HTTPStatus.COOKIE_SPLITER, -1)) {
                ss = tmp.split(HTTPStatus.PARAM_KV_SPLITER, -1);
                if (ss.length == 2) {
                    cookie = new Cookie();
                    cookie.setKey(ss[0]);
                    if (ss[0].equals(HTTPStatus.SESSION_MARK)) {
                        buildSession(ss[1], cookie);
                        initSession = true;
                    } else {
                        cookie.setValue(ss[1]);
                    }
                    cookies.add(cookie);
                }
            }
        }
        if (!initSession) {
            initSession();
        }
    }

    boolean checkSessionInvalid(String sessionID) {
        if (!dispatcher.server.getSessionMap().containsKey(sessionID)) {
            return false;
        }
        Session session = dispatcher.server.getSessionMap().get(sessionID);
        return session.getMax_age() == null || Instant.now().compareTo(session.getMax_age()) < 0;
    }

    private String judgeStatic(String reqToken) {
        if (reqToken.startsWith(dispatcher.server.getResourceRootPath())) {
            if (reqToken.endsWith(HTTPStatus.HTML_MARK) || reqToken.endsWith(HTTPStatus.HTM_MARK)) {
                return HTTPStatus.TEXT_HTML + HTTPStatus.COLON;
            }
            if (reqToken.endsWith(HTTPStatus.JPG) || reqToken.endsWith(HTTPStatus.JPEG)) {
                return HTTPStatus.JPG_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.PNG)) {
                return HTTPStatus.PNG_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.XML)) {
                return HTTPStatus.XML_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.GIF)) {
                return HTTPStatus.GIF_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.JSON)) {
                return HTTPStatus.JSON_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.PDF)) {
                return HTTPStatus.PDF_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.JS)) {
                return HTTPStatus.JS_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.CSS)) {
                return HTTPStatus.CSS_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.MP4)) {
                return HTTPStatus.MP4_TYPE + HTTPStatus.COLON;
            } else if (reqToken.endsWith(HTTPStatus.MP3)) {
                return HTTPStatus.MP3_TYPE + HTTPStatus.COLON;
            }
            //default binary data
            return HTTPStatus.BINARY_TYPE + HTTPStatus.COLON;
        }
        return null;
    }

    private void parseParams(String reqToken, boolean isGet) {
        if (StringUtils.isNotEmpty(reqToken)) {
            String[] temp = null;
            if (isGet) {
                int index = reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK);
                if (index > 0) {
                    temp = reqToken.substring(index + 1).split(String.valueOf(HTTPStatus.PARAMETER_SPLITER));
                } else {
                    index = reqToken.length();
                }
                String path = reqToken.substring(0, index);
                if (StringUtils.isNotEmpty(path) && path.length() > 1) {
                    pathParams = Arrays.asList(path.split(String.valueOf(HTTPStatus.SLASH), -1));
                }
            } else {
                temp = reqToken.split(String.valueOf(HTTPStatus.PARAMETER_SPLITER));
            }
            handleData(temp);
        }
    }

    private boolean isMVC(String reqUrl) {
        for (Map.Entry<String, String> entry : dispatcher.server.getContext().entrySet()) {
            if (entry.getValue().equals(RequestMappingHandler.MVC_CLASS) && reqUrl.matches(entry.getKey())) {// /.*.do //.do
                return true;
            }
        }
        return false;
    }

    private void buildSession(String sessionID, Cookie cookie) {
        if (checkSessionInvalid(sessionID)) {
            session = dispatcher.server.getSessionMap().get(sessionID);
        } else {
            //It's lazy to delete the session just now
            dispatcher.server.getSessionMap().remove(sessionID);
            sessionID = getSessionID();
            session = new Session(sessionID);
            dispatcher.server.addSessionMap(sessionID, session);
            if (cookie != null) {
                cookie.setNeedUpdate(true);
                cookie.setValue(sessionID);
                //sessionID will be add cookie, and it's path set the root path
                cookie.setPath(String.valueOf(HTTPStatus.SLASH));
                session.setSessionCookie(cookie);
            }
        }
    }

    private void handleData(String[] temp) {
        if (temp == null) {
            return;
        }
        String[] kv;
        String key;
        int i, j;
        List<Object> list;
        Map map;
        for (String str : temp) {
            kv = str.split(HTTPStatus.PARAM_KV_SPLITER);
            if (kv.length == 2) {
                if (kv[0].equals(HTTPStatus.SESSION_MARK) && session == null) {
                    buildSession(kv[1], null);
                } else if (kv[0].matches(HTTPStatus.LISTTYPE)) {
                    i = kv[0].indexOf(HTTPStatus.LISTTYPESEPARATOR_PRE);
                    key = kv[0].substring(0, i);
                    map = listTypeParams.computeIfAbsent(key, k -> new HashMap<>(4));
                    j = kv[0].indexOf(HTTPStatus.LISTTYPESEPARATOR_POS);
                    key = kv[0].substring(i + 1, j);
                    map = (Map) map.computeIfAbsent(Math.min(map.size(),Integer.valueOf(key)), k -> new HashMap<>(4));
                    map.put(kv[0].substring(j + 2), kv[1]);
                } else {
                    list = params.get(kv[0]);
                    if (list == null) {
                        list = new ArrayList<>(1);
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
        reqUrl = null;
        fileDesc = null;
        json = false;
        redirect = false;
        jsonParser = dispatcher.server.getJsonParser();
        if (CollectionUtil.isNotEmptry(params)) {
            params.clear();
        }
        if (CollectionUtil.isNotEmptry(listTypeParams)) {
            listTypeParams.clear();
        }
        if (CollectionUtil.isNotEmptry(uploadFiles)) {
            uploadFiles.clear();
        }
        if (CollectionUtil.isNotEmptry(cookies)) {
            cookies.clear();
        }
        if (CollectionUtil.isNotEmptry(filePaths)) {
            filePaths.clear();
        }
        if (CollectionUtil.isNotEmptry(objectList)) {
            objectList.clear();
        }
        isMultipart = false;
        if (CollectionUtil.isNotEmptry(heads)) {
            heads.clear();
        }
        if (CollectionUtil.isNotEmptry(filePaths)) {
            filePaths.clear();
        }
        pathParams = null;
        session = null;
        charset = HTTPStatus.DEFAULTENCODING;
    }

    void handleJSON() {
        if (jsonParser == null) {
            return;
        }
        List<Object> list = params.get(HTTPStatus.JSON_TYPE);
        if (CollectionUtil.isNotEmptry(list)) {
            Map<String, Object> objectMap = null;
            try {
                objectMap = jsonParser.parse((String) list.get(0));
            } catch (Exception e) {
                dispatcher.response.setCode(HTTPStatus.CODE_400);
            }
            if (objectMap != null) {
                objectMap.forEach((k, v) -> {
                    List<Object> tmp = new ArrayList<>(1);
                    tmp.add(v);
                    params.put(k, tmp);
                });
            }
            params.remove(HTTPStatus.JSON_TYPE);
        }
    }

    public void dispatcher(String path) {
        dispatcher.dispatcher(HTTPStatus.GET + HTTPStatus.BLANK + path + HTTPStatus.BLANK + HTTPStatus.HTTP1_1);
    }
}
