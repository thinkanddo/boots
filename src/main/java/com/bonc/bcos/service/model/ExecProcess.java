package com.bonc.bcos.service.model;

import com.bonc.bcos.service.entity.SysInstallPlayExec;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public  class ExecProcess{
    private String taskId;
    private long createDate;
    private char status;
    private String stdout;
    private Integer present;
    private Integer size;
    private String msg;
    private List<String> targetIps;

    public ExecProcess(SysInstallPlayExec exec) {
        // 设置执行状态
        this.taskId = exec.getUuid();
        this.createDate = exec.getCreateDate().getTime();
        this.status = exec.getStatus();
        this.targetIps = exec.getTargetIps();
        this.stdout = exec.getStdout();
        this.msg = exec.getMessage();
        this.size = exec.getCurIndex();
        if (exec.getPlaybooks().size()>0){
            this.present = (exec.getPercent()+size*100)*100/(100*exec.getPlaybooks().size());
        }
    }
}