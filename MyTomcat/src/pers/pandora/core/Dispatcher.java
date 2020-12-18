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

import java.nio.ByteBuffer;

//servlet dispatcher
abstract class Dispatcher {

    protected static final Logger logger = LogManager.getLogger(Dispatcher.class);

    protected Server server;

    protected Request request;

    protected Response response;

    protected boolean keepAlive;

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    void addUrlMapping(String url, String mvcClass) {
        if (StringUtils.isNotEmpty(url)) {
            server.getContext().put(url, mvcClass);
        }
    }

    protected Dispatcher() {
        request = new Request(this);
        response = new Response(this);
    }

    //main handle HTTP message
    final void dispatcher(String reqMsg) {
        if (StringUtils.isNotEmpty(reqMsg)) {
            String servlet = request.getServlet();
            if (!request.isFlag()) {
                servlet = request.handle(reqMsg);
                request.setServlet(servlet);
            }
            if (servlet != null) {
                if (servlet.equals(HTTPStatus.OPTIONS)) {
                    pushClient(response.handle(HTTPStatus.OPTIONS, true), null);
                } else if (servlet.equals(RequestMappingHandler.MVC_CLASS)) {
                    //exec mvc operation
                    //add mvc path into the map
                    ModelAndView mv = new ModelAndView(request.getReqUrl(), false);
                    mv.setRequest(request);
                    mv.setResponse(response);
                    handlePre();
                    server.getRequestMappingHandler().parseUrl(mv);
                    handleAfter();
                    if (StringUtils.isNotEmpty(mv.getPage())) {
                        if (mv.isJson() || request.isRedirect()) {
                            pushClient(response.handle(HTTPStatus.GET, false), null);
                        } else {
                            request.setFlag(false);
                            dispatcher(HTTPStatus.GET + HTTPStatus.BLANK + mv.getPage() + HTTPStatus.BLANK + HTTPStatus.HTTP1_1);
                        }
                    } else {
                        //not found, in MVC-uri paths
                        pushClient(response.handle(null, true), null);
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
                            response.setCode(HTTPStatus.CODE_404);
                        } else {
                            response.setCode(HTTPStatus.CODE_200);
                            response.setType(ss[0]);
                            response.setLen(file.length());
                        }
                    }
                    response.setServlet(servlet);
                    pushClient(response.handle(request.getMethod(), true), response.getCode() == HTTPStatus.CODE_304 ? null : file);
                }
            } else {
                pushClient(response.handle(null, true), null);
            }
        }
    }

    void handleRequestCompleted() {
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().completeRequest(request, response)) {
                logger.warn(LOG.LOG_PRE + "exec completeRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return;
            }
        }
    }

    void initRequest(ByteBuffer data) {
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().initRequest(request, data)) {
                logger.warn(LOG.LOG_PRE + "exec initRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return;
            }
        }
    }

    void handleAfter() {
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().afterMethod(request, response)) {
                return;
            }
        }
    }

    void handlePre() {
        //handle json data
        if (request.isJson()) {
            request.handleJSON();
        }
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().preMethod(request, response)) {
                return;
            }
        }
    }

    protected abstract void pushClient(byte[] content, java.io.File staticFile);

}