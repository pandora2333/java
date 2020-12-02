package pers.pandora.servlet;

import pers.pandora.core.Request;
import pers.pandora.core.Response;

public interface Servlet {
    //It provides some initialization operations
    public  void service();
    public  String  doGet(Request request, Response response);
    public String doPost(Request request, Response response);
}
