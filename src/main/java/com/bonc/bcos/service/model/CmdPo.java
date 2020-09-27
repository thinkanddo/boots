package com.bonc.bcos.service.model;

import lombok.Data;

@Data
public class CmdPo {

    private String ip;

    
    //例如： source /etc/profile; kubectl get ns
    private String cmd;

    public CmdPo() {
    }

    public CmdPo(String cmd,String ip) {
        this.ip = ip;
        this.cmd = cmd;
    }
}
