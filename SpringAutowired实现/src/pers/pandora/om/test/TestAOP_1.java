package pers.pandora.om.test;

import pers.pandora.om.annotation.After;
import pers.pandora.om.annotation.Aspect;
import pers.pandora.om.annotation.Before;
import pers.pandora.om.annotation.Throw;
import pers.pandora.om.core.JoinPoint;

/**
 * 1.All AOP Methods should have no return value
 * 2.All AOP Method should have only one parameter and it is JoinPoint class
 * 3.The symbol separating the class name from the method is #,for example, TaskBean#error
 * Note: The static methods of class properties belong to the member properties of the parent class and cannot be inherited or overridden. Therefore, AOP cannot proxy static methods of classes
 */
@Deprecated
@Aspect
public class TestAOP_1 {

    @Before("pers.*")
    public void beforeMethod(JoinPoint joinPoint){
        System.out.println("TestAOP_1 beforeMethod:" + joinPoint);
    }

    @After("pers.*")
    public void afterMethod(JoinPoint joinPoint){
        System.out.println("TestAOP_1 afterMethod:" + joinPoint);
    }

    @Throw("pers.*")
    public void throwMethod(JoinPoint joinPoint) throws Throwable {
        System.out.println("TestAOP_1 throwMethod:" + joinPoint);
        throw joinPoint.getException();
    }
}
