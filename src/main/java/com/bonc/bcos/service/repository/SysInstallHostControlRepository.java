package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysInstallHostControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysInstallHostControlRepository extends JpaRepository<SysInstallHostControl, String> {

    List<SysInstallHostControl> findByPlaybook(String id);
}
