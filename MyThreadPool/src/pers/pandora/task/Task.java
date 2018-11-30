package pers.pandora.task;

public abstract class Task implements  Runnable {
    @Override
    public void run() {
        task();
    }

    public  abstract  void task();
}
