package com.bonc.bcos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`sys_cluster_version_image`")
@Data
public class SysClusterVersionImage {

    public SysClusterVersionImage() {
    }

    public SysClusterVersionImage(String version, String image) {
        this.version = version;
        this.image = image;
    }

    /**
     *  镜像版本
     */
    @Id
    @Column(name = "`image`")
    private String image;

    /**
     *  bcos 版本
     */
    @Column(name = "`version`" ,length = 32)
    private String version;


}
