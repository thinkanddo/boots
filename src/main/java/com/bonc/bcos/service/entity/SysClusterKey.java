package com.bonc.bcos.service.entity;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.exception.ClusterException;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class SysClusterKey implements Serializable,Cloneable {

    @Column(name = "`code`")
    private String code;

    @Column(name = "`cfg_key`" ,length = 64)
    private String cfgKey;

    public SysClusterKey(){
    }

    public SysClusterKey(String code){
        this(code,null);
    }

    public SysClusterKey(String code, String key) {
        this.code = code;
        this.cfgKey = key;
    }

    @Override
    public SysClusterKey clone(){
        try {
            return (SysClusterKey)super.clone();
        }catch (Exception e){
            throw new ClusterException(ReturnCode.CODE_DATA_CLONE_ERROR,"配置主键复制失败！");
        }
    }
}
