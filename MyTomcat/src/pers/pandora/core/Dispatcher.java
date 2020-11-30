package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.vo.Pair;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.mvc.ModelAndView;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.StringUtils;

//servlet分发器
abstract class Dispatcher {

    protected static Logger logger = LogManager.getLogger(Dispatcher.class);

    protected Server server;

    protected Request request;

    protected Response response;

    public void addUrlMapping(String url, String mvcClass) {
        if (StringUtils.isNotEmpty(url)) {
            server.getContext().put(url, mvcClass);
        }
    }

    protected Dispatcher() {
        request = new Request(this);
        response = new Response(this);
    }

    //处理请求
    //只有第一次返回不是404应答时候才会建立与浏览器sessionID联系
    public final void dispatcher(String reqMsg) {
        if (StringUtils.isNotEmpty(reqMsg)) {
            String servlet = request.handle(reqMsg);
            if (servlet != null) {
                if (servlet.equals(RequestMappingHandler.MVC_CLASS)) {
                    //执行mvc操作
                    //将mvc重定向路径加入map
                    ModelAndView mv = new ModelAndView(request.getReqUrl(), false);
                    mv.setRequest(request);
                    mv.setResponse(response);
                    RequestMappingHandler.parseUrl(mv);
                    if (StringUtils.isNotEmpty(mv.getPage())) {
                        if (mv.isJson()) {
                            pushClient(response.handle(HTTPStatus.GET, request), null);
                        } else {
                            dispatcher(HTTPStatus.GET + HTTPStatus.BLANK + mv.getPage() + HTTPStatus.BLANK + HTTPStatus.HTTP1_1);
                        }
                    } else {
                        //找不到对应MVC拦截uri路径
                        pushClient(response.handle(null, request), null);
                    }
                } else {
                    String ss[] = servlet.split(HTTPStatus.HEAD_INFO_SPLITER);
                    java.io.File file = null;
                    if (ss.length == 2) {
                        file = new java.io.File(server.getRootPath() + ss[1]);
                    }
                    if (file != null) {
                        if (!file.exists()) {
                            file = null;
                        } else {
                            response.setResource(true);
                            response.setType(ss[0]);
                            response.setLen(file.length());
                        }
                    }
                    response.setServlet(servlet);
                    String content = response.handle(request.getMethod(), request);
                    pushClient(content, file);
                }
            } else {
                pushClient(response.handle(null, request), null);
            }
        }
    }

    protected boolean handleRequestCompleted() {
        for (Pair<Integer, Interceptor> interceptor : RequestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().completeRequest(request, response)) {
                logger.warn(LOG.LOG_PRE + "exec completeRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return false;
            }
        }
        return true;
    }

    protected boolean initRequest(byte[] data) {
        for (Pair<Integer, Interceptor> interceptor :  RequestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().initRequest(request, data)) {
                logger.warn(LOG.LOG_PRE + "exec initRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return false;
            }
        }
        return true;
    }

    protected abstract void pushClient(String content, java.io.File staticFile);

}