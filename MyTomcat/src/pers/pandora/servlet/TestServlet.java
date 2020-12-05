package pers.pandora.servlet;

import pers.pandora.core.Request;
import pers.pandora.core.Response;
import pers.pandora.vo.Tuple;

import java.util.Map;

@Deprecated
public class TestServlet implements Servlet {
    @Override
    public void service() {

    }

    @Override
    public String doGet(Request request, Response response) {
        for(Map.Entry<String, Tuple<String, String, byte[]>> entry : request.getUploadFiles().entrySet()){
            request.saveFileData(entry.getKey(),null);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>")
                .append("图片")
                .append("</title></head><body>")
                .append("<img src='http://localhost:8080/static/114.png'></img>")
                .append("<img src='http://localhost:8080/static/1.png'></img>")
                .append("test页面")
                .append("</body></html>");
        return sb.toString();
    }

    @Override
    public String doPost(Request request, Response response) {
        return doGet(request, response);
    }
}
