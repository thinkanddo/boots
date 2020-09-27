package com.bonc.bcos.service.service.impl;

import com.bonc.bcos.service.entity.SysClusterHostRole;
import com.bonc.bcos.service.entity.SysClusterRole;
import com.bonc.bcos.service.model.*;
import com.bonc.bcos.service.repository.SysClusterHostRepository;
import com.bonc.bcos.service.repository.SysClusterHostRoleRepository;
import com.bonc.bcos.service.service.CmdService;
import com.bonc.bcos.service.tasks.AnalysisUtil;
import com.bonc.bcos.service.tasks.CmdExecutor;
import com.bonc.bcos.sys.BootConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CmdServiceImpl implements CmdService {

    private final SysClusterHostRoleRepository clusterHostRoleDao;
    private final SysClusterHostRepository clusterHostDao;
    private final BootConfig config;

    @Autowired
    public CmdServiceImpl(SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao,BootConfig config) {
        this.clusterHostDao = clusterHostDao;
        this.clusterHostRoleDao = clusterHostRoleDao;

        this.config = config;
    }
    
    @Override
    public CmdTablePo k8sTable(CmdK8SPo k8sCmd, String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("get", k8sCmd, "wide",config.getMerges().get("k8s"));

        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);
        CmdDictPo originList = cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);

        return AnalysisUtil.analysisListMap(originList.getRows());
    }

    @Override
    public CmdTablePo k8sResource(String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("--cached=true", new CmdK8SPo("api-resources"));

        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);
        CmdDictPo originList = cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);

        return AnalysisUtil.analysisListMap(originList.getRows());
    }

    @Override
    public CmdDictPo k8sYaml(CmdK8SPo k8sCmd, String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("get", k8sCmd, "yaml",null);
        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);

        return cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);
    }

    @Override
    public CmdDictPo k8sDetail(CmdK8SPo k8sCmd, String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("describe", k8sCmd, null,null);

        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);

        return cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);
    }

    @Override
    public CmdStdoutPo k8sDelete(CmdK8SPo k8sCmd, String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("delete", k8sCmd);

        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);
        CmdDictPo cmdDictPo = cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);

        return cmdDictPo.getStdoutPo();
    }

    @Override
    public CmdDictPo k8sDict(CmdK8SPo k8sCmd, String code) throws Exception {
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        String cmd = makeK8sCmd("get", k8sCmd, "name ");
        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);
        
        return cmdUtil.exec(new CmdPo(cmd + "| cut -f2 -d/",targets.get(0)), code);
    }

    @Override
    public CmdDictPo k8sLogs(CmdK8SPo po, String code) throws Exception{
        List<String> targets = getTargetsByCodeAndCodeRole(code, SysClusterRole.MASTER_ROLE);
        po.setResource("");
        String cmd = makeK8sCmd("logs --tail=1000 ", po, null,null);
        CmdExecutor cmdUtil = new CmdExecutor(clusterHostDao);

        return cmdUtil.exec(new CmdPo(cmd,targets.get(0)), code);
    }

    // 根据角色获取ip
    private List<String> getTargetsByCodeAndCodeRole(String code, String roleCode) {
        List<String> targets = new ArrayList<>();
        List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByCodeAndRoleCode(code, roleCode);
        for(SysClusterHostRole hostRole : hostRoles) {
            if(hostRole.getStatus() == SysClusterHostRole.INSTALLED){
                targets.add(hostRole.getIp());
            }
        }
        return targets;
    }

    private static String makeK8sCmd(String active, CmdK8SPo k8sCmd) {
        return makeK8sCmd(active,k8sCmd,null);
    }

    private static String makeK8sCmd(String active, CmdK8SPo k8sCmd, String output) {
        return makeK8sCmd(active,k8sCmd,output,null);
    }

    // 拼接k8s get命令  active: get describe delete  output: null name yaml
    private static String makeK8sCmd(String active, CmdK8SPo k8sCmd, String output, String [] merges) {
        String namespace = k8sCmd.getNamespace();
        String resource = k8sCmd.getResource();
//        String group = k8sCmd.getGroup(); //预留
        String name = k8sCmd.getName();

        StringBuilder builder = new StringBuilder();
        builder.append("kubectl ").append(active).append(" ").append(resource);

        if (!StringUtils.isEmpty(name)){
            builder.append(" ").append(name);
        }
        if (!StringUtils.isEmpty(namespace)) {
            builder.append(" -n ").append(namespace);
        }
        if (!StringUtils.isEmpty(output)) {
            builder.append(" -o ").append(output);
        }

        if (null!=merges && merges.length>0){
            builder.append(" ' | awk '{ ");
            for (String merge:merges){
                builder.append("gsub(\"").append(merge).append("\",\"").append(merge.replace(" ", "_")).append("\"); ");
            }
            builder.append(" print $0} ");
        }
        return builder.toString();
    }
}
