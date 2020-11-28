package pers.pandora.servlet;

import pers.pandora.vo.Pair;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.mvc.ModelAndView;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.server.Server;
import pers.pandora.utils.StringUtils;

//servlet分发器
public abstract class Dispatcher {

    protected Server server;

    protected Request request;

    protected Response response;

    public void addUrlMapping(String url, String mvcClass) {
        server.getContext().put(url, mvcClass);
    }

    protected Dispatcher() {
        request = new Request(this);
        response = new Response(this);
    }

    //处理请求
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
                    server.getRequestMappingHandler().parseUrl(mv);
                    if (mv.isJson()) {
                        pushClient(response.handle(HTTPStatus.GET, request), null);
                    } else {
                        dispatcher(HTTPStatus.GET + HTTPStatus.BLANK + mv.getPage() + HTTPStatus.BLANK + HTTPStatus.HTTP1_1);
                    }
                } else {
                    String ss[] = servlet.split(HTTPStatus.HEAD_INFO_SPLITER);
                    String file = ss.length == 2 ? ss[1] : null;
                    response.setResource(ss.length == 2 ? ss[1] : null);
                    response.setServlet(servlet);
                    response.setType(ss.length == 2 ? ss[0] : null);
                    String content = response.handle(request.getMethod(), request);
                    pushClient(content, file);
                }
            } else {
                pushClient(response.handle(null, request), null);
            }
        }
    }

    protected boolean handleRequestCompleted() {
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().completeRequest(request, response)) {
                throw new RuntimeException(interceptor.getV().getClass().getName() + "执行completeRequest资源处理出错");
            }
        }
        return true;
    }

    protected boolean initRequest(byte[] data) {
        for (Pair<Integer, Interceptor> interceptor : server.getRequestMappingHandler().getInterceptors()) {
            if (!interceptor.getV().initRequest(request, data)) {
                throw new RuntimeException(interceptor.getV().getClass().getName() + "执行initRequest资源处理出错");
            }
        }
        return true;
    }

    protected abstract void pushClient(String content, String staticFile);

}