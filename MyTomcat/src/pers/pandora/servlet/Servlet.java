package pers.pandora.servlet;

public interface Servlet {
    public  void service();//提供一些初始化操作
    public  String  doGet(Request request,Response response);
    public String doPost(Request request,Response response);
}
