package com.bonc.bcos.config;

import com.bonc.bcos.elfinder.param.Node;
import com.bonc.bcos.elfinder.param.Thumbnail;
import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.entity.SysClusterInfo;
import com.bonc.bcos.service.repository.SysClusterInfoRepository;
import com.bonc.bcos.sys.Global;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix="boots.file-manager") //接收application.yml中的file-manager下面的属性
public class ElfinderConfiguration {

    private final SysClusterInfoRepository clusterInfoDao;

    private static final String ADMIN_VOLUME = Global.SYSTEM_WORK_DIR_ADMIN;

    private Thumbnail thumbnail;

    private List<Node> volumes = new ArrayList<>();

    private Long maxUploadSize = -1L;//默认不限制

    private Node template;

    @Autowired
    public ElfinderConfiguration(SysClusterInfoRepository clusterInfoDao) {
        this.clusterInfoDao = clusterInfoDao;
    }

    void initVolumes() {
        Node admin = template.clone();
        SysClusterInfo info = clusterInfoDao.findAllByIdCodeAndIdCfgKey(SysClusterEnv.DEFAULT_ENV_CODE,Global.SYSTEM_WORK_DIR_ADMIN);
        admin.setAlias(info.getMemo());
        admin.setPath(info.getCfgValue());
        volumes.add(admin);
    }
}
