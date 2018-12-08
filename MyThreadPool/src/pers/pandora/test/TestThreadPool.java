package pers.pandora.test;

import org.junit.jupiter.api.Test;
import pers.pandora.ThreadPool;
import pers.pandora.impl.MyThreadPoolImpl;
import pers.pandora.task.TaskActive;
import pers.pandora.task.TestAsk;

import java.time.Instant;
import java.util.concurrent.*;

public class TestThreadPool {
    public static void main(String[] args) {
        ThreadPool myThreadPool = new MyThreadPoolImpl();
        long start = Instant.now().toEpochMilli();
        for(int i=0;i<10;i++){
            myThreadPool.execute(new TestAsk());
//            System.out.println(myThreadPool.getCompletedTask());
//            new Thread(new TaskActive(countDownLatch)).start();
        }

        System.out.println("pass:"+Thread.currentThread().getName());
        myThreadPool.shutdown();
        long end = Instant.now().toEpochMilli();
        System.out.println("spend time:"+(end - start));//256 //3106
        System.out.println(myThreadPool.getCompletedTask());
    }
    @Test
    public void test(){
//        int [] a = new int[123];
//        a[0]=1;
//        System.out.println(a.length);//123
        ThreadPool myThreadPool = new MyThreadPoolImpl();
        CountDownLatch countDownLatch = new CountDownLatch(1000);
        long start = Instant.now().toEpochMilli();
        for(int i=0;i<1000;i++){
            myThreadPool.execute(new TaskActive(countDownLatch));
//            System.out.println(myThreadPool.getCompletedTask());
//            new Thread(new TaskActive(countDownLatch)).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("pass");
        myThreadPool.shutdown();
        long end = Instant.now().toEpochMilli();
        System.out.println("spend time:"+(end - start));//256 //3106
        System.out.println(myThreadPool.getCompletedTask());
    }
    @Test
    public void test2(){
        CountDownLatch countDownLatch = new CountDownLatch(700);
        long start = Instant.now().toEpochMilli();
            ThreadPoolExecutor th = new ThreadPoolExecutor(100,200,1000, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.DiscardPolicy());
        for(int i = 0;i<700;i++){
//            new Thread(new TaskActive(countDownLatch)).start();
            th.execute(new TaskActive(countDownLatch));
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        th.shutdown();
        long end = Instant.now().toEpochMilli();
        System.out.println("spend time:"+(end - start));//141 //31 //78
    }
}
