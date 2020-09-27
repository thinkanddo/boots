package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysClusterVersionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterVersionImageRepository extends JpaRepository<SysClusterVersionImage,String> {
    List<SysClusterVersionImage> findByVersion(String version);

}
