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

import java.nio.charset.Charset;
import java.util.*;

public final class Response {

    private static Logger logger = LogManager.getLogger(Response.class);

    private String servlet;

    private String charset = HTTPStatus.DEFAULTENCODING;
    //content length
    private long len;
    //response body data
    private byte[] content;
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

    public byte[] getContent() {
        return content;
    }

    public boolean isResource() {
        return resource;
    }

    public void setContent(byte[] content) {
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
            content = String.format("request uri params " + LOG.LOG_PRE, LOG.ERROR_DESC).getBytes(Charset.forName(charset));
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

    private byte[] createHeadInfo(List<Cookie> cookies) {
        StringBuilder headInfo = new StringBuilder();
        //http version，status code，description
        headInfo.append(HTTPStatus.HTTP1_1).append(HTTPStatus.BLANK).append(code).append(HTTPStatus.BLANK);
        switch (code) {
            case HTTPStatus.CODE_200:
                headInfo.append(HTTPStatus.CODE_200_DESC);
                break;
            case HTTPStatus.CODE_302:
                headInfo.append(HTTPStatus.CODE_302_DESC);
                break;
            case HTTPStatus.CODE_400:
                headInfo.append(HTTPStatus.CODE_400_BAD_REQUEST);
                break;
            case HTTPStatus.CODE_404:
                headInfo.append(HTTPStatus.CODE_404_DESC);
                break;
            case HTTPStatus.CODE_405:
                headInfo.append(HTTPStatus.CODE_405_METHOD_NOT_SUPPORTED);
                break;
            case HTTPStatus.CODE_500:
                headInfo.append(HTTPStatus.CODE_500_DESC);
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
        return headInfo.toString().getBytes(Charset.forName(charset));
    }

    public byte[] handle(String method, Request request) {
        //OPTIONS is HTTP pre-request，just return ok signal or other bad request
        if (code == HTTPStatus.CODE_400 || code == HTTPStatus.CODE_405 || (StringUtils.isNotEmpty(method) && method.equals(HTTPStatus.OPTIONS))) {
            return createHeadInfo(null);
        }
        handlePre(request);
        try {
            if (request != null && request.getParams().containsKey(PLAIN)) {
                type = HTTPStatus.PLAIN;
                Object obj = request.getParams().get(PLAIN).get(0);
                if (obj != null) {
                    content = obj.toString().getBytes(Charset.forName(charset));
                    len = content.length;
                }
                code = HTTPStatus.CODE_200;
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
                    String ret = null;
                    if (method.equals(HTTPStatus.GET)) {
                        ret = handler.doGet(request, this);
                    } else if (method.equals(HTTPStatus.POST)) {
                        ret = handler.doPost(request, this);
                    }
                    if (StringUtils.isNotEmpty(ret)) {
                        content = ret.getBytes(Charset.forName(charset));
                        len = content.length;
                    }
                    if (code <= 0) {
                        code = HTTPStatus.CODE_200;
                    }
                } else {
                    handle_404_NOT_FOUND();
                }
            } else if (resource) {
                code = HTTPStatus.CODE_200;
            } else {
                handle_404_NOT_FOUND();
            }
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "handle" + LOG.LOG_POS, dispatcher.server.getServerName(), LOG.EXCEPTION_DESC, e);
            handle_500_SERVER_ERROR(e.getMessage());
        }
        handleAfter(request);
        byte[] heads = createHeadInfo(request.getCookies());
        byte[] datas = new byte[heads.length + (content != null ? content.length : 0)];
        System.arraycopy(heads, 0, datas, 0, heads.length);
        if (content != null) {
            System.arraycopy(content, 0, datas, heads.length, content.length);
        }
        return datas;
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
        content = (HTTPStatus.CODE_500_OUTPUT_DESC + HTTPStatus.COLON + errorMessage).getBytes(Charset.forName(charset));
        type = HTTPStatus.PLAIN;
        len = content.length;
        code = HTTPStatus.CODE_500;
    }

    private void handle_404_NOT_FOUND() {
        content = HTTPStatus.CODE_404_OUTPUT_DESC.getBytes(Charset.forName(charset));
        type = HTTPStatus.PLAIN;
        len = content.length;
        code = HTTPStatus.CODE_404;
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
