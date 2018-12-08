package pers.pandora;

import pers.pandora.task.Task;

/**
 * author:by pandora
 * version 1.4
 * up date:2018//11/20
 * encoding:utf8
 *
 */
public interface ThreadPool {
    public void execute(Task task);
    public void shutdown();
    public int getTaskCount();
    public int getCompletedTask();
    public  boolean getKeepAlive();
    public void setTimeout(long millis);
    public long getTimeout();
}
