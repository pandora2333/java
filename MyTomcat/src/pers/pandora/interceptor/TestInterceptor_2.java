package pers.pandora.interceptor;

import pers.pandora.annotation.Order;
import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;

@Deprecated
@Order
public class TestInterceptor_2 implements Interceptor {
    @Override
    public boolean preMethod(Request request, Response response) {
        System.out.println("preMethod:"+this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean afterMethod(Request request, Response response) {
        System.out.println("afterMethod:"+this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean completeRequest(Request request, Response response) {
        System.out.println("completeRequest:"+this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean initRequest(Request request, byte[] data) {
        System.out.println("initRequest:"+this.getClass().getSimpleName());
        return true;
    }
}