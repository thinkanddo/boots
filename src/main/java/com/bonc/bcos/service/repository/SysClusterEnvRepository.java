package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterEnv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterEnvRepository extends JpaRepository<SysClusterEnv,String> {

    List<SysClusterEnv> findAllByCode(String code);
}
