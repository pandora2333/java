package pers.pandora.mvc;

import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装参数和模型
 * 页面间，请求重定向传参
 */
public final class ModelAndView {
    private String page;
    private boolean isJson;
    private Request request;
    private Response response;

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setJson(boolean json) {
        isJson = json;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public boolean isJson() {
        return isJson;
    }

    public String getPage() {
        return page;
    }

    public ModelAndView(String page,boolean isJson) {
        this.page = page;
        this.isJson = isJson;
    }
}
