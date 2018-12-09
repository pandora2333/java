package pers.pandora.task;

import java.util.concurrent.CountDownLatch;

public class TaskActive extends  Task {
    private CountDownLatch countDownLatch;
    public  TaskActive(CountDownLatch countDownLatch){
        this.countDownLatch = countDownLatch;
    }
    @Override
    public void task() {
        int i = 0;
        int num = 0;
        while(i<1000){
            num+=i;
            i++;
        }
        System.out.println(Thread.currentThread().getName()+"["+num+"]");
        countDownLatch.countDown();
    }
}
