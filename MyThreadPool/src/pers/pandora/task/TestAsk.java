package pers.pandora.task;

/**
 * 测试无序并行
 */
public class TestAsk extends Task {
    @Override
    public void task() {
        while(true){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("thread :"+Thread.currentThread().getName());
        }
    }
}
