package pers.pandora.test;

import pers.pandora.MySkipList;

import java.util.stream.IntStream;

public class TestSkipList {

	public static void main(String[] args) {
		MySkipList<Integer> mySkipList = new MySkipList<>();
//		IntStream.range(0, 5).boxed().map((data)-> {mySkipList.add(data);});
		for(int i=0;i<100;i++){
			mySkipList.add(Integer.valueOf(i));
		}
		IntStream.range(0,50).boxed().forEach(i -> mySkipList.remove(i));
//        mySkipList.remove(30);
//        mySkipList.remove(31);
////        mySkipList.remove(32);
//        mySkipList.remove(0);
//        mySkipList.remove(1);
//        mySkipList.remove(64);
        IntStream.range(0,51).boxed().map(i -> mySkipList.contains(i)).forEach(System.out::println);
//        System.out.println("contains:"+mySkipList.contains(55)+",size:"+mySkipList.size());
//		mySkipList.printAllElments();
	}
}
