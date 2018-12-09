package pers.pandora.test;


import pers.pandora.core.TaskScheduler;
import pers.pandora.impl.MyThreadPoolImpl;

import java.util.concurrent.Executors;

public class TestQuartz {
    public static void main(String[] args) {
//        TaskScheduler taskScheduler = new TaskScheduler(new MyThreadPoolImpl());
        TaskScheduler taskScheduler = new TaskScheduler(Executors.newFixedThreadPool(5));//推荐使用jdk自带线程池
        taskScheduler.exectueJob();
    }

}
