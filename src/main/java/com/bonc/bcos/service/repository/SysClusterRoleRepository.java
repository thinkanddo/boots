package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterRoleRepository extends JpaRepository<SysClusterRole, String> {

    List<SysClusterRole> findByRoleCode(String key);
}
