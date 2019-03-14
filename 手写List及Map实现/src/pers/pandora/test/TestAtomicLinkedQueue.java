package pers.pandora.test;

import pers.pandora.AtomicLinkedQueue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * simply test my defined unlock linked queue
 * by pandora 2018/12/16
 */
public class TestAtomicLinkedQueue {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        AtomicLinkedQueue<Long> queue = new AtomicLinkedQueue<>();
        AtomicInteger counter = new AtomicInteger();
//        IntStream.rangeClosed(1,5).boxed().map(t -> (Runnable)()->{
//            while (counter.getAndIncrement()<=100){//many threads only submit tasks = 100
//                try {
//                    Thread.sleep(25);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                queue.addLast(System.nanoTime());
//            }
//        }).forEach(executorService::submit);
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        IntStream.rangeClosed(1,5).boxed().map(t -> (Runnable)()->{
//            while (!queue.isEmpty()) {
//                try {
//                    Thread.sleep(20);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println(queue.removeFirst());
//            }
//            }).forEach(executorService::submit);
        //IntStream.rangeClosed(1,50).boxed().map(i->queue.removeFirst()).forEach(i -> System.out.println(i));
//        int i = 0;
//        while(!queue.isEmpty()){
//            i++;
//            System.out.println(queue.removeFirst());
//        }
//        System.out.println("exc:"+i);
//        System.out.println(queue.size());
//        executorService.shutdown();
//        try {
//            executorService.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("counter:"+counter.get());
//        AtomicLinkedQueue queue = new AtomicLinkedQueue();
//        IntStream.rangeClosed(1,5).boxed().map(i->System.nanoTime()).forEach(i -> queue.addLast(i));
//
//        List<Object> list = IntStream.rangeClosed(1, 5).boxed().map(i -> queue.peek()).collect(Collectors.toList());
//        System.out.println(queue.size());
//        System.out.println(list);
        /**
         * some read threads and some write threads parallel execute!
         */
        IntStream.range(0,500).boxed().map(l->(Runnable)()->{
            int count = 0;
            while((count++)<=10){
                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue.addLast(System.nanoTime());
            }
        }).forEach(executorService::submit);
        IntStream.range(0,500).boxed().map(l->(Runnable)()->{
            int count = 10;
            while((count--)>=0){
                try {
                    TimeUnit.MILLISECONDS.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(queue.removeFirst());
                counter.getAndIncrement();
            }
        }).forEach(executorService::submit);
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(queue.size());
        System.out.println(counter.get());
    }
}
