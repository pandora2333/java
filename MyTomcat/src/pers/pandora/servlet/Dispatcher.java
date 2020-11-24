package pers.pandora.servlet;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pers.pandora.bean.Pair;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.mvc.ModelAndView;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.MapContent;
import pers.pandora.utils.StringUtils;
import pers.pandora.utils.XMLFactory;

//servlet分发器
public class Dispatcher implements Runnable {

    private Socket client;
    public static final String ROOTPATH = "./WebRoot/";
    public static final String ROOTPATH2 = "./WebRoot";
    private static String webConfig = ROOTPATH + "WEB-INF/web.xml";
    private static final String mvcClass = "pers.pandora.mvc.RequestMappingHandler";
    private volatile RequestMappingHandler requestMappingHandler;
    private static Map<String, MapContent> context;
    protected Request request = new Request(context, mvcClass);
    protected Response response = new Response(null, null, null);

    static {
        context = XMLFactory.parse(webConfig);
//        try {
//            requestMappingHandler = (RequestMappingHandler) Class.forName(mvcClass).newInstance();
//        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//            e.printStackTrace();
//        }
    }


    public Dispatcher(Socket client) {
        this.client = client;
    }

    protected Dispatcher() {
        //留给扩展server的后门
    }

    void close() {
        if (client != null && !client.isClosed()) {
            try {
                if (client.getInputStream() != null) {
                    client.getInputStream().close();
                }
                if (client.getOutputStream() != null) {
                    client.getOutputStream().close();
                }
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Deprecated
    @Override
    public void run() {//兼容BIO模式
        try {
            int len = client.getInputStream().available();
            byte[] data = new byte[len];
            client.getInputStream().read(data);
            dispatcher(new String(data));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("分发执行出错!");
        } finally {
            close();
        }
    }

    //处理请求
    public final void dispatcher(String reqMsg) {
        if (StringUtils.isNotEmpty(reqMsg)) {
            String servlet = request.handle(reqMsg);
            if (servlet != null) {
                if (servlet.equals(mvcClass)) {
                    //执行mvc操作
                    requestMappingHandler.setModelAndView(new ModelAndView(request.getReqUrl(), request.getParams(), false));
                    Pair<ModelAndView, List<Object>> pair = requestMappingHandler.parseUrl(request.getReqUrl());
                    //请求重定向
                    if (pair != null) {
                        request.getJspParser().setValueObject(pair.getV());
                        if (pair.getK() != null) {
                            if (pair.getK().isJson()) {
                                List temp = new LinkedList<>();
                                temp.add(pair.getK().getPage());
                                Map<String, List<Object>> params = new ConcurrentHashMap<>();
                                params.put(Response.PLAIN, temp);
                                request.setParams(params);
                                response.setServlet(mvcClass);
                                //pre intercepter
                                if (!handlePre()) {
                                    return;
                                }
                                pushClient(response.handle(Request.GET, request), null);
                                //after intercepter
                                if (!handleAfter()) {
                                    return;
                                }
                                response.reset();
                            } else {
                                request.setParams(pair.getK().getParams());
                                //pre intercepter
                                if (!handlePre()) {
                                    return;
                                }
                                dispatcher(Request.GET + Request.BLANK + pair.getK().getPage() + Request.BLANK + Request.HTTP1_1);
                                //after intercepter
                                if (!handleAfter()) {
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    String ss[] = servlet.split(Request.spliter);
                    String file = ss.length == 2 ? ss[1] : null;
                    response.setResource(ss.length == 2 ? ss[1] : null);
                    response.setServlet(servlet);
                    response.setType(ss.length == 2 ? ss[0] : null);
                    //after intercepter
                    if (!handlePre()) {
                        return;
                    }
                    String content = response.handle(request.getMethod(), request);
                    pushClient(content, file);
                    //after intercepter
                    if (!handleAfter()) {
                        return;
                    }
                    response.reset();
                }
            } else {
                pushClient(response.handle(null, null), null);
            }
        }
    }

    private boolean handleAfter() {
        for (Pair<Integer, Interceptor> interceptor : requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().afterRequest(request, response)) {
                return false;
            }
        }
        return true;
    }

    private boolean handlePre() {
        for (Pair<Integer, Interceptor> interceptor : requestMappingHandler.getInterceptors()) {
            if (!interceptor.getV().preRequest(request, response)) {
                return false;
            }
        }
        return true;
    }

    protected void pushClient(String response, String staticFile) {
        if (client != null) {
            if (response != null) {
                try {
                    client.getOutputStream().write(response.getBytes(request.getCharset()));
                    client.getOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (staticFile != null) {
                try {
                    client.getOutputStream().write(Files.readAllBytes(Paths.get(ROOTPATH + staticFile)));
                    client.getOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void setRequestMappingHandler(RequestMappingHandler requestMappingHandler) {
        this.requestMappingHandler = requestMappingHandler;
    }

    public RequestMappingHandler getRequestMappingHandler() {
        return requestMappingHandler;
    }
}