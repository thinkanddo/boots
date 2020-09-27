package com.bonc.bcos.service.service.impl;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.SysClusterInfo;
import com.bonc.bcos.service.entity.SysClusterVersion;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.repository.SysClusterInfoRepository;
import com.bonc.bcos.service.repository.SysClusterVersionRepository;
import com.bonc.bcos.service.service.VersionService;
import com.bonc.bcos.service.tasks.FTPUtil;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.utils.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

@Service
public class VersionServiceImpl implements VersionService {

    private final SysClusterVersionRepository versionDao;
    private final SysClusterInfoRepository clusterDao;

    public VersionServiceImpl(SysClusterVersionRepository versionDao, SysClusterInfoRepository clusterDao) {
        this.versionDao = versionDao;
        this.clusterDao = clusterDao;
    }

    @Override
    public void saveUploadPackage(MultipartFile file, HttpSession session,String code) throws IOException {
        HashMap<String,String> global = Global.getCfgMap(code);


        String curlVersion = global.get("SYSTEM_CUR_VERSION");
        String filename = file.getOriginalFilename();

        if (filename==null){
            throw new ClusterException(ReturnCode.CODE_ENV_VERSION_MATCH,"升级包版本不匹配!");
        }

        String[] version = filename.replace("bcos-admin-","").replace(".tar","").split("-");
        if (version.length!=2||!version[0].equals(curlVersion)){
            throw new ClusterException(ReturnCode.CODE_ENV_VERSION_MATCH,"升级包版本不匹配!");
        }

        SysClusterInfo versionCfg = clusterDao.findAllByIdCodeAndIdCfgKey(code,"SYSTEM_TARGET_VERSION");
        versionCfg.setCfgValue(version[1]);
        clusterDao.save(versionCfg);
        Global.loadCfg(versionCfg);


        String ip = global.get("COMPOSE_FTP_IP");
        String port = global.get("PORT_FTP_CONTROL");
        String user = global.get("COMPOSE_FTP_USERNAME");
        String password = global.get("COMPOSE_FTP_PASSWORD");

        FTPUtil ftpUtil = new FTPUtil(file);
        session.setAttribute(FTPUtil.PACKAGE_LISTENER_KEY,ftpUtil.getListener());
        if(!ftpUtil.connect(ip,port)){
            throw new ClusterException(ReturnCode.CODE_FTP_CONNECT_EXCEPTION,"FTP 登陆失败!");
        }
        if(!ftpUtil.login(user,password)){
            throw new ClusterException(ReturnCode.CODE_FTP_LOGIN_EXCEPTION,"FTP 连接失败!");
        }
        try{
            ftpUtil.send();
        }catch (Exception e){
            throw new ClusterException(ReturnCode.CODE_FTP_UPLOAD_EXCEPTION,"FTP 传输失败!");
        }

        session.setAttribute(FTPUtil.PACKAGE_LISTENER_KEY,null);
    }

    @Override
    public void saveVersion(SysClusterVersion version) {
        version.setCreateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
        versionDao.save(version);
    }

    @Override
    public List<SysClusterVersion> getPack() {
        return versionDao.findAll();
    }

    @Override
    public void deletePack(String packageName) {
        versionDao.deleteById(packageName);
    }

}
