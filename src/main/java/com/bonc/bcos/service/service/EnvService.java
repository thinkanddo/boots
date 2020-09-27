package com.bonc.bcos.service.service;

import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.model.License;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface EnvService {

    /**
     *  创建集群，如果不存在
     * @param env 环境信息
     */
    SysClusterEnv saveEnv(SysClusterEnv env) ;

    void deleteEnv(SysClusterEnv env);

    List<SysClusterEnv> findEnv();

    Boolean entryEnv(SysClusterEnv env);

    boolean checkLicense(SysClusterEnv env);

    boolean checkEnv();

    License getEnvLicense(String code);

    String getLicense(String attribute);

    void getFaqs(String fileName, HttpServletResponse resp) throws IOException;
}
