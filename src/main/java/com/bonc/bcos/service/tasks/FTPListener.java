package com.bonc.bcos.service.tasks;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FTPListener {
    private static final Logger LOG = LoggerFactory.getLogger(FTPListener.class);


    private FTPUtil ftpUtil;

    private UpProcess process = new UpProcess();

    private long speed = 0;

    public UpProcess getProcess() {
        process.speed=speed;
        speed=0;
        return process;
    }

    @Data
    private class UpProcess{
        private long send = 0;

        private long size;

        private boolean finish = false;

        private boolean success = false;

        private long speed=0;
    }


    FTPListener(long size,FTPUtil ftpUtil) {
        process.size = size;
        this.ftpUtil = ftpUtil;
    }

    public void update(long send) {
        process.send+=send;
        speed+=send;
    }

    public void finish (boolean success){
        process.finish = true;
        process.success = success;
    }

    public void stop() {

        if(!process.finish){
            finish(false);
        }

        if (null!=ftpUtil){
            try {
                ftpUtil.logout();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ftpUtil.quit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
