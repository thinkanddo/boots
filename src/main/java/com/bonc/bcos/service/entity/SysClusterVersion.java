package com.bonc.bcos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "`sys_cluster_version`")
@Data
public class SysClusterVersion {
    /**
     *  安装包名称
     */
    @Id
    @Column(name = "`package_name`" ,length = 128)
    private String packageName;

    /**
     *  安装包名称
     *  package/
     */
    @Column(name = "`package_path`" )
    private String packagePath;

    /**
     *  安装包大小
     *  package/
     */
    @Column(name = "`package_size`" ,length = 32)
    private String packageSize;

    /**
     *  当前版本
     */
    @Column(name = "`cur_version`" ,length = 32)
    private String curVersion;

    /**
     *  升级版本
     */
    @Column(name = "`target_version`" ,length = 32)
    private String targetVersion;

    /**
     *  ftp 链接串
     *  ftp://user:pass@ip:port/
     */
    @Column(name = "`ftp_link`" )
    private String ftpLink;

    /**
     *  环境创建时间
     */
    @Column(name = "`create_date`")
    private Timestamp createDate;

    /**
     *  包类型、
     *  0:   安装包
     *  1： 升级包
     */
    @Column(name = "`type`")
    private char type;
}
