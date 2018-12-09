package pers.pandora.task;

import java.util.concurrent.CountDownLatch;

/**
 * 测试无序并行
 */
public class TestAsk extends Task {
    CountDownLatch countDownLatch;

    @Override
    public void task() {
        while (true) {
            try {
                Thread.currentThread().interrupt();
//                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("thread :" + Thread.currentThread().getName());
            countDownLatch.countDown();
        }
    }

    public TestAsk(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
}