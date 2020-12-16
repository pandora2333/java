//package pers.pandora.test;
//
//import pers.pandora.annotation.After;
//import pers.pandora.annotation.Aspect;
//import pers.pandora.annotation.Before;
//import pers.pandora.annotation.Throw;
//import pers.pandora.core.JoinPoint;
//
///**
// * Test AOP
// */
//@Deprecated
//@Aspect
//public class AopConfig {
//
//    @Before("pers\\.pandora\\.servlet.*")
//    public void before(JoinPoint joinPoint) {
//        System.out.println("before :" + joinPoint);
//    }
//
//    @After("pers\\.pandora\\.servlet.*")
//    public void after(JoinPoint joinPoint) {
//        System.out.println("after :" + joinPoint);
//    }
//
//    @Throw("pers\\.pandora\\.servlet.*")
//    public void exception(JoinPoint joinPoint) {
//        System.out.println("exception :" + joinPoint);
//    }
//}
