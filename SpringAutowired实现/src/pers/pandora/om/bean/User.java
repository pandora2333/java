package pers.pandora.om.bean;

import pers.pandora.om.annotation.PropertySource;
import pers.pandora.om.annotation.Value;

@Deprecated
@PropertySource("userPro.properties")
public class User {
    @Value("uid")
    private int id;
    @Value
    private String name;
    private String subject;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                ", className='" + className + '\'' +
                '}';
    }

    private String className;

}
