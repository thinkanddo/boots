package com.bonc.bcos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "`sys_cluster_role`")
@Data
public class SysClusterRole implements Serializable {

	private static final long serialVersionUID = -2599707778227199288L;
	public  static final String DEFAULT_ROLE = "default";
	public  static final String MASTER_ROLE = "master";
	public static final char HIDE_ROLE='0';

	@Id
	@Column(name = "`id`")
	private Integer id;

	@Column(name = "`role_code`", length = 32)
	private String roleCode;

	@Column(name = "`icon`" , length = 32)
	private String icon;

	@Column(name = "`role_desc`")
	private String roleDesc;

	/**
	 *   0： 每个机器上都有的主机
	 *   1： 界面展示的角色
	 *   2： 界面不展示的角色
	 */
	@Column(name = "`role_type`")
	private char roleType;

	@Column(name = "`status`")
	private Boolean status;

	/**
	 *  角色编码适用于所有的角色配置表，包括：
	 *  SysClusterRoleNum
	 *  SysClusterRolePolicy
	 *  SysClusterStoreCfg
	 *  当且仅当 status 未false 时候不可用，兼容历史配置没有 此字段
	 * @return 角色是否可用
	 */
	public boolean isEnable(){
		return status==null||status;
	}
}
