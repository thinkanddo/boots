package com.bonc.bcos.sys;

import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.repository.SysClusterEnvRepository;
import com.bonc.bcos.service.repository.SysClusterInfoRepository;
import com.bonc.bcos.service.repository.SysInstallLogLabelRepository;
import com.bonc.bcos.service.repository.SysInstallPlaybookRepository;
import com.bonc.bcos.service.service.CallService;
import com.bonc.bcos.service.service.ExecService;
import com.bonc.bcos.service.service.HostService;
import com.bonc.bcos.service.tasks.PlaybookExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
 * @desc:应用数据初始化
 * @author:
 * @time:
 */

@Component
public class AppInit implements CommandLineRunner{

    private final CallService callbackService;
    private final ExecService execService;
    private final SysClusterInfoRepository clusterInfoDao;
    private final SysInstallLogLabelRepository labelDao;
    private final SysInstallPlaybookRepository playbookDao;
    private final HostService hostService;
    private final SysClusterEnvRepository envDao;
    private final BootConfig config;

    @Autowired
    public AppInit(SysClusterInfoRepository clusterInfoDao, CallService callbackService, ExecService execService, SysInstallLogLabelRepository labelDao,
                   SysInstallPlaybookRepository playbookDao, HostService hostService, SysClusterEnvRepository envDao,BootConfig config) {
        this.clusterInfoDao = clusterInfoDao;

        this.callbackService = callbackService;
        this.execService = execService;

        this.labelDao = labelDao;

        this.playbookDao = playbookDao;

        this.hostService = hostService;
        this.envDao = envDao;

        this.config = config;
    }

    @Override
    public void run(String... args){
        System.out.println(">>>>>>>>>>>>>>>服务启动执行，执行加载数据等操作<<<<<<<<<<<<<");

        Global.setConfig(config);

        // 缓存全局参数
        Global.loadCfg(clusterInfoDao.findAll());

        // 释放任务资源
        execService.reset();

        // 初始化任务标签信息
        PlaybookExecutor.init(callbackService,labelDao.findAll(),playbookDao.findAll());

        // 主机设备归属设备源数据处理，兼容历史版本
        for (SysClusterEnv env: envDao.findAll()){
            hostService.handleData(env.getCode());
        }

        System.out.println(">>>>>>>>>>>>>>>服务启动执行，数据初始化完成<<<<<<<<<<<<<");
    }

}
