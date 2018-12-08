package pers.pandora.impl;

import pers.pandora.ThreadPool;
import pers.pandora.task.Task;
import pers.pandora.util.PropertiesUtils;

import java.util.concurrent.*;

public class MyThreadPoolImpl implements ThreadPool {
    private  int maxsize;// 线程池最大线程数
    private  int initalSize;//线程池初始化线程数
    private  static volatile int  completedTask;//完成工作数
    private static volatile WorkThread[] workThreads;//维护线程池
    private volatile static int cursor;//线程池扩容游标,记录总的在线工作数
    private volatile static BlockingQueue<Task> queue;// 缓冲任务队列
    private static  volatile long idleTime;
    public MyThreadPoolImpl() {
        this("src/threadPool.properties",new LinkedBlockingQueue<>());
    }

    public MyThreadPoolImpl(String file,BlockingQueue<Task> queue) {
        try {
            if (!PropertiesUtils.parse("maxsize", file).equals("null")) {
                maxsize = Integer.valueOf(PropertiesUtils
                        .parse("maxsize", file));
            }
            if (!PropertiesUtils.parse("initalSize", file).equals("null")) {
                initalSize = Integer.valueOf(PropertiesUtils.parse(
                        "initalSize", file));
            }
            if (!PropertiesUtils.parse("idleTime", file).equals("null")) {
                idleTime = Long.valueOf(PropertiesUtils.parse(
                        "idleTime", file));
            }

        } catch (Exception e) {
            System.out.println("配置文件非数值!");
            // e.printStackTrace();
        }

        if ( maxsize < initalSize || initalSize <= 0||idleTime<=0) {
            throw new RuntimeException("配置文件数值错误!");
        } else {
            this.queue = queue;
            workThreads = new WorkThread[initalSize];
        }
    }

    @Override
    public void execute(Task task) {
        if (task != null) {
            synchronized (queue) {
                if ((cursor>(initalSize<<4)&&initalSize<maxsize) && !isAllBuy()) {
                    rePool();
                }
                try {
                    queue.put(task);
                    if (cursor<workThreads.length && workThreads[cursor] == null) {
                        workThreads[cursor] = initWorker();
                    }
                    cursor++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private WorkThread initWorker(){
        WorkThread workThread = new WorkThread();
        workThread.setDaemon(false);
        workThread.setPriority(Thread.NORM_PRIORITY);
        workThread.isWork = true;
        workThread.start();
        return workThread;
    }
    /**
     * 当所有线程都忙碌时，外加任务没触及最大线程数，可以扩容线程池
     * @return
     */
    private boolean isAllBuy() {

        for (WorkThread workThread:workThreads) {
            if(!workThread.isWork){
                return  false;
            }
        }
        return  true;
    }

    @Override
    public synchronized void shutdown() {
        for(WorkThread work:workThreads){
            if(work!=null && work.running){
                work.close();
            }
        }
        System.out.println("pool:"+workThreads.length);
        cursor = 0;
        workThreads = null;

    }

    @Override
    public int getTaskCount() {
        return cursor;
    }

    @Override
    public int getCompletedTask() {
        return completedTask;
    }

    @Override
    @Deprecated
    /**
     * 未实现
     */
    public boolean getKeepAlive() {
        return false;
    }

    @Override
    @Deprecated
    /**
     * 未实现
     */
    public void setTimeout(long millis) {

    }

    @Override
    @Deprecated
    /**
     * 未实现
     */
    public long getTimeout() {
        return 0;
    }

    private void rePool() {// resize pool initalsize<=maxSize
        if (initalSize < maxsize) {
            final WorkThread[] reThreads;
            if (maxsize < initalSize << 1 + 1) {
                reThreads = new WorkThread[maxsize];
                initalSize = maxsize;
            } else {
                reThreads = new WorkThread[initalSize << 1 + 1];
                initalSize = initalSize << 1 + 1;
            }
            int cursor = 0;
            for (WorkThread thread : workThreads) {
                if (thread != null && thread.isAlive()) {
                    reThreads[cursor] = thread;
                }
                cursor++;
            }
            for(int i = cursor;i<reThreads.length;i++){
                reThreads[i] = initWorker();
            }
            workThreads = reThreads;
        }
    }

    static class WorkThread extends Thread {
        private boolean running = true;
        private boolean isWork;//当前线程是否占用
        @Override
        public void run() {
            while (running) {
                if (!queue.isEmpty()) {
                    /**
                     * 无序并行执行
                     */
//                    Task task = null;
//                    try {
//                        Thread.sleep(idleTime);
//                        task = queue.poll(idleTime,TimeUnit.MILLISECONDS);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    if (task != null) {
//                        isWork = true;//标志开始工作
//                        task.run();
//                        isWork = false;
//                        completedTask++;
//                        cursor--;
//                    }
                    /**
                     * 有序并行执行
                     */
                synchronized (queue) {
                    Task task = null;
                    try {
                        queue.wait(idleTime);
                        task = queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (task != null) {
                        isWork = true;//标志开始工作
                        task.run();
                        isWork = false;
                        completedTask++;
                        cursor--;
                    }
                   }
                }
            }

        }

        public void close() {
            running = false;
        }
    }
}
