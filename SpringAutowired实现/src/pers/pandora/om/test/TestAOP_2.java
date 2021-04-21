package pers.pandora.om.test;

import pers.pandora.om.annotation.After;
import pers.pandora.om.annotation.Aspect;
import pers.pandora.om.annotation.Before;
import pers.pandora.om.annotation.Throw;
import pers.pandora.om.core.JoinPoint;

@Deprecated
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
