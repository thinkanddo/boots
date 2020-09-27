package com.bonc.bcos.sys;

import com.alibaba.fastjson.JSON;
import com.bonc.bcos.service.model.CmdK8SPo;
import com.bonc.bcos.service.model.CmdTablePo;
import com.bonc.bcos.service.service.CmdService;
import com.bonc.bcos.utils.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.util.*;

public final class SessionCache {
    private static final Logger LOG = LoggerFactory.getLogger(SessionCache.class);

    private static final String ENV_SESSION_KEY = "env_code";

    private static final String DICT_K8S_GROUP = "DICT_K8S_GROUP";
    private static final String DICT_K8S_NS = "DICT_K8S_NS";
    private static final String DICT_K8S_RESOURCE = "DICT_K8S_RESOURCE.";

    public static void  initSession(HttpSession session,String code){
        session.setAttribute(ENV_SESSION_KEY, code);

//        BootConfig config = SpringUtils.getBean(BootConfig.class);
//
//        LOG.info("{} 环境登陆中 资源编码： {}",code,config.getResources());
//
//        config.getResources().remove("false");
//
//        LOG.info("{} 环境登陆后 资源编码： {}",code,config.getResources());
//        session.setAttribute(DICT_K8S_RESOURCE, config.getResources());
        new Thread(()-> setApiResource(session), "异步同步资源").start();

        new Thread(()-> session.setAttribute(DICT_K8S_NS,getK8SResource(getCode(session))), "异步查询租户").start();
    }

    private static void setApiResource(HttpSession session){
        CmdService cmdService = SpringUtils.getBean(CmdService.class);
        try {
            CmdTablePo tablePo = cmdService.k8sResource(getCode(session));
            HashMap<String, Set<String>>  groups = new HashMap<>();

            for (HashMap<String,String> row: tablePo.getRows()){
                String group = row.get("APIGROUP");
                String shortNames = row.get("SHORTNAMES");

                if (!StringUtils.isEmpty(shortNames)){
                    if (!groups.containsKey(group)){
                        groups.put(group,new HashSet<>());
                    }
                    groups.get(group).add(shortNames);
                }
            }

            LOG.info("资源对象：{}", JSON.toJSONString(groups));

            session.setAttribute(DICT_K8S_GROUP,groups.keySet());
            for (String key: groups.keySet()){
                session.setAttribute(DICT_K8S_RESOURCE+key,groups.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getK8SResource(String code)  {
        CmdService cmdService = SpringUtils.getBean(CmdService.class);
        try {
            return cmdService.k8sDict(new CmdK8SPo("ns"),code).getRows();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void  removeSession(HttpSession session){
        session.removeAttribute(ENV_SESSION_KEY);
    }

    public static String getCode(HttpSession session){
        return (String) session.getAttribute(ENV_SESSION_KEY);
    }


}
