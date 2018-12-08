package pers.pandora.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


/**
 * 定时任务调度器
 */
public class TaskScheduler {
    private int delay;//初始化后延时执行时间
    private static Map<Class<?>, List<String>> jobMap;
    private static Map<String,String> crons;
    private ExecutorService executorService;
    static {
        crons = AnnotationSchedulerDriver.getCrons();
        jobMap = AnnotationSchedulerDriver.getJobMap();
    }
    public synchronized void exectueJob() {
        for (Map.Entry<Class<?>, List<String>> entry : jobMap.entrySet()) {
            try {
                Object obj = entry.getKey().newInstance();
                for (String method : entry.getValue()) {
                    if (method != null) {
                        Job job = new Job(getDelay());
                        CronParser cronParser = new CronParser();
                        job.setMethod(entry.getKey().getDeclaredMethod(method));
                        job.setObject(obj);
                        cronParser.parseCron(crons.get(method),job);
                        if (executorService != null) {
                            executorService.execute(job);
                        } else {
                           Thread t = new Thread(job);
                           t.setDaemon(false);
                           t.setPriority(Thread.NORM_PRIORITY);
                           t.start();
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public TaskScheduler(ExecutorService executorService){
        this.executorService = executorService;
    }
    public  TaskScheduler(){

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
