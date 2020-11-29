package pers.pandora.servlet;

import pers.pandora.core.Cookie;
import pers.pandora.core.Request;
import pers.pandora.core.Response;

import java.util.List;

@Deprecated
public class LoginServlet implements Servlet {
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
//        Map params = request.getParams();
//        if(params.get("username")!=null&&params.get("password")!=null) {
//            username = ((List<String>)params.get("username")).get(0);
//            password = ((List<String>) params.get("password")).get(0);
//            service();
//            sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>")
//                    .append(head)
//                    .append("</title></head><body>")
//                    .append(content)
//                    .append("</body></html>");
//        }
        //Cookie测试
        List<Cookie> cookies = request.getCookies();
        System.out.println("current cookies:" + cookies);
        Cookie cookie = new Cookie();
        cookie.setKey("current_uesr");
        cookie.setValue("Pandora WebServer");
        cookie.setMax_age(60);
        cookie.setPath("/static");
//        Date date = new Date();
//        date.setMinutes(date.getMinutes()+1);
//        System.out.println("cookie:"+cookie);
//        cookie.setExpires(date.toString());
        cookies.add(cookie);
        String value1 = "user=pandora; Version=1; Domain=localhost; Max-Age=60; Path=/static;";
        response.addHeads("Set-Cookie",value1);
//        response.addHeads("Set-Cookie",value2);
        //重定向测试
        response.addHeads("Location","/");
        response.setCode(302);

        return sb.toString();
    }

    @Override
    public String doPost(Request request, Response response) {
        return doGet(request,response);
    }
}
