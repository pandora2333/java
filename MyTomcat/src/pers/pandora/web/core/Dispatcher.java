package pers.pandora.web.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.common.constant.LOG;
import pers.pandora.common.utils.StringUtils;
import pers.pandora.common.vo.Pair;
import pers.pandora.web.interceptor.Interceptor;
import pers.pandora.web.constant.HTTPStatus;
import pers.pandora.web.mvc.ModelAndView;
import pers.pandora.web.mvc.RequestMappingHandler;

import java.nio.ByteBuffer;

//servlet dispatcher
abstract class Dispatcher {

    protected static final Logger logger = LogManager.getLogger(Dispatcher.class.getName());

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

    void addUrlMapping(final String url, final String mvcClass) {
        if (StringUtils.isNotEmpty(url)) {
            server.context.put(url, mvcClass);
        }
    }

    protected Dispatcher() {
        request = new Request(this);
        response = new Response(this);
    }

    //main handle HTTP message
    final void dispatcher(final String reqMsg) {
        if (StringUtils.isNotEmpty(reqMsg)) {
            final String servlet = request.handle(reqMsg);
            //session rebuild
            if (request.getSession() == null || request.checkSessionInvalid(request.getSession().getSessionID())) {
                request.initSession(null);
            }
            if (servlet != null) {
                if (servlet.equals(HTTPStatus.OPTIONS)) {
                    pushClient(response.handle(HTTPStatus.OPTIONS, true), null);
                } else if (servlet.equals(RequestMappingHandler.MVC_CLASS)) {
                    //exec mvc operation
                    //add mvc path into the map
                    final ModelAndView mv = new ModelAndView(request.getReqUrl(), false);
                    mv.setRequest(request);
                    mv.setResponse(response);
                    handlePre();
                    server.requestMappingHandler.parseUrl(mv);
                    handleAfter();
                    if (StringUtils.isNotEmpty(mv.getPage())) {
                        if (mv.isJson() || request.isRedirect()) {
                            pushClient(response.handle(HTTPStatus.GET, false), null);
                        } else {
                            request.setAllowAccess(true);
                            dispatcher(HTTPStatus.GET + HTTPStatus.BLANK + mv.getPage() + HTTPStatus.BLANK + request.getVersion() + HTTPStatus.CRLF);
                        }
                    } else {
                        //not found, in MVC-uri paths
                        pushClient(response.handle(null, true), null);
                    }
                } else {
                    final String ss[] = servlet.split(HTTPStatus.HEAD_INFO_SPLITER);
                    java.io.File file = null;
                    if (ss.length == 2) {
                        file = new java.io.File(server.rootPath + ss[1]);
                    }
                    if (file != null) {
                        if (!file.exists()) {
                            file = null;
                            response.setCode(HTTPStatus.CODE_404);
                        } else {
                            response.setCode(HTTPStatus.CODE_200);
                            response.setType(ss[0]);
                            long len = file.length();
                            final String range = request.getHeads().get(HTTPStatus.RANGE);
                            if (StringUtils.isNotEmpty(range)) {
                                String tmp[] = range.split(HTTPStatus.PARAM_KV_SPLITER);
                                //Currently only bytes are processed
                                if (tmp.length == 2 && tmp[0].equals(HTTPStatus.BYTES)) {
                                    if (StringUtils.isNotEmpty(tmp[1])) {
                                        tmp = tmp[1].split(HTTPStatus.RANGEREGEX, -1);
                                        response.setTotal(len);
                                        if (tmp.length == 2) {
                                            response.setCode(HTTPStatus.CODE_206);
                                            final long start = Long.valueOf(tmp[0]);
                                            if (start < len) {
                                                response.setStart(start);
                                                if (StringUtils.isNotEmpty(tmp[1])) {
                                                    response.setEnd(Long.valueOf(tmp[1]));
                                                    if (response.getEnd() < len) {
                                                        len = Math.min(len - start + 1, response.getEnd() - start + 1);
                                                    } else {
                                                        response.handle_Code_416(len);
                                                    }
                                                } else {
                                                    len = Math.min(len - start + 1, server.sendBuffer);
                                                }
                                                response.setEnd(len + start - 1);
                                            } else {
                                                response.handle_Code_416(len);
                                            }
                                        } else {
                                            response.handle_Code_416(len);
                                        }
                                    } else {
                                        response.handle_Code_416(len);
                                    }
                                } else {
                                    response.handle_Code_416(len);
                                }
                            }
                            response.setLen(len);
                        }
                    }
                    response.setServlet(servlet);
                    pushClient(response.handle(request.getMethod(), true), response.isOutPutStaticFile() ? file : null);
                }
            } else {
                pushClient(response.handle(null, true), null);
            }
        }
    }

    void handleRequestCompleted() {
        for (Pair<Integer, Interceptor> interceptor : server.requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().completeRequest(request, response)) {
                logger.warn(LOG.LOG_PRE + "exec completeRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return;
            }
        }
    }

    void initRequest(final ByteBuffer data) {
        for (Pair<Integer, Interceptor> interceptor : server.requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().initRequest(request, data)) {
                logger.warn(LOG.LOG_PRE + "exec initRequest" + LOG.LOG_PRE, interceptor.getV().getClass().getName(), LOG.ERROR_DESC);
                return;
            }
        }
    }

    void handleAfter() {
        for (Pair<Integer, Interceptor> interceptor : server.requestMappingHandler.getInterceptors()) {
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
        for (Pair<Integer, Interceptor> interceptor : server.requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().preMethod(request, response)) {
                return;
            }
        }
    }

    protected abstract void pushClient(byte[] content, java.io.File staticFile);

}