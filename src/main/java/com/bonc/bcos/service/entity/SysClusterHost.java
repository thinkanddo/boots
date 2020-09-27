package com.bonc.bcos.service.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.utils.Base64Util;
import com.bonc.bcos.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Max;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ApiModel(value = "主机信息")
@Entity
@Table(name = "`sys_cluster_host`")
@Data
public class SysClusterHost implements Serializable,Cloneable {

	public static final char NO_CHECK = '0';
	public static final char ERROR = '1';
	public static final char SUCCESS = '2';

	// 初始化的状态，不可启动或者停止
	public static final char POWER_CLOSE = '0';
	// 所有角色都是启动状态
	public static final char POWER_ON = '1';
	// 存在角色是已安装但是停止的状态
	public static final char POWER_OFF = '2';

	private static final HashMap<Character,String> STATUS_DESC = new HashMap<Character, String>(){{
		put('0',"未校验");
		put('1',"校验失败");
		put('2',"校验成功");
	}};

	public SysClusterHost() {
		this.status = SysClusterHost.NO_CHECK;
		this.sshPort = 22;
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
		this.updateDate = createDate;
		this.hostLock = false;
		this.power = SysClusterHost.POWER_CLOSE;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 3053015296950705164L;

	@Id
	@EmbeddedId
	private SysClusterHostKey id;

	@Column(name = "`hostname`", length = 32)
	@ApiModelProperty(value = "hostname", example = "127-0-0-1", dataType = "String")
	private String hostname;

	@Column(name = "`ssh_port`")
	@ApiModelProperty(value = "sshPort", required = true, example = "22", dataType = "int")
    @Max(value = 65536)
	private Integer sshPort;

	@Column(name = "`memory`")
	@ApiModelProperty(value = "memory", example = "32", dataType = "int")
	private Integer memory;

	@Column(name = "`cpu`")
	@ApiModelProperty(value = "cpu", example = "8", dataType = "int")
	private Integer cpu;

	@Column(name = "`has_gpu`")
	@ApiModelProperty(value = "hasGpu", example = "true", dataType = "bool")
	private Boolean hasGpu;

	@Column(name = "`os`",length = 32)
	@ApiModelProperty(value = "os", example = "Centos", dataType = "String")
	private String os;

	@Column(name = "`os_version`" ,length = 16 )
	@ApiModelProperty(value = "os_version", example = "7", dataType = "String")
	private String osVersion;

	@Column(name = "`kernel`" ,length = 64)
	@ApiModelProperty(value = "kernel", example = "3.10.0-957.5.1.el7.x86_64", dataType = "String")
	private String kernel;

	@Column(name = "`username`",length = 32)
	@ApiModelProperty(value = "username", required = true, example = "root", dataType = "String")
	private String username;

	@Column(name = "`password`")
	@ApiModelProperty(value = "password", required = true, example = "root", dataType = "String")
	private String password;

    @Column(name = "`create_date`")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createDate;

	@Column(name = "`update_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateDate;

	@Column(name = "`message`")
	@ApiModelProperty(hidden = true)
	private String message;

	/**
	 * 主要维护主机的状态
	 *
	 * 0: 主机要加入进群，但是还没验证，初始状态 1: 校验成功 2: 校验失败
	 */
	@Column(name = "`status`")
    @ApiModelProperty(hidden = true)
	private Character status;

	/**
	 * 主要维护主机的状态
	 *
	 * 0: 主机要加入进群，但是还没验证，初始状态 1: 校验成功 2: 校验失败
	 */
	@Transient
	private Character power;

	/**
	 * 主机锁 false 未锁状态，true 已锁状态
	 */
	@Column(name = "`host_lock`")
    @ApiModelProperty(hidden = true)
	private Boolean hostLock;

	/**
	 *  判断是否已经加密
	 */
	@ApiModelProperty(hidden = true)
	private Boolean isEncode;

	@Transient
	@ApiModelProperty(hidden = true)
	private String roleId;

	@Transient
	@ApiModelProperty(hidden = true)
	private char roleStatus;

	@Transient
	private String mac;

	@Transient
    @ApiModelProperty(hidden = true)
	private List<SysClusterHostRoleDev> devs = new ArrayList<>();

	@Transient
    @ApiModelProperty(hidden = true)
	private HashMap<String,SysClusterHostRole> roles = new HashMap<>();

	public SysClusterHost(String code, String ip, String error) {
		this.id = new SysClusterHostKey(code,ip);
		this.message = error;
	}

	public HashMap<String, SysClusterHostRole> getRoles() {
		return roles;
	}

	public void addRole(SysClusterHostRole hostRole) {
		if (hostRole!=null&& !StringUtils.isEmpty(hostRole.getRoleCode())) {
			this.roles.put(hostRole.getRoleCode(),hostRole);
		}
	}

	@Override
	public SysClusterHost clone(){
		try {
			return   (SysClusterHost) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new ClusterException(ReturnCode.CODE_DATA_CLONE_ERROR, "克隆主机数据异常！");
		}
	}

    @ApiModelProperty(hidden = true)
	public String getStatusDesc(){
		if (SysClusterHost.STATUS_DESC.containsKey(this.status)) {
			return this.id.getIp()+SysClusterHost.STATUS_DESC.get(status);
		}
		return this.id.getIp()+"未知的主机状态";
	}

	void initHostInventory(StringBuffer inv){
		inv.append(this.id.getIp()).append(" ansible_ssh_port=").append(sshPort).append(" ansible_ssh_user=").append(username).
				append(" ansible_ssh_pass='").append(password).append("' ansible_sudo_pass='").append(password).
				append("' this_hostname=").append(this.id.getIp().replace(".","-")).append(" this_ip=").append(this.id.getIp()).append("\n");
	}

	public boolean check() {
		return this.status == SysClusterHost.SUCCESS;
	}

	public SysClusterHost encodePassword() {
		decodePassword();
		this.password = Base64Util.encrypt(this.password);
		this.isEncode = true;
		return this;
	}

	public void decodePassword() {
		if (isEncode!=null&&isEncode){
			this.password = Base64Util.decrypt(this.password);
		}
		this.isEncode = false;
	}
}
