package pers.pandora.bean;

import java.util.Objects;

public class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==null){
            return false;
        }
        if(obj==this){
            return true;
        }
        if(obj.getClass()==getClass()&&obj.hashCode()==hashCode()){
            return true;
        }
        if(obj instanceof  User) {
            User user= (User)obj;
            if (getAge() == user.getAge()&& getName().equals(user.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int id = Objects.hash(name, age);
        return name!=null? name.hashCode()^id:(age!=0? age^id:id);
    }

    public void setAge(int age) {
        this.age = age;
    }
}
