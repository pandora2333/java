package pers.pandora.servlet;

import pers.pandora.utils.ClassUtils;

import java.io.*;
import java.util.Date;
import java.util.Map;

public class Response {
    private Servlet servlet;
    //存储头信息
    private StringBuilder headInfo;
    private static final String CRLF = "\n";
    private static final String BLANK = " ";
    //存储正文长度
    private int len;
    //正文
    private StringBuilder content;
    //静态资源
    private String resource;

    /**
     * 构建响应头
     */
    private void createHeadInfo(int code) {
        headInfo = new StringBuilder();
        //1.http协议版本，状态代码，描述
        headInfo.append("HTTP/1.1").append(BLANK).append(code).append(BLANK);
        switch (code) {
            case 200:
                headInfo.append("OK");
                break;
            case 404:
                headInfo.append("not found");
                break;
            case 500:
                headInfo.append("server erro");
                break;
            default:
                headInfo.append("erro code");
        }
        headInfo.append(CRLF);
        //2.响应头
        headInfo.append("Server:pandora Server/1.0.1").append(CRLF);
        headInfo.append("Date:").append(new Date()).append(CRLF);
        headInfo.append("Content-type:text/html;charset=utf8").append(CRLF);
        //正文长度，字节长度
        headInfo.append("Content-Length:").append(len).append(CRLF);
        headInfo.append(CRLF);//分隔符
    }

    public Response(String servlet) {
        if (servlet != null && !servlet.contains("static:")) {
            this.servlet = ClassUtils.getClass(servlet, Servlet.class);
        } else if (servlet != null && servlet.contains("static:")) {
            resource = servlet.split("static:")[1];
            if (resource.contains("?")) {
                resource = null;
            }
        }

    }

    public void handle(String method, BufferedOutputStream outputStream, Map params) {
        content = new StringBuilder();
        content.append(CRLF);
        if(resource!=null){
            try {
                handleStatic(resource,outputStream);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (servlet != null) {
            if (method.equals("GET")) {
                content.append(servlet.doGet(params));
            } else if (method.equals("POST")) {
                content.append(servlet.doPost(params));
            }
            len = content.toString().getBytes().length;
            createHeadInfo(200);
        } else{
            content.append("页面找不到!");
            len = content.toString().getBytes().length;
            createHeadInfo(404);
        }

        pushToClient(outputStream,null);
    }

    public void pushToClient(BufferedOutputStream outputStream,byte[] data){
        try {
            StringBuilder writer = new StringBuilder().append(headInfo);
            if (resource == null) {
                writer.append(content);
                outputStream.write(writer.toString().getBytes());
            }
            if(data!=null){
                outputStream.write(data);
            }
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //静态资源处理
    private void handleStatic(String msg, BufferedOutputStream outputStream) throws IOException {
        String temp = msg.substring(msg.lastIndexOf("/")+1);
        File file =  new File("WebRoot/"+temp);
        if(file!=null) {
            InputStream in = new FileInputStream(file);
            byte[] data = new byte[in.available()];
            len = in.available();
            in.read(data);
            createHeadInfo(200);
            pushToClient(outputStream,data);
        }
    }

}
