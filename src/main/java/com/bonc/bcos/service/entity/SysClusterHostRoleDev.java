package com.bonc.bcos.service.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bcos.utils.DateUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;

@Entity
@Table(name = "`sys_cluster_host_role_dev`")
@Data
public class SysClusterHostRoleDev implements Serializable {

	private static final long serialVersionUID = 2299808939612775242L;

	public static final String UNKNOWN_PART_TYPE="unknown";

	public static final char UN_USED = '0';
	public static final char DISABLED = '1';
	public static final char ALLOC = '1';
	public static final char IN_USED = '2';
	public static final char UN_FORMAT = '3';

	private static HashMap<Character,String> statusDesc = new HashMap<Character,String>(){{
		put(UN_USED,"未使用");
		put(DISABLED,"已禁用");
		put(IN_USED,"已使用");
		put(UN_FORMAT,"不可格式化");
	}};

	private static HashMap<Character,String> roleStatusDesc = new HashMap<Character,String>(){{
		put(UN_USED,"未使用");
		put(ALLOC,"已分配");
		put(IN_USED,"已使用");
	}};

	public SysClusterHostRoleDev() {
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
		this.updateDate = this.createDate;
		this.status = SysClusterHostRoleDev.UN_USED;
		this.devSizeUsed = 0;
	}

	public SysClusterHostRoleDev(SysClusterHostRole hostRole, String devName, String partType, int devSize, String vgName) {
		this();
		this.hostRoleId = hostRole.getId();
		this.devName = devName;
		this.devParent = devName;
		this.devSize = devSize;
		this.name = vgName;
		this.ip = hostRole.getIp();
		this.code = hostRole.getCode();
		this.partType = partType;
	}

	@Id
	@GenericGenerator(name = "uuidGenerator", strategy = "uuid")
	@GeneratedValue(generator = "uuidGenerator")
	private String id;

	@Column(name = "`code`")
	private String code;

	@Column(name = "`ip`",length = 15)
	private String ip;

	@Column(name = "`host_role_id`", length = 32)
	private String hostRoleId;

	@Column(name = "`dev_name`",length = 16)
	private String devName;

	@Column(name = "`dev_parent`",length = 16)
	private String devParent;
	
	@Column(name = "`dev_size`")
	private int devSize;
	
	@Column(name = "`dev_size_used`")
	private int devSizeUsed;
	
	@Column(name = "`name`",length = 32)
	private String name;
	
	@Column(name = "`part_type`",length = 32)
    private String partType;
	
    @Column(name = "`create_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp createDate;

	@Column(name = "`update_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateDate;

	/**
	 * 设备盘的状态
	 * 0: 初始状态 未使用
	 * 1: 禁用设备  用于default角色的使用控制，其他角色没有这个状态，只有0状态才能禁用
	 * 2: 已使用
	 * 3: 不可格式化  设备只能在0状态才能双击变为当前状态
	 */
	@Column(name = "`status`")
	private char status;

	/**
	 *  使用方式： 从store_cfg 表中的store_type字段继承而来
	 *   1： 创建vg lv 挂载
	 *   2： 不创建vg
	 *   3:   创建vg
	 *   4:   创建vg lv
	 */
	@Transient
	private char storeType;

    @Transient
    private int allocSize = 0;

	@Transient
	private int unUsedSize = 0;

	public boolean isEnable(){
		return SysClusterHostRoleDev.DISABLED != this.status;
	}

	public boolean isUsed(){
		return SysClusterHostRoleDev.IN_USED == this.status;
	}

	/**
	 *  从当前设备中分配一块size 大小空间 ,返回能分到的空间
	 * @param size 需要分配空间大小
	 * @return  实际分配空间大小
	 */
	public int accessAlloc( int size){
		int allocSpace = getEnableSize() >= size ? size:getEnableSize();
		setAllocSize(getAllocSize()+allocSpace);
		return allocSpace;
	}

    public void enable() {
		this.status = UN_USED;
	}

	public void unformat() {
		this.status = UN_FORMAT;
	}

	public void disable() {
		this.status = DISABLED;
	}

	@Override
	public int  hashCode(){
		return this.getId().hashCode();
	}

	public String getStatusDesc() {
		if (StringUtils.isEmpty(this.name)){
			return statusDesc.get(this.status);
		}else{
			return roleStatusDesc.get(this.status);
		}
	}

	public String getStoreTypeDesc(){
		return SysClusterStoreCfg.storeTypeDesc(this.storeType);
	}

	public int getEnableSize() {
		return devSize-devSizeUsed-allocSize;
	}
}
