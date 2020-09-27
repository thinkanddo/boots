package com.bonc.bcos.sys;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.entity.SysClusterInfo;
import com.bonc.bcos.service.entity.SysClusterKey;
import com.bonc.bcos.service.exception.ClusterException;

import java.util.*;

public class Global {

    public static final String SYSTEM_ENV_CODE = "SYSTEM_ENV_CODE";
    private static final String SYSTEM_WORK_DIR = "SYSTEM_WORK_DIR";
    private static final String SYSTEM_CUR_VERSION = "SYSTEM_CUR_VERSION";
    private static final String SYSTEM_INSTALL_FLAG = "SYSTEM_INSTALL_FLAG";
    public static final String SYSTEM_WORK_DIR_ADMIN = "SYSTEM_WORK_DIR_ANSIBLE";
    private static BootConfig config;

    // 集群信息配置集合
    private static final HashMap<SysClusterKey, SysClusterInfo> GLOBAL = new HashMap<>();

    private static final List<String> TAGS = Collections.synchronizedList(new ArrayList<>());

    private static final HashSet<String> LICENSE_IPS = new HashSet<>();

    public static final char READ_ONLY = '0';
    public static final char INNER_SET = '1';
    public static final char OUTER_SET = '2';

    public static void setConfig(BootConfig config) {
        Global.config = config;
    }

    public static BootConfig getConfig() {
        return Global.config;
    }

    public static void loadCfg(Collection<SysClusterInfo> cfgList) {
        for (SysClusterInfo cfg : cfgList) {
            loadCfg(cfg);
        }
        if (config.getEnvironment()){
            TAGS.add("latest");
        }else{
            TAGS.add(getCfgMap(SysClusterEnv.DEFAULT_ENV_CODE).get(SYSTEM_CUR_VERSION));
        }
    }

    public static void loadCfg(SysClusterInfo cfg) {
        GLOBAL.put(cfg.getId(), cfg);
    }

    public static HashMap<String, String> getCfgMap(String code) {
        HashMap<String, String> cfgMap = new HashMap<>();
        for (SysClusterInfo cfg : GLOBAL.values()) {
            // 不把环境编码暴露出去
            if (code.equals(cfg.getId().getCode())){
                cfgMap.put(cfg.getId().getCfgKey(), cfg.getCfgValue());
            }
        }
        return cfgMap;
    }

    public static SysClusterInfo getEntity(SysClusterKey key) {
        return Global.GLOBAL.get(key);
    }

    public static void updateGlobal(SysClusterInfo cfg) {
        GLOBAL.put(cfg.getId(), cfg);
    }

    private static String getValue(SysClusterKey key) {
        if (Global.GLOBAL.containsKey(key)){
            return Global.GLOBAL.get(key).getCfgValue();
        }
        throw new ClusterException(ReturnCode.CODE_GLOBAL_CFG_NOT_EXIST,"全局配置不存在"+key.toString());
    }

    /**
     * 获取 工作目录
     *
     * @return WORK_DIR_ANSIBLE
     */
    public static String getWorkDir(String code) {
        return getValue(new SysClusterKey(code,SYSTEM_WORK_DIR));
    }

    /**
     * 获取 ansible 安装包路径
     * 
     * @return WORK_DIR_ANSIBLE
     */
    public static String getAnsibleDir(String code) {
        return getValue(new SysClusterKey(code,SYSTEM_WORK_DIR_ADMIN));
    }

    public static void setTags(List<String> tags) {
        TAGS.clear();
        TAGS.add("latest");
        TAGS.addAll(tags);
    }

    public static List<String> getTags() {
        return TAGS;
    }

    public static void addLicenseIp(Set<String> ips){
        LICENSE_IPS.clear();
        LICENSE_IPS.addAll(ips);
    }

    public static boolean containIp(String ip){
        return LICENSE_IPS.contains(ip);
    }

    public static boolean isInstall(String code) {
        String install = getCfgMap(code).get(SYSTEM_INSTALL_FLAG);
        return "true".equalsIgnoreCase(install);
    }

}
