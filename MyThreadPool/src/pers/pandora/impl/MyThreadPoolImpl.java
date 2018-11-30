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
    private volatile static BlockingQueue<Task> queue = new LinkedBlockingQueue<>();// 缓冲任务队列
    private static  volatile long idleTime;
    public MyThreadPoolImpl() {
        this("src/threadPool.properties");
    }

    public MyThreadPoolImpl(String file) {
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

        if ( maxsize < initalSize || initalSize <= 0||idleTime>=0) {
            throw new RuntimeException("配置文件数值错误!");
        } else {
            workThreads = new WorkThread[initalSize];
            initalThread();
        }
    }

    private synchronized void initalThread() {
        for (int i = 0; i < workThreads.length; i++) {
            workThreads[i] = new WorkThread();
            workThreads[i].start();
        }
    }

    @Override
    public void execute(Task task) {
        if (task != null) {
            synchronized (queue) {
                if (cursor>(initalSize<<1)&&initalSize<maxsize) {
                    rePool();
                }
                try {
                    queue.put(task);
                    cursor++;
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public synchronized void shutdown() {
        for(WorkThread work:workThreads){
            if(work!=null && work.isAlive()){
                work.close();
            }
        }
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
                reThreads[i] = new WorkThread();
                reThreads[i].start();
            }
            workThreads = reThreads;
        }
    }

    static class WorkThread extends Thread {
        private boolean running = true;

        @Override
        public void run() {
            while (running) {
                synchronized (queue) {
                    if (!queue.isEmpty()) {
                        Task task = null;
                        try {
                            queue.wait(idleTime);
                            task = queue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (task != null) {
                            task.run();
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
