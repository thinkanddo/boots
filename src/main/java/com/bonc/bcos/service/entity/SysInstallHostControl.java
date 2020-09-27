package com.bonc.bcos.service.entity;

import com.bonc.bcos.utils.StringUtils;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;

@Entity
@Table(name = "`sys_install_host_control`")
@Data
public class SysInstallHostControl implements Serializable {

	private static final long serialVersionUID = -2102082194607883083L;

	// 严格匹配模式
	public static final char EXACT_MATCH_CTL = '0';

	// 状态匹配模式
	public static final char STATUS_MATCH_CTL = '1';

	// 空匹配模式
	public static final char NONE_MATCH_CTL = '2';

	@Id
	@Column(name = "`id`")
	private Long id;
	
	@Column(name = "`playbook`",length = 32)
	private String playbook;

    /**
     *  用户生成playbook 文件中主机分组的组名
     *  每一个分组都是由一种角色根据状态控制出来的，每一个host_control 都唯一定义一个group，同一个playbook 下面不可以有重复的group 定义
     *
     *  playbook 和 group 是联合主键
     */
	@Column(name = "`group`",length = 32)
	private String group;

    /**
     * 筛选主机控制组的原始角色，最初设计两者是用的一个字段控制的，为了更高的灵活性，将两者拆开
     */
	@Column(name = "`role_code`",length = 32)
	private String roleCode;

	/**
	 *  管控约束出来的集合是否要和targets中取交集，target 集合一般是约束集合的子集
	 *  false: 以角色状态约束的主机为准
	 *  true:  依据角色的主机信息做主机关系合并的
	 *
	 *  筛选出可以操作的主机；  0：所有主机  1：目标主机（即在页面选中的主机）
	 */

	@Column(name = "`target_enable`")
	private Boolean targetEnable;

	/**
	 *  控制，主要把控当前主机环境下是否能够完成安装的功能，比如master 未安装就是不能拓展节点
	 *  生成ansible的group组里的主机列表
	 *  0:  精准控制（target主机列表所有ip都为指定状态，才将所有主机添加到group组里）
	 *  1:  状态控制（从target主机列表中筛选出指定状态的ip，添加到group组里）
	 *  2:  不控制  （不管status为多少，把target主机都添到group组里）
	 */
	@Column(name = "`control`",length = 32)
	private char control;

	/**
	 *  根据角色状态约束主机集合，这个字段和角色状态对应的，与control一起用于筛选主机
	 */
	@Column(name = "`status`")
	private char status;

	@Column(name = "`memo`",length = 32)
	private String memo;

	/**
	 *  hostControl 主机组装标识，默认是true，如果主机组装失败则设置未false
	 */
	@Transient
	private Boolean flag = true;

	/**
	 *  控制主机组，如果主机控制数据能够组装成功，则hosts存储对应的控制主机信息
	 */
	@Transient
    private HashMap<String,SysClusterHost> hosts = new HashMap<>();


	public void addHost(SysClusterHost host) {
		if (null!=host && !StringUtils.isEmpty(host.getId().getIp())) {	hosts.put(host.getId().getIp(),host);}
	}

	public void clearHosts(){
		this.hosts.clear();
	}

	public String getConstructMsg() {
	    return "安装步骤"+this.playbook+"中角色为："+roleCode+" 的状态不满足执行条件！"+hosts.keySet();
	}
}
