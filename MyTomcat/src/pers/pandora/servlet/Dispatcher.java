package pers.pandora.servlet;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pers.pandora.mvc.ModelAndView;
import pers.pandora.mvc.RequestMappingHandler;
import pers.pandora.utils.JspParser;
import pers.pandora.utils.MapContent;
import pers.pandora.utils.XMLFactory;


//servlet分发器
public class Dispatcher implements  Runnable{
     private BufferedInputStream inputStream;
     private  BufferedOutputStream outputStream;
     private Socket client;
     private static String webConfig = "WebRoot/WEB-INF/web.xml";
     private  String mvcClass = "pers.pandora.mvc.RequestMappingHandler";
     private static volatile RequestMappingHandler requestMappingHandler;
     private static Map<String,MapContent> context;
     static{
    	 context = XMLFactory.parse(webConfig);
     }
    public Dispatcher(Socket client) {
        this.client = client;
        try {
            this.inputStream = new BufferedInputStream(client.getInputStream());
            this.outputStream = new BufferedOutputStream(client.getOutputStream());
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Dispatcher初始化出错!");
            close();
        }

    }

   public void close(){
        if(inputStream!=null){
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       if(outputStream!=null){
           try {
               outputStream.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
       if(client!=null&&!client.isClosed()){
           try {
               client.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }
    @Override
    public void run() {
        try {
            dispatcher(null,null,false);
            close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("分发执行出错!");
        }
    }
    //处理请求
    public void  dispatcher(String mvcUrl, Map<String, List<Object>> params,boolean initalJsp) throws Exception {
        String reqMsg = "";
        if(mvcUrl==null) {
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            reqMsg = new String(data);
        }else {
        	reqMsg =mvcUrl;
        }
        if (reqMsg != null&&!reqMsg.equals("")) {
            JspParser jsp = null;
            Request request = null;
            if(reqMsg.substring(reqMsg.indexOf("/"), reqMsg.indexOf("HTTP")).contains(".jsp")&&!initalJsp){//前置判断是否需要解析jsp
                jsp = new JspParser(null,context);//此时不支持#{}表达式
                request = new Request(reqMsg, context, mvcClass, jsp);
            }else {
                request = new Request(reqMsg, context, mvcClass, null);
            }
            String servlet = request.handle();
            if(params!=null){
                request.setParams(params);
            }
            if(servlet!=null&&servlet.equals(mvcClass)){
                synchronized (Dispatcher.class) {
                    if (requestMappingHandler == null) {
                        requestMappingHandler = (RequestMappingHandler) Class.forName(mvcClass).newInstance();
                    }
                }
                //执行mvc操作
                requestMappingHandler.setModelAndView(new ModelAndView(request.getReqUrl(),request.getParams(),false));
                ModelAndView mv = requestMappingHandler.parseUrl(request.getReqUrl());
                //请求重定向
                if(mv!=null) {
                    JspParser jspParser = null;
                    if(mv.getPage().contains(".jsp")) {
                        jspParser = new JspParser(requestMappingHandler.getValueStack(), context);
                    }
                    if (mv.isJson()) {
                        List temp = new LinkedList<>();
                        temp.add(mv.getPage());
                        if(params==null) {
                        	params = new ConcurrentHashMap<>();
                        }
                        params.put("json", temp);
                        new Response(mvcClass).handle("GET", outputStream, params);
                    } else {
                        if(jspParser!=null) {
                            jspParser.parse("WebRoot" + mv.getPage());
                        }
                        dispatcher("GET " + mv.getPage() + " HTTP/1.1", mv.getParams(),true);
                    }
                }
            }else {
                new Response(servlet).handle(request.getMethod(), outputStream, request.getParams());
                if(mvcUrl!=null&&params!=null){
                    close();
                }
            }
        }
    }
}