package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterInfoRepository extends JpaRepository<SysClusterInfo,String>{


    List<SysClusterInfo> findAllByIdCode(String code);

    void deleteByIdCode(String code);

    SysClusterInfo findAllByIdCodeAndIdCfgKey(String code, String system_cur_version);
}
