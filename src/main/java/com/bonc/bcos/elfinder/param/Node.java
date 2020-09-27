package com.bonc.bcos.elfinder.param;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.exception.ClusterException;
import lombok.Data;

@Data
public class Node implements Cloneable{
    private String source;
    private String alias;
    private String path;
    private Boolean defaultFlag;
    private String locale;
    private Constraint constraint;

    @Override
    public Node clone(){
        try {
            return   (Node) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ClusterException(ReturnCode.CODE_DATA_CLONE_ERROR, "克隆主机数据异常！");
        }
    }
}
