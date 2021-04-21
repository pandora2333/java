package pers.pandora.om.bean;

import pers.pandora.om.annotation.Autowired;
import pers.pandora.om.annotation.PropertySource;

@Deprecated
@PropertySource
public class B {
    private String a;
    private String b;
    @Autowired("taskBean")
    public TaskBean taskBean;//循环依赖

    public void setA(String a) {
        this.a = a;
    }

    public void setB(String b) {
        this.b = b;
    }
    public B(){
    }
    @Override
    public String toString() {
        return "B{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", taskBean=" + (taskBean!=null?taskBean.get():"null")+
                '}';
    }
}
