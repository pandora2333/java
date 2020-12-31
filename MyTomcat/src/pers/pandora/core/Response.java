package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.JSP;
import pers.pandora.constant.LOG;
import pers.pandora.servlet.Servlet;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.StringUtils;

import java.nio.charset.Charset;
import java.util.*;

public final class Response {

    private static final Logger logger = LogManager.getLogger(Response.class);

    private String servlet;

    private String charset = HTTPStatus.DEFAULTENCODING;
    //content length
    private long len;
    //response body data
    private byte[] content;
    //resource type for the response
    private String type = HTTPStatus.PLAIN;
    //PLAIN parser
    private JSONParser jsonParser;

    private Dispatcher dispatcher;
    //control cache time
    private long max_age;
    //response code
    private int code;

    private Map<String, String> heads;
    //support code 206
    private long end;

    private long total;

    private long start;

    public static final String PLAIN = "MODELANDVIEW_REQUEST_FORWARD_PLAIN";

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setJsonParser(JSONParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public Response(final Dispatcher dispatcher) {
        heads = new HashMap<>();
        this.dispatcher = dispatcher;
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

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Map<String, String> getHeads() {
        return heads;
    }

    public boolean addHeads(final String key, final String value) {
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

    public void setType(final String type) {
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

    public void setServlet(final String servlet) {
        if (StringUtils.isNotEmpty(servlet) && !servlet.contains(String.valueOf(HTTPStatus.COLON))) {
            this.servlet = servlet;
        }
    }

    public String getServlet() {
        return servlet;
    }

    private byte[] createHeadInfo(final List<Cookie> cookies, final boolean options, final boolean src) {
        final StringBuilder headInfo = new StringBuilder();
        //http version，status code，description
        headInfo.append(HTTPStatus.HTTP1_1).append(HTTPStatus.BLANK).append(code).append(HTTPStatus.BLANK);
        switch (code) {
            case HTTPStatus.CODE_200:
                headInfo.append(HTTPStatus.CODE_200_DESC);
                break;
            case HTTPStatus.CODE_206:
                headInfo.append(HTTPStatus.CODE_206_DESC);
                break;
            case HTTPStatus.CODE_302:
                headInfo.append(HTTPStatus.CODE_302_DESC);
                break;
            case HTTPStatus.CODE_304:
                headInfo.append(HTTPStatus.CODE_304_DESC);
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
        headInfo.append(HTTPStatus.SERVER).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(HTTPStatus.SERVER_DESC).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.DATE).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(new Date()).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.CONNECTION).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(dispatcher.isKeepAlive() ? HTTPStatus.KEEPALIVE : HTTPStatus.CLOSE).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.CONTENTTYPE).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(type).append(HTTPStatus.COOKIE_SPLITER)
                .append(HTTPStatus.CHARSET).append(HTTPStatus.PARAM_KV_SPLITER).append(charset).append(HTTPStatus.CRLF);
        if (options) {
            headInfo.append(HTTPStatus.ALLOW).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(HTTPStatus.GET).append(HTTPStatus.COMMA)
                    .append(HTTPStatus.POST).append(HTTPStatus.COMMA).append(HTTPStatus.PUT).append(HTTPStatus.COMMA).append(HTTPStatus.DELETE)
                    .append(HTTPStatus.CRLF);
        }
        if (src) {
            headInfo.append(HTTPStatus.CACAHE_CONTROL).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(HTTPStatus.CACHAE_CONTROL_DESC).append(max_age).append(HTTPStatus.CRLF);
            String time = dispatcher.request.getHeads().get(HTTPStatus.IF_MODIFIED_SINCE);
            String etag = dispatcher.request.getHeads().get(HTTPStatus.IF_NONE_MATCH);
            headInfo.append(HTTPStatus.LASTMODIFIED).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(time).append(HTTPStatus.CRLF);
            headInfo.append(HTTPStatus.ETAG).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(etag).append(HTTPStatus.CRLF);
        }

        if (code == HTTPStatus.CODE_206) {
            headInfo.append(HTTPStatus.ACCEPTRANGES).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(HTTPStatus.BYTES).append(HTTPStatus.CRLF);
            headInfo.append(HTTPStatus.CONTENTRANGE).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(HTTPStatus.BYTES).append(HTTPStatus.BLANK).append(start)
                    .append(HTTPStatus.MUPART_DESC_LINE.charAt(0)).append(String.valueOf(end)).append(HTTPStatus.SLASH).append(String.valueOf(total)).append(HTTPStatus.CRLF);
        }
        //The browser decides to accept the data length according to the content length. If the length is not specified, all resource requests will be pending until the timeout except for the non resource request status such as HTTP 304
        headInfo.append(HTTPStatus.CONTENTLENGTH).append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(len).append(HTTPStatus.CRLF);
        heads.forEach((k, v) -> headInfo.append(k).append(HTTPStatus.COLON).append(v).append(HTTPStatus.CRLF));
        //build cookies
        if (CollectionUtil.isNotEmptry(cookies)) {
            final StringBuilder sb = new StringBuilder();
            cookies.forEach(cookie -> {
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
                        sb.append(HTTPStatus.SECURE).append(HTTPStatus.PARAM_KV_SPLITER).append(true)
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

    public byte[] handle(final String method, final boolean interceptor) {
        //OPTIONS is HTTP pre-request，just return ok signal or other bad request
        boolean options = StringUtils.isNotEmpty(method) && method.equals(HTTPStatus.OPTIONS), src = false;
        if (code == HTTPStatus.CODE_400 || code == HTTPStatus.CODE_405 || options) {
            if (options) {
                code = HTTPStatus.CODE_200;
            }
            return createHeadInfo(null, options, src);
        }
        if (interceptor) {
            dispatcher.handlePre();
        }
        try {
            if (dispatcher.request.isRedirect()) {
                code = HTTPStatus.CODE_302;
                addHeads(HTTPStatus.LOCATION, dispatcher.request.getReqUrl());
            } else if (dispatcher.request.getParams().containsKey(PLAIN)) {
                type = HTTPStatus.PLAIN;
                Object obj = dispatcher.request.getParams().get(PLAIN).get(0);
                if (obj != null) {
                    if (!(obj instanceof String)) {
                        obj = jsonParser.getJson(obj);
                    }
                    content = obj.toString().getBytes(Charset.forName(charset));
                    len = content.length;
                }
                code = HTTPStatus.CODE_200;
            } else if (StringUtils.isNotEmpty(servlet)) {
                final Map<String, List<Object>> params = dispatcher.request.getParams();
                //init object instance just support basic data type and string type
                final Servlet handler = ClassUtils.getClass(servlet, dispatcher.request.getDispatcher().server.getRequestMappingHandler().getBeanPool(), true);
                //requestScope
                ClassUtils.initWithParams(handler, params);
                //sessionScope
                ClassUtils.initWithParams(handler, dispatcher.request.getSession().getAttrbuites());
                //mvcScope
                ClassUtils.initWithObjectList(handler, dispatcher.request.getObjectList());
                if (handler != null) {
                    String ret = null;
                    if (method.equals(HTTPStatus.GET)) {
                        ret = handler.doGet(dispatcher.request, this);
                    } else if (method.equals(HTTPStatus.POST)) {
                        ret = handler.doPost(dispatcher.request, this);
                    }
                    if (dispatcher.request.isRedirect()) {
                        code = HTTPStatus.CODE_302;
                        addHeads(HTTPStatus.LOCATION, dispatcher.request.getReqUrl());
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
            } else if (code == HTTPStatus.CODE_200 || code == HTTPStatus.CODE_206) {
                src = true;
                if (code == HTTPStatus.CODE_200) {
                    code = HTTPStatus.CODE_304;
                }
                final String etag = dispatcher.request.getHeads().get(HTTPStatus.IF_NONE_MATCH);
                final String time = dispatcher.request.getHeads().get(HTTPStatus.IF_MODIFIED_SINCE);
                if (!StringUtils.isNotEmpty(time) || !StringUtils.isNotEmpty(etag) || JSP.NULL.equals(etag)) {
                    dispatcher.request.getHeads().put(HTTPStatus.IF_MODIFIED_SINCE, new Date().toString());
                    dispatcher.request.getHeads().put(HTTPStatus.IF_NONE_MATCH, String.valueOf(dispatcher.server.getIdWorker().nextId()));
                    if (code == HTTPStatus.CODE_304) {
                        code = HTTPStatus.CODE_200;
                    }
                }
                String cahce = dispatcher.request.getHeads().get(HTTPStatus.CACAHE_CONTROL);
                if (StringUtils.isNotEmpty(cahce) && cahce.equals(HTTPStatus.NO_CACHE)) {
                    if (code == HTTPStatus.CODE_304) {
                        code = HTTPStatus.CODE_200;
                    }
                    src = false;
                } else {
                    cahce = dispatcher.request.getHeads().get(HTTPStatus.PRAGMA);
                    if (StringUtils.isNotEmpty(cahce) && cahce.equals(HTTPStatus.NO_CACHE)) {
                        if (code == HTTPStatus.CODE_304) {
                            code = HTTPStatus.CODE_200;
                        }
                        src = false;
                    }
                }
            } else {
                handle_404_NOT_FOUND();
            }
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "handle" + LOG.LOG_POS, dispatcher.server.getServerName(), LOG.EXCEPTION_DESC, e);
            handle_500_SERVER_ERROR(e.getMessage());
        }
        if (interceptor) {
            dispatcher.handleAfter();
        }
        final byte[] heads = createHeadInfo(dispatcher.request.getCookies(), false, src);
        final byte[] datas = new byte[heads.length + (content != null ? content.length : 0)];
        System.arraycopy(heads, 0, datas, 0, heads.length);
        if (content != null) {
            System.arraycopy(content, 0, datas, heads.length, content.length);
        }
        return datas;
    }

    public void redirect(String path) {
        dispatcher.request.setRedirect(true);
        dispatcher.request.setReqUrl(path);
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
        content = null;
        jsonParser = dispatcher.server.getJsonParser();
        len = 0;
        code = 0;
        max_age = 0;
        start = 0;
        end = 0;
        total = 0;
        if (CollectionUtil.isNotEmptry(heads)) {
            heads.clear();
        }
        charset = HTTPStatus.DEFAULTENCODING;
        type = HTTPStatus.PLAIN;
    }
}
