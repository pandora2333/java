package pers.pandora.controller;

import pers.pandora.annotation.*;
import pers.pandora.vo.User;
import pers.pandora.core.Request;
import pers.pandora.core.Response;

/**
 * 引入Spring MVC的@Controllert模式
 */
@Deprecated
@Controller("/test")
public class TestController {
    @RequestMapping("/loginUser.do")
    public String login(Request request, @RequestBody User user, Response response) {
        System.out.println("user登录:" + user);
        response.addHeads("test", "test response add head");
        return "/test.jsp";
    }

    @RequestMapping("/loginUser2.do")
    public String login2(@RequestParam("username") String username, @RequestParam("password") String pwd) {
        System.out.println("user登录:" + username + ":" + pwd);
        return "/login";
    }

    @ResponseBody
    @RequestMapping("/testHello.do")
    public String testHello() {
        System.out.println("通过...");
        return "Hello World!";
    }
}
