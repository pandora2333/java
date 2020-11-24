package pers.pandora.interceptor;

import pers.pandora.annotation.Order;
import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;

@Deprecated
@Order
public class TestInterceptor_2 implements Interceptor {
    @Override
    public boolean preRequest(Request request, Response response) {
        System.out.println("preRequest:"+this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean afterRequest(Request request, Response response) {
        System.out.println("afterRequest:"+this.getClass().getSimpleName());
        return true;
    }
}
