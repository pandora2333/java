package pers.pandora.web.servlet;

import pers.pandora.om.annotation.Autowired;
import pers.pandora.om.annotation.PropertySource;
import pers.pandora.om.annotation.Value;
import pers.pandora.web.core.Response;
import pers.pandora.web.test.Client;
import pers.pandora.web.constant.HTTPStatus;
import pers.pandora.web.core.Request;

@Deprecated
@PropertySource("TestServlet.properties")
public class TestServlet implements Servlet {

    @Value("testOM")
    private String test;

    @Autowired
    Client client;

    @Override
    public void service() {

    }

    @Override
    public String doGet(Request request, Response response) {
        request.getUploadFiles().forEach((k,v)->{
            v.forEach(file -> request.saveFileData(null,file));
        });
//        System.out.println("test:" + test + ",client:" + client.getTestOM());
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>")
                .append("picture")
                .append("</title></head><body>")
                .append("<img src='http://localhost:8080/static/114.png'></img>")
                .append("<img src='http://localhost:8080/static/1.png'></img>")
                .append("test page")
                .append("</body></html>");
        response.setType(HTTPStatus.TEXT_HTML);
        return sb.toString();
    }

    @Override
    public String doPost(Request request, Response response) {
        return doGet(request, response);
    }
}
