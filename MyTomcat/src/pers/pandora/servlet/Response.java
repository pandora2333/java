package pers.pandora.servlet;

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

    private String servlet;

    private String charset = HTTPStatus.DEFAULTENCODING;
    //存储正文长度
    private int len;
    //正文
    private StringBuilder content;
    //资源类型
    private String type = HTTPStatus.TEXT_HTML;
    //静态资源地址
    private String resource;

    private Dispatcher dispatcher;
    //响应状态码
    private int code;

    public static final String PLAIN = "MODELANDVIEW_REQUEST_FORWARD_PLAIN";


    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private Map<String, String> heads;

    public Response(Dispatcher dispatcher) {
        heads = new HashMap<>();
        this.dispatcher = dispatcher;
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
        if (StringUtils.isNotEmpty(servlet) && !servlet.contains(String.valueOf(HTTPStatus.COLON))) {
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
        //2.响应头
        headInfo.append(HTTPStatus.SERVER + HTTPStatus.COLON + HTTPStatus.BLANK + HTTPStatus.SERVER_DESC).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.DATE + HTTPStatus.COLON + HTTPStatus.BLANK).append(new Date()).append(HTTPStatus.CRLF);
        headInfo.append(HTTPStatus.CONTENTTYPE + HTTPStatus.COLON + HTTPStatus.BLANK + type + HTTPStatus.COOKIE_SPLITER +
                HTTPStatus.CHARSET + HTTPStatus.COOKIE_KV_SPLITE + charset).append(HTTPStatus.CRLF);
        //正文长度，字节长度
        headInfo.append(HTTPStatus.CONTENTLENGTH + HTTPStatus.COLON + HTTPStatus.BLANK).append(len).append(HTTPStatus.CRLF);
        for (Map.Entry<String, String> head : heads.entrySet()) {
            headInfo.append(head.getKey() + HTTPStatus.COLON + head.getValue()).append(HTTPStatus.CRLF);
        }
        //构建Cookie头
        if (cookies != null && cookies.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Cookie cookie : cookies) {
                if (cookie != null && cookie.isNeedUpdate()) {
                    sb.append(HTTPStatus.SET_COOKIE);
                    sb.append(HTTPStatus.COLON).append(HTTPStatus.BLANK).append(cookie.getKey()).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getValue())
                            .append(HTTPStatus.COOKIE_SPLITER);
                    sb.append(HTTPStatus.VERSION).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getVersion())
                            .append(HTTPStatus.COOKIE_SPLITER);
                    if (StringUtils.isNotEmpty(cookie.getExpires())) {
                        sb.append(HTTPStatus.EXPIRES).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getExpires())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (cookie.getMax_age() != 0) {
                        sb.append(HTTPStatus.MAXAEG).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getMax_age())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getDoamin())) {
                        sb.append(HTTPStatus.DOMAIN).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getMax_age())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (StringUtils.isNotEmpty(cookie.getDoamin())) {
                        sb.append(HTTPStatus.PATH).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getPath())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    if (cookie.getSecure() > 0) {
                        sb.append(HTTPStatus.SECURE).append(HTTPStatus.COOKIE_KV_SPLITE).append(cookie.getSecure())
                                .append(HTTPStatus.COOKIE_SPLITER);
                    }
                    sb.append(HTTPStatus.CRLF);
                    headInfo.append(sb);
                    sb.delete(0, sb.length());
                    cookie.setNeedUpdate(false);
                }
            }
        }
        headInfo.append(HTTPStatus.CRLF);//分隔符
        return headInfo;
    }

    public String handle(String method, Request request) {
        if (content == null) {
            content = new StringBuilder();
        }
        handlePre(request);
        try {
            if (request != null && request.getParams().containsKey(PLAIN)) {
                type = HTTPStatus.PLAIN;
                content.append(request.getParams().get(HTTPStatus.PLAIN).get(0));//保留JSON序列化扩展
                len = content.toString().getBytes(charset).length;
                code = 200;
            } else if (StringUtils.isNotEmpty(servlet)) {
                Map<String, List<Object>> params = request.getParams();
                //初始化对象赋值只支持基本数据类型和String类型
                Servlet handler = ClassUtils.getClass(servlet, params);
                initRequstObjectList(request.getObjectList(), handler);
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
            } else if (resource == null) {
                handle_404_NOT_FOUND();
            } else {
                File file = new File(dispatcher.server.getRootPath() + resource);
                len += file.length();
                code = file.exists() ? 200 : 404;
            }
        } catch (Exception e) {
            handle_500_SERVER_ERROR(e.getMessage());
        }
        handleAfter(request);
        return createHeadInfo(request.getCookies()).append(content).toString();
    }

    private boolean handleAfter(Request request) {
        for (Pair<Integer, Interceptor> interceptor : dispatcher.server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().afterMethod(request, this)) {
                return false;
            }
        }
        return true;
    }

    private boolean handlePre(Request request) {
        for (Pair<Integer, Interceptor> interceptor : dispatcher.server.getRequestMappingHandler().getInterceptors()) {
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
            System.out.println("当前编码错误: " + charset + "\n编码内容： " + content);
        }
        code = 500;
    }

    private void handle_404_NOT_FOUND() throws UnsupportedEncodingException {
        content.append(HTTPStatus.CODE_404_DESC);
        type = HTTPStatus.PLAIN;
        len = content.toString().getBytes(charset).length;
        code = 404;
    }

    private void initRequstObjectList(Map<String, Object> objectList, Servlet handler) {
        ClassUtils.initWithObjectList(objectList, handler);
    }


    void reset() {
        servlet = null;
        resource = null;
        content = null;
        len = 0;
        code = 0;
    }
}
