package com.bonc.bcos.service.entity;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.service.exception.ClusterException;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashMap;

@Entity
@Table(name = "`sys_cluster_info`")
@Data
public class SysClusterInfo implements Serializable,Cloneable{

	private static final long serialVersionUID = 1819099555348375258L;

	private static final String HIDDEN_PREFIX = "HIDDEN_";

	// operator 控制器  下划线第二个字段代表对应的角色
	public static final String COMPOSE_OPERATOR_INSTALL_FLAG="COMPOSE_OPERATOR_INSTALL_FLAG";

	private static final HashMap<Character,String> TYPE_DESC = new HashMap<Character, String>(){{
		put(Global.READ_ONLY , "配置不可以修改");
		put(Global.INNER_SET, "配置只能内部修改");
		put(Global.OUTER_SET, "配置只能外部修改");
	}};

	@EmbeddedId
	private SysClusterKey id;

	@Column(name = "`cfg_value`")
	private String cfgValue;

	@Column(name = "`cfg_type`")
	private char cfgType;

	@Column(name = "`regular`")
	private String regular;

	@Column(name = "`icon`")
	private String icon;

	@Column(name = "`memo`")
	private String memo;

	public void setCfgValue(String cfgValue) {
		this.cfgValue = cfgValue;
		Global.updateGlobal(this);
	}

	/**
	 *  深度clone 配置数据
	 * @return 配置
	 */

	@Override
	public SysClusterInfo clone(){
		try {
			SysClusterInfo cfg = (SysClusterInfo)super.clone();
			cfg.setId(getId().clone());
			return cfg;
		}catch (Exception e){
			throw new ClusterException(ReturnCode.CODE_DATA_CLONE_ERROR,"配置文件复制失败！");
		}
	}

	public boolean isHidden() {
		return getId().getCfgKey().startsWith(HIDDEN_PREFIX);
	}
}
