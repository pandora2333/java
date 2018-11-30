package pers.pandora.servlet;

import java.util.List;
import java.util.Map;

public class LoginServlet implements  Servlet {
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
    public String doGet(Map params) {
        Map te = params;
        StringBuilder sb = new StringBuilder();
        if(((List<String>)params.get("username"))!=null&&((List<String>) params.get("password"))!=null) {
            username = ((List<String>)params.get("username")).get(0);
            password = ((List<String>) params.get("password")).get(0);
            service();
            sb.append("<html><head><title>")
                    .append(head)
                    .append("</title></head><body>")
                    .append(content)
                    .append("</body></html>");
        }
        return sb.toString();
    }

    @Override
    public String doPost(Map params) {

        return doGet(params);
    }
}
