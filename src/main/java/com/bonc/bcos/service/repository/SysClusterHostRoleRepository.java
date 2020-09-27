package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterHostRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterHostRoleRepository extends JpaRepository<SysClusterHostRole,String> {

    List<SysClusterHostRole>  findByCodeAndIp(String code,String ip);

    List<SysClusterHostRole> findByCodeAndRoleCode(String code,String roleCode);

    void deleteByCodeAndIp(String code,String ip);

    List<SysClusterHostRole> findAllByCodeAndIp(String code,String ip);

    SysClusterHostRole findByCodeAndIpAndRoleCode(String ip, String code, String role);
}
