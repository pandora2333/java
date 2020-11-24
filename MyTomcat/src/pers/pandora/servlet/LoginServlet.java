package pers.pandora.servlet;

import java.util.List;
import java.util.Map;
@Deprecated
public class LoginServlet implements Servlet{
    private  String username;
    private  String password;
    private String content;
    private String head;
    @Override
    public void service() {
        if(username!=null&&password!=null) {
            if (password.equals("123")&&username.equals("tom")){
                head = "登陆成功!";
                content = "欢迎:"+username+"回来!";
                return;
            }
        }
        head="登陆失败!~";
        content = "用户名或密码不正确!";
    }

    @Override
    public String doGet(Request request,Response response) {
        StringBuilder sb = new StringBuilder();
        Map params = request.getParams();
        if(params.get("username")!=null&&params.get("password")!=null) {
            username = ((List<String>)params.get("username")).get(0);
            password = ((List<String>) params.get("password")).get(0);
            service();
            sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>")
                    .append(head)
                    .append("</title></head><body>")
                    .append(content)
                    .append("</body></html>");
        }
        return sb.toString();
    }

    @Override
    public String doPost(Request request,Response response) {
        return doGet(request,response);
    }
}
