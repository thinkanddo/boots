package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterHost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterHostRepository extends JpaRepository<SysClusterHost,String> {
    List<SysClusterHost> findAllByIdCode(String code);

    SysClusterHost findByIdCodeAndIdIp(String code, String ip);

    void deleteByIdCodeAndIdIp(String code, String ip);
}
