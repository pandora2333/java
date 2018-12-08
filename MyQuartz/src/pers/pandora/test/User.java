package pers.pandora.test;

import pers.pandora.core.annotation.EnableScheduling;
import pers.pandora.core.annotation.Scheduled;

import java.time.LocalDateTime;

@EnableScheduling
@Scheduled
public class User {
    private LocalDateTime dateTime;
    @Scheduled(cron = "0/3 * * * * * ?")
    public void driver(){
        dateTime = dateTime.now();
        System.out.println("小车开啊开~~~~");
        System.out.println("时间:"+dateTime.getSecond());
    }

    public void test3(){
        dateTime = dateTime.now();
        System.out.println("小a back~~~~");
        System.out.println("时间:"+dateTime.getSecond());
    }
    @Scheduled(cron = "0/7 * * * * * ?")
    public  void test2(){
        dateTime = dateTime.now();
        System.out.println("小b coming~~~~");
        System.out.println("时间:"+dateTime.getSecond());
    }
}
