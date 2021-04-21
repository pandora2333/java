package pers.pandora.web.servlet;

import pers.pandora.web.core.Request;
import pers.pandora.web.core.Response;

public interface Servlet {
    //It provides some initialization operations
    void service();
    String  doGet(Request request, Response response);
    String doPost(Request request, Response response);
}
