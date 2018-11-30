package pers.pandora.test;

import org.junit.Test;
import pers.pandora.MyMap;
import pers.pandora.bean.User;
import pers.pandora.impl.MyHashMap;

public class TestMap {
    @Test
    public void test(){
        MyMap<String, User> myMap = new MyHashMap<>();
        for(int i=0;i<50;i++){
            myMap.put("a"+i,new User("a"+i,i+10));
        }
        for(int i=0;i<49;i++) {
            myMap.delete("a" + i);
        }
//        myMap.delete("a21");
//        myMap.delete("a48");
        System.out.println(myMap);
        System.out.println(myMap.size());
    }
}
