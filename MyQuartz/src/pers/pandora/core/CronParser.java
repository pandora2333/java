package pers.pandora.core;

/**
 * 解析cron表达式
 */
public class CronParser {
    private int[] step = new int[7];//记录哪一位出现步伐值
    public Job parseCron(String cron,Job job){//"0/1 * * * * * ?"
        cron = cron.trim();
        String[] crons = cron.split(" ");
        if(crons.length == 7) {
            handleTime(crons);
            setJob(crons,job);
            job.setStep(step);
            return job;
        }else {
            throw new RuntimeException("@Scheduled cron表达式有误，不合法");
        }
    }

    /**
     * 解析所有出现的时间阈值错误全部交给jdk自带的时间类处理
     * @param crons
     * @param job
     */
    private void setJob(String[] crons, Job job) {
        job.setSecond(Integer.parseInt(crons[0]));//s
        job.setMin(Integer.parseInt(crons[1]));//m
        job.setHour(Integer.parseInt(crons[2]));//h
        job.setDay(Integer.parseInt(crons[3]));//d
        job.setMonth(Integer.parseInt(crons[4]));//M
        job.setYear(Integer.parseInt(crons[5]));//y
        job.setWeek(Integer.parseInt(crons[6]));//w
    }

    private void handleTime(String[] crons) {
        for(int i = 0;i<crons.length;i++){
            String temp = crons[i];
            if(!temp.equals(" ")||!temp.equals("")){
                if(temp.equals("*")){
                    crons[i] = String.valueOf(Integer.MAX_VALUE);
                }else if(temp.equals("?")){
                    crons[i] = String.valueOf(Integer.MIN_VALUE);
                }
                if(temp.contains("/")){
                    String[] steps = temp.split("/");
                    step[i] = Integer.parseInt(steps[1]);
                    crons[i] = steps[0];
                }else{
                    step[i] = 0;
                }
            }
        }
    }
}
