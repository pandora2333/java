package pers.pandora.om.test;

import pers.pandora.om.bean.TaskBean;
import pers.pandora.om.core.BeanPool;
import pers.pandora.om.core.JavassistAOPProxyFactory;

import java.util.Objects;

@Deprecated
public class TestBean {

    public static void main(String[] args) {
        BeanPool beanPool = new BeanPool();
        //firstly init AOP Config
        beanPool.setAopPaths("pers.pandora.test");
        beanPool.setAopProxyFactory(new JavassistAOPProxyFactory());
        //secondly,init bean
        beanPool.init(BeanPool.ROOTPATH);
        TaskBean taskBean = beanPool.getBean("taskBean");
        assert taskBean != null;
//        taskBean.error();
        System.out.println("taskBean:" + Objects.requireNonNull(taskBean).toString());
        System.out.println("askBean:" + Objects.requireNonNull(beanPool.getBean("askBean")).toString());
        System.out.println(Objects.requireNonNull(beanPool.getBean("user")).toString());
        System.out.println(Objects.requireNonNull(beanPool.getBean("b")).toString());
    }
}
