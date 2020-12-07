package pers.pandora.bean;

import pers.pandora.annotation.PropertySource;
import pers.pandora.annotation.Value;

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
