package pers.pandora.web.interceptor;

import pers.pandora.web.core.Request;
import pers.pandora.web.core.Response;

import java.nio.ByteBuffer;

public interface Interceptor {
    //Modify request parameters, modify response information (such as encoding)
    boolean preMethod(Request request, Response response);

    //Modify request parameters (request forwarding), modify response information (encryption / decryption / decompression response information)
    boolean afterMethod(Request request, Response response);

    //Recycling resources
    boolean completeRequest(Request request, Response response);

    //Initialize and analyze the HTTP request, and preprocess the request information (such as modifying the HTTP request parsing code and modifying the original request information (encryption / decryption / decompression request information))
    boolean initRequest(Request request, ByteBuffer data);
}
