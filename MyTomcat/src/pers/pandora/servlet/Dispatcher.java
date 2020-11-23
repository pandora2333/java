package pers.pandora.servlet;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pers.pandora.mvc.ModelAndView;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.JspParser;
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
    private static volatile RequestMappingHandler requestMappingHandler;
    private static volatile Map<String, MapContent> context;
    protected Request request = new Request(null, context, mvcClass, null);
    protected Response response = new Response(null, null, null);

    static {
        context = XMLFactory.parse(webConfig);
        try {
            requestMappingHandler = (RequestMappingHandler) Class.forName(mvcClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public Dispatcher(Socket client) {
        this.client = client;
    }

    protected Dispatcher() {
        //留给扩展server的后门
    }

    public void close() {
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

    @Override
    public void run() {
        try {
            int len = client.getInputStream().available();
            byte[] data = new byte[len];
            client.getInputStream().read(data);
            dispatcher(new String(data), null, null);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("分发执行出错!");
        } finally {
            close();
        }
    }

    //处理请求
    public void dispatcher(String reqMsg, Map<String, List<Object>> params, JspParser jsp) throws Exception {
        if (StringUtils.isNotEmpty(reqMsg)) {
            //暂不支持test.jsp?username=xx方式
            request.setMsg(reqMsg);
            if (reqMsg.contains(Request.JSP) && jsp == null) {
                jsp = new JspParser(requestMappingHandler.getValueStack(), context);
            }
            request.setJspParser(jsp);
            String servlet = request.handle();
            if (params != null) {
                request.setParams(params);
            }
            if (servlet != null && servlet.equals(mvcClass)) {
                //执行mvc操作
                requestMappingHandler.setModelAndView(new ModelAndView(request.getReqUrl(), request.getParams(), false));
                ModelAndView mv = requestMappingHandler.parseUrl(request.getReqUrl());
                //请求重定向
                if (mv != null) {
                    if (mv.isJson()) {
                        List temp = new LinkedList<>();
                        temp.add(mv.getPage());
                        if (params == null) {
                            params = new ConcurrentHashMap<>();
                        }
                        params.put(Response.PLAIN, temp);
                        response.setServlet(mvcClass);
                        pushClient(response.handle(Request.GET, params, request), null);
                        response.clear();
                    } else {
                        dispatcher(Request.GET + Request.BLANK + mv.getPage() + Request.BLANK + Request.HTTP1_1, mv.getParams(), jsp);
                    }
                }
            } else if (servlet != null) {
                String ss[] = servlet.split(Request.spliter);
                String file = ss.length == 2 ? ss[1] : null;
                if (jsp != null) {
                    servlet += Request.LINE + jsp.getJspNum();
                }
                response.setResource(ss.length == 2 ? ss[1] : null);
                response.setServlet(servlet);
                response.setType(ss.length == 2 ? ss[0] : null);
                String content = response.handle(request.getMethod(), request.getParams(), request);
                pushClient(content, file);
                response.clear();
            }
        }
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
}