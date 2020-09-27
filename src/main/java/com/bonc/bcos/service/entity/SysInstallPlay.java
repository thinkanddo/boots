package com.bonc.bcos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "`sys_install_play`")
@Data
public class SysInstallPlay implements Serializable {
	private static final Integer TIMEOUT=30*60;

	private static final long serialVersionUID = -2102082194607883083L;

	// 主要用于前端界面展示
	private static final char STATUS_DISABLE='2';  // 状态2 为不可执行任务
	private static final char STATUS_ENABLE='1';  // 状态1  为轻量级任务，不可以看到执行日志
	private static final char STATUS_LOG='3';      // 状态3  为重要任务，需要关注执行日志
	private static final char STATUS_ADMIN='4';  // 状态4  为默认租户任务，展示效果全部前端控制，后再将这种任务的环境编码设置为 000000 默认租户

	// 控制任务集合的时序
	@Id
	@Column(name = "`id`")
	private Long id;

	@Column(name = "`play_code`",length = 32)
	private String playCode;

	/**
	 *  一般配合status 和 play_code 判断前缀关系
	 *  0 ： 集群安装
	 *  1：  一类工具节点
	 *  2： 二类工具节点
	 *  3:   三类工具节点
	 *  ...
	 */
	@Column(name = "`play_type`",length = 32)
	private int playType;

	@Column(name = "`play_name`",length = 32)
	private String playName;

	@Column(name = "`play_desc`")
	private String playDesc;

	/**
	 * 锁类型： 目标play锁主机的范围控制为  全量主机和增量主机两种
	 *
	 * true: 全量主机： 全量主机在验证锁的时候会去校验集群全部的主机的锁状态，并且会锁住目标主机
	 * false: 增量主机： 增量主机锁，会校验targets 参数中目标机器的锁状态， 仅仅锁住目标主机
	 */
	@Column(name = "`lock_type`")
	private Boolean lockType;

	/**
	 *  可用状态，
	 *  3 可以展示执行日志的调用
	 *  2 功能分类，做菜单用
	 *  1 可以被外部调用
	 *  0 不可以被外部调用，一旦禁用，此play 下面的配置将不会关联生效，主要关联配置有
	 *  SysInstallPlaybook
	 *  SysInstallHostControl
	 */
	@Column(name = "`status`")
	private char status;

	// 工具展示的fa图标
	@Column(name = "`icon`")
	private String icon;

	// 工具item 项展示的font 颜色 支持的class 有 text-muted/text-primary/text-success/text-info/text-warning/text-danger
	@Column(name = "`color`")
	private String color;

	// play 的超时时间
	@Column(name = "`timeout`")
	private Integer timeout;

	public Integer getTimeout(){
		return timeout==null?TIMEOUT:timeout;
	}

	public boolean isEnable() {
		return status!=SysInstallPlay.STATUS_DISABLE;
	}

	public boolean isAdmin() {
		return status==SysInstallPlay.STATUS_ADMIN;
	}
}
