package pers.pandora.core;

import pers.pandora.core.annotation.EnableScheduling;
import pers.pandora.core.annotation.Scheduled;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描@Schedued注解
 */
public class AnnotationSchedulerDriver {
    private static Map<Class<?>, List<String>> jobMap = new ConcurrentHashMap<>();//以键值对方式存储method与execut method 的 Class Instance
    private static Map<String,String> crons =  new ConcurrentHashMap<>();//以键值对方式存储method与cron

    public static Map<Class<?>, List<String>> getJobMap() {
        return jobMap;
    }

    public static Map<String, String> getCrons() {
        return crons;
    }

    static {
        scanScheduler();
    }
    private static void  scanScheduler(){
        scanFile("src/");
    }
    private static void scanFile(String path){
        File files = new File(path);
        if(files!=null) {
            if(files.isDirectory()) {
                for(File file:files.listFiles()){
                    scanFile(file.getPath());
                }

            }else{
                if(files.getPath().endsWith(".java")){
                    path = 	files.getPath();
                    try {
                        handleClass(Class.forName(path.substring(4).replace(".java","").replace("\\",".")));
                    } catch (ClassNotFoundException e) {
                        System.out.println("类型转换异常!");
                    }
                }
            }
        }
    }

    private static void handleClass(Class<?> forName) {
        if(forName.isAnnotationPresent(EnableScheduling.class)){
            for(Method method:forName.getDeclaredMethods()){
                Scheduled annotation = method.getAnnotation(Scheduled.class);
                if(annotation!=null||forName.isAnnotationPresent(Scheduled.class)){
                    String methodName = method.getName();
                    try {
                        List<String> methodList = jobMap.get(forName);
                        if(methodList==null){
                            methodList = new Vector<>();
                        }
                        methodList.add(methodName);
                        jobMap.put(forName,methodList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    crons.put(methodName,annotation!=null?annotation.cron():forName.getAnnotation(Scheduled.class).cron());
                }
            }
        }
    }
}
