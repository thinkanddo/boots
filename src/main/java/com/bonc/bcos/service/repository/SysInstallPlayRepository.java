package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysInstallPlay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysInstallPlayRepository extends JpaRepository<SysInstallPlay, String> {

    List<SysInstallPlay> findByPlayCode(String playCode);
}
