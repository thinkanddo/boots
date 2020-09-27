package com.bonc.bcos.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.entity.SysClusterHost;
import com.bonc.bcos.service.entity.SysClusterInfo;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.model.License;
import com.bonc.bcos.service.repository.SysClusterEnvRepository;
import com.bonc.bcos.service.repository.SysClusterHostRepository;
import com.bonc.bcos.service.repository.SysClusterInfoRepository;
import com.bonc.bcos.service.service.EnvService;
import com.bonc.bcos.sys.BootConfig;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.utils.DESUtil;
import com.bonc.bcos.utils.DateUtil;
import com.bonc.bcos.utils.FileUtils;
import com.bonc.bcos.utils.LicenseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class EnvServiceImpl implements EnvService {



    private static final Logger LOG = LoggerFactory.getLogger(EnvServiceImpl.class);

    private static final Pattern ipPattern = Pattern.compile( "^((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])$" );
//    private static final Pattern macPattern =Pattern.compile("^[A-F0-9]{2}(-[A-F0-9]{2}){5}$");

    private final SysClusterEnvRepository envDao;
    private final SysClusterInfoRepository clusterDao;
    private final SysClusterHostRepository hostDao;
    private final BootConfig config;

    public EnvServiceImpl(SysClusterEnvRepository envDao, SysClusterInfoRepository clusterDao, SysClusterHostRepository hostDao,BootConfig config) {
        this.envDao = envDao;
        this.clusterDao = clusterDao;
        this.hostDao = hostDao;
        this.config = config;
    }

    @Override
    @Transactional
    public SysClusterEnv saveEnv(SysClusterEnv env) {
        Optional<SysClusterEnv> opt = envDao.findById(env.getCode());
        // 创建环境
        if (env.getCreateDate() == null) {
            if (opt.isPresent()) {
                throw new ClusterException(ReturnCode.CODE_ENV_EXISTED, "环境已经存在");
            } else {
                env.encryptPassword();
                // 创建环境的时候，根据global 中target 中的版本信息设置环境的版本信息
                env.setVersion(Global.getCfgMap(SysClusterEnv.DEFAULT_ENV_CODE).get("SYSTEM_TARGET_VERSION"));
                env.setCreateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));

                loadLicense(env);

                envDao.save(env);

                List<SysClusterInfo> cfgList = clusterDao.findAllByIdCode(SysClusterEnv.DEFAULT_ENV_CODE);
                List<SysClusterInfo> newCfgList = new ArrayList<>();
                for (SysClusterInfo cfg:cfgList){
                    SysClusterInfo newCfg = cfg.clone();
                    newCfg.getId().setCode(env.getCode());
                    newCfgList.add(newCfg);

                    if (Global.SYSTEM_ENV_CODE.equals(newCfg.getId().getCfgKey())){
                        newCfg.setCfgValue(env.getCode());
                    }
                }
                clusterDao.saveAll(newCfgList);
                Global.loadCfg(newCfgList);
            }
        } else {  // 更新环境
            if (!opt.isPresent()) {
                throw new ClusterException(ReturnCode.CODE_ENV_NOT_EXIST, "环境不存在");
            }
            SysClusterEnv preEnv = opt.get();
            if (!preEnv.decryptPassword().equals(env.getPassword())) {
                throw new ClusterException(ReturnCode.CODE_ENV_PASSWORD_ERROR, "环境密码错误");
            }
            env.encryptPassword();
            env.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));

            loadLicense(env);

            envDao.save(env);
        }
        return env;
    }

    private void loadLicense(SysClusterEnv env) {
        try {
            License license = resolveLicense(env.getLicense(), env.getCode());
            Global.addLicenseIp(license.getIps());
        } catch (Exception e) {
            if (!config.getEnvironment()) {
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void deleteEnv(SysClusterEnv env) {
        List<SysClusterHost> hosts = hostDao.findAllByIdCode(env.getCode());
        if (hosts.size() > 0) {
            throw new ClusterException(ReturnCode.CODE_ENV_HOST_EXISTED, "环境内还存在主机信息");
        }

        Optional<SysClusterEnv> opt = envDao.findById(env.getCode());
        if (opt.isPresent()) {
            SysClusterEnv preEnv = opt.get();
            if (preEnv.decryptPassword().equals(env.getPassword())) {
                envDao.deleteById(env.getCode());
                clusterDao.deleteByIdCode(env.getCode());
            } else {
                throw new ClusterException(ReturnCode.CODE_ENV_PASSWORD_ERROR, "环境密码错误");
            }
        } else {
            throw new ClusterException(ReturnCode.CODE_ENV_NOT_EXIST, "环境不存在");
        }
    }

    @Override
    public List<SysClusterEnv> findEnv() {
        List<SysClusterEnv> envs = envDao.findAll();
        for(SysClusterEnv env: envs){
            List<SysClusterHost> hosts = hostDao.findAllByIdCode(env.getCode());
            String memo = "";
            for (SysClusterHost host: hosts){
                memo+=(host.getId().getIp()+",");
            }
            env.setMemo(memo);
        }
        return envs;
    }

    @Override
    public Boolean entryEnv(SysClusterEnv env) {
        Optional<SysClusterEnv> opt = envDao.findById(env.getCode());
        if (opt.isPresent()) {
            SysClusterEnv preEnv = opt.get();
            if (preEnv.decryptPassword().equals(env.getPassword())) {
                preEnv.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
                envDao.save(preEnv);
                loadLicense(preEnv);
                return true;
            } else {
                throw new ClusterException(ReturnCode.CODE_ENV_PASSWORD_ERROR, "环境密码错误");
            }
        } else {
            throw new ClusterException(ReturnCode.CODE_ENV_NOT_EXIST, "环境不存在");
        }
    }

    @Override
    public boolean checkLicense(SysClusterEnv env) {
        try {
            License license = resolveLicense(env.getLicense(),env.getCode());
            List<SysClusterHost> hosts = hostDao.findAllByIdCode(env.getCode());
            for (SysClusterHost host:hosts){
                if(!license.getIps().contains(host.getId().getIp())){
                    throw new ClusterException(ReturnCode.CODE_ENV_LICENSE_MATCH,"LICENSE 主机不在License中");
                }
            }
            return true;
        }catch (ClusterException e){
            LOG.error("license 解析失败 编码：{},描述: {}, 详细:{}",e.getCode(),e.getMsg(),e.getDetail());
        }catch (Exception e){
            LOG.error("license 解析失败 详情:{}",e.getMessage());
        }
        return false;
    }

    @Override
    public boolean checkEnv() {
        return null!=config.getEnvironment()&&config.getEnvironment();
    }

    @Override
    public License getEnvLicense(String code) {
        Optional<SysClusterEnv> opt = envDao.findById(code);
        if(opt.isPresent()){
            SysClusterEnv env = opt.get();
            String license = env.getLicense();
            return resolveLicense(license,code);
        }
        return null;
    }

    private static License resolveLicense(String licenseStr,String code){
        String licenseJson = DESUtil.decrypt(licenseStr,code);
        License license = JSON.parseObject(licenseJson,License.class);
        if (null==license||null==license.getIps()||license.getIps().isEmpty()){
            throw new ClusterException(ReturnCode.CODE_ENV_LICENSE_RESOLVE,"LICENSE 解析失败，解析结果未空");
        }

        for (String ip: license.getIps()){
            validIP(ip);
        }
        return license;
    }

    private static void validIP(String ip){
        if(!ipPattern.matcher(ip).find()){
            throw new ClusterException(ReturnCode.CODE_ENV_LICENSE_RESOLVE,"LICENSE IP解析失败");
        }
    }

    @Override
    public String getLicense(String code) {
        Optional<SysClusterEnv> opt = envDao.findById(code);

        // 如果是本地环境的话，初始化license
        if (opt.isPresent()&&checkEnv()){
            String license = LicenseUtil.getLicense(code);
            SysClusterEnv env = opt.get();
            if(null!=license){
                env.setLicense(license);
            }
        }

        return opt.map(SysClusterEnv::getLicense).orElse(null);
    }

    @Override
    public void getFaqs(String fileName, HttpServletResponse resp) throws IOException {
        if (fileName.endsWith(".md")){
            resp.setContentType("text/html;charset=utf-8");
        }else if(fileName.endsWith(".pdf")){
            resp.setContentType("application/pdf");
        }else if(fileName.endsWith(".docx")){
            resp.setContentType("application/msword");
        }else{
            resp.setContentType("application/text;charset=utf-8");
        }
        FileUtils.readFiles(resp,Global.getAnsibleDir(SysClusterEnv.DEFAULT_ENV_CODE)+ File.separator + fileName);
    }
}
