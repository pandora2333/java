package pers.pandora.servlet;

@Deprecated
public class TestServlet implements Servlet {
    @Override
    public void service() {

    }

    @Override
    public String doGet(Request request, Response response) {
        if (request.getFileData() != null) {
            request.setFilePath("e:/");
//            request.setFileName("test.jpg");
            request.saveFileData();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>")
                .append("图片")
                .append("</title></head><body>")
                .append("<img src='http://localhost:8080/114.png'></img>")
                .append("test页面")
                .append("</body></html>");
        return sb.toString();
    }

    @Override
    public String doPost(Request request, Response response) {
        return doGet(request, response);
    }
}
