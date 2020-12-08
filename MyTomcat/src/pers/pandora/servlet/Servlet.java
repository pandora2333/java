package pers.pandora.servlet;

import pers.pandora.core.Request;
import pers.pandora.core.Response;

public interface Servlet {
    //It provides some initialization operations
    void service();
    String  doGet(Request request, Response response);
    String doPost(Request request, Response response);
}
