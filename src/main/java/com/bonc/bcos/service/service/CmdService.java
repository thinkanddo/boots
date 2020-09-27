package com.bonc.bcos.service.service;

import com.bonc.bcos.service.model.*;

public interface CmdService {

    CmdTablePo k8sTable (CmdK8SPo k8s, String code) throws Exception;

    CmdTablePo k8sResource (String code) throws Exception;

    CmdDictPo k8sYaml (CmdK8SPo k8s, String code) throws Exception;

    CmdDictPo k8sDetail (CmdK8SPo k8sCmd, String code) throws Exception;

    CmdStdoutPo k8sDelete (CmdK8SPo k8sCmd, String code) throws Exception;

    CmdDictPo k8sDict (CmdK8SPo k8sCmd, String code) throws Exception;

    CmdDictPo k8sLogs(CmdK8SPo po, String code) throws Exception;
}
