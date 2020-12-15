package pers.pandora.controller;

import pers.pandora.annotation.*;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.vo.User;
import pers.pandora.core.Request;
import pers.pandora.core.Response;

/**
 * Introducing @Controller mode of springMVC
 */
@Deprecated
@Controller("/test")
public class TestController {
    @RequestMapping(value = "/loginUser.do")
    public String login(Request request, @RequestBody User user, Response response) {
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
    public User testHello(@PathVariable("password")int pwd,@PathVariable("username")String username) {
        System.out.println("user login:" + username + ":" + pwd);
        System.out.println("restful api...");
        User user = new User();
        user.setUsername("Hello World!");
        user.setPassword("123");
        return user;
    }
}
