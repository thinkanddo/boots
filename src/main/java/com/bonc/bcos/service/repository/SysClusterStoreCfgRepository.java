package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterStoreCfg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterStoreCfgRepository extends JpaRepository<SysClusterStoreCfg, String> {

    List<SysClusterStoreCfg> findAllByRoleCode(String roleCode);
}
