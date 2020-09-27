package com.bonc.bcos.service.entity;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Embeddable
@Data
public class SysClusterHostKey implements Serializable,Cloneable {

    @Column(name = "`code`")
    private String code;

    @Column(name = "`ip`", length = 15 )
    @ApiModelProperty(value = "ip", required = true, example = "192.168.1.1", dataType = "String")
    @Pattern(regexp="(?=(\\b|\\D))(((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))(?=(\\b|\\D))")
    private String ip;

    public SysClusterHostKey(){
    }

    public SysClusterHostKey(String code, String ip) {
        this.code = code;
        this.ip = ip;
    }

}
