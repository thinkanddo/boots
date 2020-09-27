package com.bonc.bcos.service.repository;

import java.util.List;

import com.bonc.bcos.service.entity.SysClusterHostRoleDev;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysClusterHostRoleDevRepository extends JpaRepository<SysClusterHostRoleDev, String> {

	/**
	 * 
	 * @return
	 * 		List<SysClusterRoleDev>
	 */
	List<SysClusterHostRoleDev> findByHostRoleIdOrderByDevName(String roleId);

    
    /**
     * 根据主机角色ID 和状态         删除下面的所有设备信息
     * @param hostRoleId   主机角色主键         status状态
     */
    void deleteByHostRoleIdAndStatus(String hostRoleId,char status);

	/**
	 * 根据主机角色ID        删除下面的所有设备信息
	 * @param hostRoleId   主机角色主键         status状态
	 */
	void deleteByHostRoleId(String hostRoleId);

	/**
	 *
	 */
    void deleteByCodeAndIp(String code,String ip);

    List<SysClusterHostRoleDev> findAllByCodeAndIp(String code, String ip);
}
