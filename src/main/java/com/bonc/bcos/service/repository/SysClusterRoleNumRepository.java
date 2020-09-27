package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterRoleNum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterRoleNumRepository extends JpaRepository<SysClusterRoleNum, String> {
    List<SysClusterRoleNum> findAllByOrderByRefNum();
}
