package pers.pandora.interceptor;

import pers.pandora.core.Request;
import pers.pandora.core.Response;

import java.nio.ByteBuffer;

public interface Interceptor {
    //修改请求参数，修改响应信息（比如编码）
    public boolean preMethod(Request request, Response response);

    //修改请求参数（请求转发），修改响应信息（加解密/加解压缩响应信息）
    public boolean afterMethod(Request request, Response response);

    //做资源回收
    public boolean completeRequest(Request request, Response response);

    //初始化分析HTTP请求,对请求信息做资源预处理 （比如修改HTTP请求解析编码，修改请求原文信息(加解密/加解压缩请求信息)）
    public boolean initRequest(Request request, ByteBuffer data);
}
