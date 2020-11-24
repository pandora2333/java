package pers.pandora.interceptor;

import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;
public interface Interceptor {
    public boolean preRequest(Request request, Response response);
    public boolean afterRequest(Request request, Response response);
}
