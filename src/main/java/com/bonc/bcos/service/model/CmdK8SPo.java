package com.bonc.bcos.service.model;

import lombok.Data;

@Data
public class CmdK8SPo {

    public CmdK8SPo() {
    }

    public CmdK8SPo(String resource) {
        this.resource = resource;
    }

    // 租户
    private String namespace;

    // 资源类型
    private String resource;

    // 预留 api 分组
    private String group;

    // 查询筛选条件
    private String search;

    // 具体的资源名
    private String name;
}
