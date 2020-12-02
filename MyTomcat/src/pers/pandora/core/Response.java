package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.servlet.Servlet;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.vo.Pair;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Response {

    private static Logger logger = LogManager.getLogger(Response.class);

    private String servlet;

    private String charset = HTTPStatus.DEFAULTENCODING;
    //content length
    private long len;
    //response body data
    private StringBuilder content;
    //resource type for the response
    private String type = HTTPStatus.TEXT_HTML;
    //the response is the static resoucre
    private boolean resource;

    private Dispatcher dispatcher;
    //response code
    private int code;

    private Map<String, String> heads;

    public static final String PLAIN = "MODELANDVIEW_REQUEST_FORWARD_PLAIN";


    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public Response(Dispatcher dispatcher) {
        heads = new HashMap<>();
        this.dispatcher = dispatcher;
    }

    public void setResource(boolean resource) {
        this.resource = resource;
    }

    public void setLen(long len) {
        this.len = len;
    }

    public long getLen() {
        return len;
    }

    public StringBuilder getContent() {
        return content;
    }

    public boolean isResource() {
        return resource;
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }

    public Map<String, String> getHeads() {
        return heads;
    }

    public boolean addHeads(String key, String value) {
        if (StringUtils.isNotEmpty(key)) {
            String t = key.toLowerCase();
            if (!t.equals(HTTPStatus.SERVER.toLowerCase()) && !t.equals(HTTPStatus.DATE.toLowerCase()) &&
                    !t.equals(HTTPStatus.CONTENTLENGTH.toLowerCase())
                    && !t.equals(HTTPStatus.CONTENTTYPE.toLowerCase())) {
                this.heads.put(key, value);
                return true;
            } else {
                logger.warn(LOG.LOG_POS + LOG.VERTICAL + LOG.LOG_PRE + LOG.VERTICAL + LOG.LOG_PRE + LOG.VERTICAL + LOG.LOG_PRE +
                                "can't add into headers", dispatcher.server.getServerName(), HTTPStatus.SERVER, HTTPStatus.DATE,
                        HTTPStatus.CONTENTLENGTH, HTTPStatus.CONTENTTYPE);
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

    public void setType(String type) {
        if (type == null && servlet == null) {
            content = new StringBuilder(String.format("request uri params " + LOG.LOG_PRE, LOG.ERROR_DESC));
        }
        if (type != null) {
            this.type = type;
        }
    }

    public String getType() {
        return type;
    }

    public void setServlet(String servlet) {
        if (StringUtils.isNotEmpty(servlet) && !servlet.contains(String.valueOf(HTTPStatus.COLON))) {
            this.servlet = servlet;
        }
    }

    public String getServlet() {
        return servlet;
    }

    private StringBuilder createHeadInfo(List<Cookie> cookies) {
        StringBuilder headInfo = new StringBuilder();
        //http version，status code，description
        headInfo.append(HTTPStatus.HTTP1_1).append(HTTPStatus.BLANK).append(code).append(HTTPStatus.BLANK);
        switch (code) {
            case 200:
                headInfo.append(HTTPStatus.CODE_200);
                break;
            case 404:
                headInfo.append(HTTPStatus.CODE_404);
                break;
            case 500:
                headInfo.append(HTTPStatus.CODE_500);
                break;
            case 302:
                headInfo.append(HTTPStatus.CODE_302);
                break;
            default:
                headInfo.append(HTTPStatus.ERROR_CODE);
        }
        headInfo.append(HTTPStatus.CRLF);
        //2.build response head
        headInfo.append(HTTPStatus.SERVER + HTTPStatus.COLON + HTTPStatus.BLANK + HTTPStatus.SERVER_DESC).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.DATE + HTTPStatus.COLON + HTTPStatus.BLANK).append(new Date()).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.CONTENTTYPE + HTTPStatus.COLON + HTTPStatus.BLANK + type + HTTPStatus.COOKIE_SPLITER +
                HTTPStatus.CHARSET + HTTPStatus.PARAM_KV_SPLITER + charset).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.CONTENTLENGTH + HTTPStatus.COLON + HTTPStatus.BLANK).append(len).append(HTTPStatus.CRLF);
        heads.forEach((k, v) -> headInfo.append(k + HTTPStatus.COLON + v).append(HTTPStatus.CRLF));
        //build cookies
        if (CollectionUtil.isNotEmptry(cookies)) {
            StringBuilder sb = new StringBuilder();
            cookies.stream().forEach(cookie -> {
                if (cookie != null && cookie.isNeedUpdate()) {
                    sb.append(HTTPStatus.SET_COOKIE);
                    sb.append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(cookie.getKey()).append(HTTPStatus.PARAM_KV_SPLITER).
                            append(cookie.getValue()).append(HTTPStatus.COOKIE_SPLITER);
                    sb.append(HTTPStatus.VERSION).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getVersion())
                            .append(HTTPStatus.COOKIE_SPLITER);
                    if (StringUtils.isNotEmpty(cookie.getExpires())) {
                        sb.append(HTTPStatus.EXPIRES).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getExpires())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (cookie.getMax_age() != 0) {
                        sb.append(HTTPStatus.MAXAEG).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getMax_age())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getDoamin())) {
                        sb.append(HTTPStatus.DOMAIN).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getMax_age())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getPath())) {
                        sb.append(HTTPStatus.PATH).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getPath())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (cookie.getSecure() > 0) {
                        sb.append(HTTPStatus.SECURE).append(HTTPStatus.PARAM_KV_SPLITER).append(cookie.getSecure())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    sb.append(HTTPStatus.CRLF);
                    headInfo.append(sb);
                    sb.delete(0, sb.length());
                    cookie.setNeedUpdate(false);
                }
            });
        }
        headInfo.append(HTTPStatus.CRLF);
        return headInfo;
    }

    public String handle(String method, Request request) {
        if (content == null) {
            content = new StringBuilder();
        }
        //OPTIONS is HTTP pre-request，just return ok signal
        if (StringUtils.isNotEmpty(method) && method.equals(HTTPStatus.OPTIONS)) {
            code = 200;
            return createHeadInfo(null).toString();
        }
        handlePre(request);
        try {
            if (request != null && request.getParams().containsKey(PLAIN)) {
                type = HTTPStatus.PLAIN;
                content.append(request.getParams().get(HTTPStatus.PLAIN).get(0));
                len = content.toString().getBytes(charset).length;
                code = 200;
            } else if (StringUtils.isNotEmpty(servlet)) {
                Map<String, List<Object>> params = request.getParams();
                //init object instance just support basic data type and string type
                Servlet handler = ClassUtils.getClass(servlet);
                //requestScope
                ClassUtils.initWithParams(handler, params);
                //sessionScope
                ClassUtils.initWithParams(handler, request.getSession().getAttrbuites());
                //mvcScope
                ClassUtils.initWithObjectList(handler, request.getObjectList());
                if (handler != null) {
                    if (method.equals(HTTPStatus.GET)) {
                        content.append(handler.doGet(request, this));
                    } else if (method.equals(HTTPStatus.POST)) {
                        content.append(handler.doPost(request, this));
                    }
                    len = content.toString().getBytes(charset).length;
                    if (code <= 0) {
                        code = 200;
                    }
                } else {
                    handle_404_NOT_FOUND();
                }
            } else if (resource) {
                code = 200;
            } else {
                handle_404_NOT_FOUND();
            }
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "handle" + LOG.LOG_POS, dispatcher.server.getServerName(), LOG.EXCEPTION_DESC, e);
            handle_500_SERVER_ERROR(e.getMessage());
        }
        handleAfter(request);
        return createHeadInfo(request.getCookies()).append(content).toString();
    }

    private boolean handleAfter(Request request) {
        for (Pair<Integer, Interceptor> interceptor : RequestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().afterMethod(request, this)) {
                return false;
            }
        }
        return true;
    }

    private boolean handlePre(Request request) {
        for (Pair<Integer, Interceptor> interceptor : RequestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().preMethod(request, this)) {
                return false;
            }
        }
        return true;
    }

    private void handle_500_SERVER_ERROR(String errorMessage) {
        content.append(HTTPStatus.CODE_500_DESC + HTTPStatus.COLON + errorMessage);
        type = HTTPStatus.PLAIN;
        try {
            len = content.toString().getBytes(charset).length;
        } catch (UnsupportedEncodingException e) {
            logger.error(LOG.LOG_PRE + "handle_500_SERVER_ERROR current charset " + LOG.LOG_PRE + LOG.LOG_POS,
                    dispatcher.server.getServerName(), charset, content, LOG.EXCEPTION_DESC, e);
        }
        code = 500;
    }

    private void handle_404_NOT_FOUND() throws UnsupportedEncodingException {
        content.append(HTTPStatus.CODE_404_DESC);
        type = HTTPStatus.PLAIN;
        len = content.toString().getBytes(charset).length;
        code = 404;
    }

    void reset() {
        servlet = null;
        resource = false;
        content = null;
        len = 0;
        code = 0;
        if (CollectionUtil.isNotEmptry(heads)) {
            heads.clear();
        }
        charset = HTTPStatus.DEFAULTENCODING;
        type = HTTPStatus.TEXT_HTML;
    }
}
