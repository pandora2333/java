package pers.pandora.test;

import pers.pandora.annotation.After;
import pers.pandora.annotation.Aspect;
import pers.pandora.annotation.Before;
import pers.pandora.annotation.Throw;
import pers.pandora.core.JoinPoint;

@Aspect(2)
public class TestAOP_2 {

    @Before("pers\\.pandora\\.bean.*")
    public void beforeMethod(JoinPoint joinPoint){
        System.out.println("TestAOP_2 beforeMethod:" + joinPoint);
    }

    @After("pers\\.pandora\\.bean.*")
    public void afterMethod(JoinPoint joinPoint){
        System.out.println("TestAOP_2 afterMethod:" + joinPoint);
    }

    @Throw("pers\\.pandora\\.bean.*")
    public void throwMethod(JoinPoint joinPoint){
        System.out.println("TestAOP_2 throwMethod:" + joinPoint);
    }
}
