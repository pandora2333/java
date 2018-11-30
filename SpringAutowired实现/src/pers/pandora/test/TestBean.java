package pers.pandora.test;

import org.junit.Test;
import pers.pandora.bean.TaskBean;
import pers.pandora.bean.User;
import pers.pandora.util.BeanUtils;


public class TestBean {
    @Test
    public void test() {

        System.out.println(BeanUtils.getBean("taskBean", TaskBean.class));
        System.out.println(BeanUtils.getBean("user", User.class));
    }
}
