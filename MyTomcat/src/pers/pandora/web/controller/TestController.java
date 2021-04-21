package pers.pandora.web.controller;

import pers.pandora.common.web.Controller;
import pers.pandora.web.annotation.*;
import pers.pandora.web.constant.HTTPStatus;
import pers.pandora.web.vo.User;
import pers.pandora.web.core.Request;
import pers.pandora.web.core.Response;

import java.util.List;

/**
 * Introducing @Controller mode of springMVC
 */
@Deprecated
@Controller("/test")
public class TestController {
    @RequestMapping(value = "/loginUser.do")
    public String login(Request request, @RequestBody("user") List<User> user, Response response) {
        System.out.println("user login:" + user);
        response.addHeads("test", "test response add head");
        return "/test.jsp";
    }

    @RequestMapping(value = "/loginUser2.do")
    public String login2(@RequestParam(value = "username", defaultValue = "pandora") String username, @RequestParam("password") String pwd) {
        System.out.println("user登录:" + username + ":" + pwd);
        return "/login";
    }

    @ResponseBody
    @RequestMapping(value = "/haha/{username}/test/{password}/testHello.do",method = HTTPStatus.PUT)
    public User testHello(@PathVariable("password")int pwd, @PathVariable("username")String username) {
        System.out.println("user login:" + username + ":" + pwd);
        System.out.println("restful api...");
        User user = new User();
        user.setUsername("Hello World!");
        user.setPassword("123");
        return user;
    }
}
