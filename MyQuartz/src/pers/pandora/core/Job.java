package pers.pandora.core;

import pers.pandora.task.Task;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * job定时任务
 */
public class Job extends Task implements Runnable {
    private int second;
    private int min;
    private int hour;
    private int day;
    private int month;
    private int year;
    private int week;
    private int delay;//初始化任务后，第一次执行前延时执行时间
    private boolean flag;//标志当前任务是否可以结束，以便释放资源
    private Object object;//定时方法的真正执行者
    public void setObject(Object object) {
        this.object = object;

    }

    public void setMethod(Method method) {
        this.method = method;
    }

    private Method method;//具体执行的目标方法

    public int[] getStep() {
        return step;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
    public void setStep(int[] step) {
        this.step = step;
    }

    private int[] step;//步伐值
    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public Job(int delay){
        this.delay = delay;
    }


    @Override
    public void task() {
            vailFlag();
            while (true) {
                try {
                    if (equalsTime()) {
                        method.invoke(object);
                        if (flag) {//针对只执行一次就释放
                            break;
                        }
                        Thread.sleep(16);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    private void vailFlag() {
        flag = true;
        for (int i:step){//判断有无步伐值，判断是否执行一次
            if(i!=0){
                flag = false;
                break;
            }
        }
        if(getSecond()==Integer.MAX_VALUE||getYear()==Integer.MAX_VALUE||getMonth()==Integer.MAX_VALUE
                ||getWeek()==Integer.MAX_VALUE||getDay() == Integer.MAX_VALUE||getHour() == Integer.MAX_VALUE||getMin() == Integer.MAX_VALUE){
            flag = false;
        }
    }

    /**
     * time equals
     * @return
     */
    private boolean equalsTime() {
        LocalDateTime current = LocalDateTime.now();
        //嵌套equals,由大到小
        if((getYear()==current.getYear()||getYear()==Integer.MAX_VALUE||(current.getYear()-getYear())==step[5])&& getYear()>=current.getYear()){//y
            setYear(getYear()+step[5]);
            if((getMonth() == current.getMonth().getValue())||getMonth()==Integer.MAX_VALUE||(current.getMonth().getValue()-getMonth())==step[4]){//M
                setMonth(getMonth()+step[4]);
                if(getWeek()==current.getDayOfWeek().getValue()||getWeek()==Integer.MIN_VALUE||(current.getDayOfWeek().getValue()-getWeek())==step[6]) {//w
                    setWeek(getWeek()+step[6]);
                    if ((getDay() == current.getDayOfMonth()) || getDay() == Integer.MAX_VALUE || (current.getDayOfMonth() - getDay()) == step[3]) {//d
                        setDay(getDay() + step[3]);
                        if ((getHour() == current.getHour()) || getHour() == Integer.MAX_VALUE || (current.getHour() - getHour()) == step[2]) {//h
                            setHour(getHour() + step[2]);
                            if ((getMin() == current.getMinute()) || getMin() == Integer.MAX_VALUE || (current.getMinute() - getMin()) == step[1]) {//m
                                setMin(getMin() + step[1]);
                                if (getSecond() == Integer.MAX_VALUE ||getSecond()==current.getSecond()) {//s;
                                    if(step[0]!=0){
                                        try {
                                            Thread.sleep(step[0]*1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return true;
                                }else{
                                    try {
                                        int sec = 0;
                                        if(getSecond()!=0) {
                                            while (((sec = LocalDateTime.now().getSecond()) != getSecond() && sec <= getSecond())) {
                                                Thread.sleep(1000);
                                            }
                                            setSecond(0);
                                        }
                                        if(sec > getSecond()){
                                            return false;
                                        }
                                        if(step[0]!=0){
                                            Thread.sleep(step[0]*1000);
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
