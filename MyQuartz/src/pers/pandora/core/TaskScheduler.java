package pers.pandora.core;

import pers.pandora.ThreadPool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


/**
 * 定时任务调度器,并行
 */
public class TaskScheduler {
    private int delay;//初始化后延时执行时间
    private static Map<Class<?>, List<String>> jobMap;
    private static Map<String,String> crons;
    private ExecutorService executorService;
    private ThreadPool threadPool;
    static {
        crons = AnnotationSchedulerDriver.getCrons();
        jobMap = AnnotationSchedulerDriver.getJobMap();
    }
    public synchronized void exectueJob() {
        if(executorService != null||threadPool!=null) {
            for (Map.Entry<Class<?>, List<String>> entry : jobMap.entrySet()) {
                try {
                    Object obj = entry.getKey().newInstance();
                    for (String method : entry.getValue()) {
                        if (method != null) {
                            Job job = new Job(getDelay());
                            CronParser cronParser = new CronParser();
                            job.setMethod(entry.getKey().getDeclaredMethod(method));
                            job.setObject(obj);
                            cronParser.parseCron(crons.get(method), job);
                            if (executorService != null) {
                                executorService.execute(job);
                            } else {
                                threadPool.execute(job);
                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else {
            System.out.println("线程池启动失败!");
        }
    }

    public TaskScheduler(ExecutorService executorService){
        this.executorService = executorService;
    }
    public  TaskScheduler(){

    }
    public  TaskScheduler(ThreadPool threadPool){
        this.threadPool = threadPool;
    }

    /**
     * \以下可用于Spring等容器整合
     * @return
     */
    public Map<Class<?>, List<String>> getJobMap() {
        return jobMap;
    }

    public void setJobMap(Map<Class<?>, List<String>> jobMap) {
        this.jobMap = jobMap;
    }

    public Map<String, String> getCrons() {
        return crons;
    }

    public void setCrons(Map<String, String> crons) {
        this.crons = crons;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
