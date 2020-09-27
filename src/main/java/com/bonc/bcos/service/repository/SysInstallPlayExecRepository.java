package com.bonc.bcos.service.repository;

import com.bonc.bcos.service.entity.SysInstallPlayExec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysInstallPlayExecRepository extends JpaRepository<SysInstallPlayExec, String> {



    /**
     * Description:根据status查询
     * @param 
     * 		status	执行状态
     * @return
     * List<SysInstallPlayExec>
     */
    List<SysInstallPlayExec>  findByStatus(char status);

    List<SysInstallPlayExec> findAllByPlayCodeAndCodeOrderByCreateDateDesc(String playCode,String code);

    List<SysInstallPlayExec> findAllByCode(String code);
}
