package pers.pandora.test;

import pers.pandora.MySkipList;

import java.util.stream.IntStream;

/**
 * test my own skip-list
 * by pandora 2018/12/16
 */
public class TestSkipList {

	public static void main(String[] args) {
		MySkipList<Integer> mySkipList = new MySkipList<>();
//		IntStream.range(0, 5).boxed().map((data)-> {mySkipList.add(data);});
		for(int i=0;i<100;i++){
			mySkipList.add(Integer.valueOf(i));
		}
		IntStream.range(0,100).boxed().forEach(i -> mySkipList.remove(i));
//        mySkipList.remove(30);
//        mySkipList.remove(31);
//        mySkipList.remove(32);
//        mySkipList.remove(0);
//        mySkipList.remove(1);
//        mySkipList.remove(64);
//        IntStream.range(0,51).boxed().map(i -> mySkipList.contains(i)).forEach(System.out::println);
        System.out.println("contains:"+mySkipList.contains(30)+",size:"+mySkipList.size());
		mySkipList.printAllElments();
//		for(int i=0;i<100;i++){
//			mySkipList.add(Integer.valueOf(i));
//		}
//		mySkipList.add(Integer.valueOf(47));
//		mySkipList.add(Integer.valueOf(45));
//		mySkipList.add(Integer.valueOf(40));
//		mySkipList.add(Integer.valueOf(0));
//		mySkipList.add(Integer.valueOf(47));
//		mySkipList.add(Integer.valueOf(45));
//		mySkipList.add(Integer.valueOf(40));
//		mySkipList.add(Integer.valueOf(0));
//		System.out.println("contains:"+mySkipList.contains(77)+",size:"+mySkipList.size());
		mySkipList.printAllElments();
	}
}
