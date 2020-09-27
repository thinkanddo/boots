package com.bonc.bcos.service.tasks;

import com.bonc.bcos.service.entity.SysInstallPlayExec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class TaskManager {
    private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);
    private static Map<String, PlaybookExecutor> taskMap = Collections.synchronizedMap(new HashMap<>());

    static void checkTimeout(){
        List<String> timeoutTasks = new ArrayList<>();
        // 获取超时的任务
        for (String uuid: taskMap.keySet()){
            if (taskMap.get(uuid).isTimeout()){
                timeoutTasks.add(uuid);
            }
        }

        // 执行任务清理
        for(String uuid: timeoutTasks){
            destroy(uuid,SysInstallPlayExec.TIMEOUT,"任务执行超时");
        }
    }

    public static void create(SysInstallPlayExec exec) {
        // 创建任务执行器
        PlaybookExecutor ce = new PlaybookExecutor(exec);

        // 将任务添加到全局任务表里面
        taskMap.put(exec.getUuid(),ce);

        LOG.info("任务ID {} 将要添加到任务清单",exec.getUuid());
    }

    public static void start(String uuid) {
        if (taskMap.containsKey(uuid)){
            taskMap.get(uuid).start();
        }
    }

    /**
     *  删除所有任务
     */
    public static void reset(){
        for (String uuid: taskMap.keySet()){
            destroy(uuid, SysInstallPlayExec.RESET,"系统状态重置");
        }
    }

    /**
     *  删除一个任务，从内存里面删除任务
     * @param uuid 任务ID
     */
    public static void destroy(String uuid,char status,String message){
        if(taskMap.containsKey(uuid)){
            PlaybookExecutor cm = taskMap.get(uuid);
            cm.destroyTask(status,message);
            remove(uuid,message);
        }
    }


    public static SysInstallPlayExec get(String uuid) {
        if (taskMap.containsKey(uuid)){
            return taskMap.get(uuid).getTask();
        }
        return null;
    }

    public static void remove(String uuid,String message) {
        LOG.info("{} ;任务 {} 将要移除掉任务清单",message,uuid);
        taskMap.remove(uuid);
    }

    public static Set<String> tasks(){
        return taskMap.keySet();
    }
}
