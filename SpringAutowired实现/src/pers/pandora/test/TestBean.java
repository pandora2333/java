package pers.pandora.test;

import pers.pandora.bean.B;
import pers.pandora.bean.TaskBean;
import pers.pandora.bean.User;
import pers.pandora.core.BeanPool;

@Deprecated
public class TestBean {
//    @Test
//    public void test() {
//        int t = Runtime.getRuntime().availableProcessors()*2;
//        BeanPool.getInstance().init(t,t+1,50, TimeUnit.MILLISECONDS);
//        System.out.println(BeanPool.getBean("taskBean", TaskBean.class));
//        System.out.println(BeanPool.getBean("user", User.class));
//        System.out.println(BeanPool.getBean("bi", B.class));
//    }

    public static void main(String[] args) {
        int t = Runtime.getRuntime().availableProcessors()*2;
        BeanPool.init();
        System.out.println(BeanPool.getBean("taskBean", TaskBean.class));
        System.out.println(BeanPool.getBean("user", User.class));
        System.out.println(BeanPool.getBean("bi", B.class));
    }
}
