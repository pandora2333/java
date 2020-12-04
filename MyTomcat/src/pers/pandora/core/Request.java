package pers.pandora.core;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.JSP;
import pers.pandora.constant.LOG;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.JspParser;
import pers.pandora.utils.StringUtils;

public final class Request {

    private static Logger logger = LogManager.getLogger(Request.class);

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

    public List<String> getPathParams() {
        return pathParams;
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
        params = new HashMap<>();
        jspParser = new JspParser();
        uploadFiles = new HashMap<>();
        cookies = new ArrayList<>();
        this.dispatcher = dispatcher;
        filePaths = new HashMap<>();
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
        //handle request head
        msg = handleHeadInfo(msg);
        String reqToken = msg.substring(msg.indexOf(HTTPStatus.SLASH), msg.indexOf(HTTPStatus.HTTP)).trim();
        String tempStr = reqToken;
        if (reqToken.contains(String.valueOf(HTTPStatus.GET_PARAMTER_MARK))) {
            tempStr = tempStr.replace(tempStr.substring(reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK)), JSP.NO_CHAR);
        }
        if (msg.startsWith(HTTPStatus.POST)) {
            method = HTTPStatus.POST;
            if (!isMultipart) {
                String param = msg.substring(msg.indexOf(HTTPStatus.CRLF)).trim();
                parseParams(param, HTTPStatus.POST);
            }
        } else if (msg.startsWith(HTTPStatus.OPTIONS)) {
            method = HTTPStatus.OPTIONS;
            return HTTPStatus.OPTIONS;
        } else if (msg.startsWith(HTTPStatus.GET)) {
            method = HTTPStatus.GET;
            String type = judgeStatic(reqToken);
            if (type != null) {
                //static resource
                return type + reqToken;
            }
            parseParams(reqToken, HTTPStatus.GET);
        } else if (msg.startsWith(HTTPStatus.PUT)) {
            method = HTTPStatus.PUT;
            parseParams(reqToken, HTTPStatus.GET);
        } else if (msg.startsWith(HTTPStatus.DELETE)) {
            method = HTTPStatus.DELETE;
            parseParams(reqToken, HTTPStatus.GET);
        } else {
            logger.warn(HTTPStatus.CODE_400_BAD_REQUEST + LOG.LOG_PRE, msg);
            dispatcher.response.setCode(HTTPStatus.CODE_400);
            return null;
        }
        reqUrl = tempStr;
        if (isMVC(reqUrl)) {
            return RequestMappingHandler.MVC_CLASS;
        }
        if (reqUrl.contains(HTTPStatus.JSP)) {
            Tuple<String, String, String> parse = jspParser.parse(dispatcher.server.getRootPath() + reqUrl);
            if (parse != null) {
                dispatcher.addUrlMapping(parse.getK2(), parse.getV());
                return parse.getK1();
            } else {
                return null;
            }
        }
        return dispatcher.server.getContext().get(reqUrl);

    }

    private String handleHeadInfo(String msg) {
        boolean initSession = false;
        Map<String, String> heads = new HashMap<>();
        StringBuilder other = new StringBuilder();
        String[] sp;
        for (String s : msg.split(String.valueOf(HTTPStatus.CRLF), -1)) {
            sp = s.split(HTTPStatus.HEAD_INFO_SPLITER + HTTPStatus.BLANK, -1);
            if (sp.length == 2) {
                if (sp[0].toLowerCase().equals(HTTPStatus.COOKIE_MARK)) {
                    initCookies(sp[1].trim());
                    initSession = true;
                }
                heads.put(sp[0], sp[1].trim());
            } else {
                s = s.trim();
                other.append(s);
                if (StringUtils.isNotEmpty(s)) {
                    other.append(HTTPStatus.CRLF);
                }
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
        //sessionID will be add cookie, and it's path set the root path
        cookie.setPath(String.valueOf(HTTPStatus.SLASH));
        dispatcher.server.addSessionMap(session.getSessionID(), session);
        cookies.add(cookie);
    }

    private void initCookies(String cookie_str) {
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

    private boolean checkSessionInvalid(String sessionID) {
        if (!dispatcher.server.getSessionMap().containsKey(sessionID)) {
            return false;
        }
        Session session = dispatcher.server.getSessionMap().get(sessionID);
        return session.getMax_age() == null || Instant.now().compareTo(session.getMax_age()) < 0;
    }

    private String judgeStatic(String reqToken) {
        if (reqToken.contains(dispatcher.server.getResourceRootPath())) {
            if (reqToken.endsWith(HTTPStatus.HTML_MARK) || reqToken.endsWith(HTTPStatus.HTM_MARK)) {
                return HTTPStatus.TEXT_HTML + HTTPStatus.COLON;
            }
            if (reqToken.endsWith(HTTPStatus.JPG) || reqToken.endsWith(HTTPStatus.PNG) || reqToken.endsWith(HTTPStatus.JPEG)) {
                return HTTPStatus.IMAGE_TYPE + HTTPStatus.COLON;
            }
            return HTTPStatus.PLAIN + HTTPStatus.COLON;
        }
        return null;
    }

    private void parseParams(String reqToken, String method) {
        String[] temp = null;
        if (method.equals(HTTPStatus.GET)) {
            int index = reqToken.indexOf(HTTPStatus.GET_PARAMTER_MARK);
            if (index > 0) {
                temp = reqToken.substring(index + 1).split(String.valueOf(HTTPStatus.PARAMETER_SPLITER));
            } else {
                index = reqToken.length();
            }
            String path = reqToken.substring(0, index);
            if (StringUtils.isNotEmpty(path)) {
                pathParams = Arrays.asList(path.split(String.valueOf(HTTPStatus.SLASH), -1));
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

    private void buildSession(String sessionID, Cookie cookie) {
        if (checkSessionInvalid(sessionID)) {
            session = dispatcher.server.getSessionMap().get(sessionID);
        } else {
            //it's lazy to delete the session just now
            dispatcher.server.getSessionMap().remove(sessionID);
            session = new Session();
            sessionID = session.getSessionID();
            dispatcher.server.addSessionMap(sessionID, session);
            if (cookie != null) {
                cookie.setNeedUpdate(true);
                cookie.setValue(sessionID);
                //sessionID will be add cookie, and it's path set the root path
                cookie.setPath(String.valueOf(HTTPStatus.SLASH));
            }
        }
    }

    private void handleData(String[] temp) {
        if (temp == null) {
            return;
        }
        String[] kv;
        List<Object> list;
        for (String str : temp) {
            kv = str.split(HTTPStatus.PARAM_KV_SPLITER);
            if (kv != null && kv.length == 2) {
                if (kv[0].equals(HTTPStatus.SESSION_MARK) && session == null) {
                    buildSession(kv[1], null);
                }
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

    void reset() {
        method = null;
        reqUrl = null;
        if (CollectionUtil.isNotEmptry(params)) {
            params.clear();
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
}
