package pers.pandora.servlet;

import java.util.Map;

public interface Servlet {
    public  void service();//提供一些初始化操作
    public  String  doGet(Map params,Request request,Response response);
    public String doPost(Map params,Request request,Response response);
}
