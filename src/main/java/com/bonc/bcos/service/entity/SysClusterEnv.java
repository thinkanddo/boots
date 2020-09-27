package com.bonc.bcos.service.entity;

import com.bonc.bcos.utils.Base64Util;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "`sys_cluster_env`")
@Data
public class SysClusterEnv {

    public static final String DEFAULT_ENV_CODE = "000000";

    /**
     *  环境编码，唯一值
     */
    @Id
    @Column(name = "`code`" ,length = 32)
    private String code;

    /**
     *  环境访问密码
     */
    @Column(name = "`password`" ,length = 64)
    private String password;

    /**
     *  环境名称/项目名称
     */
    @Column(name = "`name`" ,length = 64)
    private String name;

    /**
     *  维护人员
     */
    @Column(name = "`maintainer`" ,length = 16)
    private String maintainer;

    /**
     *  联系方式
     */
    @Column(name = "`phone`" ,length = 11)
    private String phone;

    /**
     *  项目公司
     */
    @Column(name = "`company`" ,length = 64)
    private String company;

    /**
     *  项目地址
     */
    @Column(name = "`address`")
    private String address;

    /**
     *  安装包版本的hash 值
     */
    @Column(name = "`hash`" ,length = 64)
    private String hash;

    /**
     *  环境当前的版本信息
     */
    @Column(name = "`version`" ,length = 16)
    private String version;

    /**
     *  环境创建时间
     */
    @Column(name = "`create_date`")
    private Timestamp createDate;

    /**
     *  环境更新时间
     */
    @Column(name = "`update_date`")
    private Timestamp updateDate;

    /**
     *  环境备注信息
     */
    @Column(name = "`memo`")
    private String memo;

    /**
     *  包类型每个环境有一个独立的license
     */
    @Column(name = "`license`",columnDefinition="LONGTEXT")
    private String license;

    public void encryptPassword() {
        this.password = Base64Util.encrypt(this.password);
    }

    public String decryptPassword(){
        return Base64Util.decrypt(this.password);
    }
}
