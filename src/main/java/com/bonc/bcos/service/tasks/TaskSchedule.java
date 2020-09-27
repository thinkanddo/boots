package com.bonc.bcos.service.tasks;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Configuration
@EnableScheduling
public class TaskSchedule {

    //添加定时任务， 检查超时任务
    @Scheduled(cron = "0 * * * * ?")
    private void checkTasks() {
        TaskManager.checkTimeout();
    }

}
