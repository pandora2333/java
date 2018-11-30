package pers.pandora.test;

import org.junit.Test;
import pers.pandora.impl.MyArrayListImpl;
import pers.pandora.impl.MyLinkedListImpl;
import pers.pandora.MyList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

//测试MyList
public class TestMyList {
    @Test
    public void test(){
        MyList<String> list = new MyLinkedListImpl<>();
        long start = Instant.now().toEpochMilli();
        for (int i= 0;i<10000;i++) {
            list.add("a"+i);
        }
        list.delete(3);
        list.delete(7);
        list.delete(389);
        for(int i= 0;i<500;i++){
            list.delete(i);
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("耗时："+(end-start));
        System.out.println(list.size());

    }
    @Test
    public void test2(){
        List list = new LinkedList();
        long start = Instant.now().toEpochMilli();
        for (int i= 0;i<10000;i++) {
            list.add("a"+i);
        }
        list.remove(1);
        list.remove(1);
        System.out.println(list.size());
        long end = Instant.now().toEpochMilli();
        System.out.println("耗时："+(end-start));
    }
}
