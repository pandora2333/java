package pers.pandora.servlet;

import java.util.Map;

public class TestServlet implements  Servlet {
    @Override
    public void service() {

    }

    @Override
    public String doGet(Map params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>")
                .append("图片")
                .append("</title></head><body>")
                .append("<img src='http://localhost:8080/114.png'></img>")
                .append("test页面")
                .append("</body></html>");
        return sb.toString();
    }

    @Override
    public String doPost(Map params) {
        return doGet(params);
    }
}
