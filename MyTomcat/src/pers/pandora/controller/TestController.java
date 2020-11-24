package pers.pandora.controller;

import pers.pandora.annotation.*;
import pers.pandora.bean.User;

/**
 * 引入Spring MVC的@Controllert模式
 */
@Deprecated
@Controller
public class TestController {
    @RequestMapping("/loginUser.do")
    public String  login(@RequestBody User user){//有且仅有一个实体类参数，无其他参数
        System.out.println("user登录:"+user);
        return "/test.jsp";
    }
    @RequestMapping("/loginUser2.do")
    public String  login2(@RequestParam String username,@RequestParam("password") String pwd){
        System.out.println("user登录:"+username+":"+pwd);
        return "/login";
    }
    @ResponseBody
    @RequestMapping("/testHello.do")
    public String testHello(){
    	System.out.println("通过...");
        return "Hello World!";
    }
}
