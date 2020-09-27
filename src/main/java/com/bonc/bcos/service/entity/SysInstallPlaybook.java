package com.bonc.bcos.service.entity;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.service.exception.ClusterException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

@Entity
@Table(name = "`sys_install_playbook`")
@Data
public class SysInstallPlaybook implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SysInstallPlaybook.class);

    private static final long serialVersionUID = -2102082194607883083L;
    private static final String PLAYBOOK_BIN="source /etc/profile; ansible-playbook";

    @Id
    @Column(name = "`id`")
    private Long id;

    @Column(name = "`play_code`", length = 32)
    private String playCode;

    @Column(name = "`playbook`", length = 32)
    private String playbook;

    @Column(name = "`filename`", length = 32)
    private String filename;

    @Column(name = "`playbook_name`", length = 32)
    private String playbookName;

    /**
     * 同一个play 下面的playbook 的index 从 1 一直持续增长
     */
    @Column(name = "`index`")
    private int index;

    /**
     * 构造标识：默认是true true : 对应playbook 的数据可以构造成功 false : 对应playbook 的数据构造失败
     */
    @Transient
    private boolean flag = true;

    /**
     * 这个playbook 包含多组控制机，按group分组
     */
    @Transient
    private HashMap<String, HashMap<String, SysClusterHost>> groups = new HashMap<>();

    public void addGroups(SysInstallHostControl role) {
        if (null != role && !StringUtils.isEmpty(role.getRoleCode())) {
            groups.put(role.getGroup(), role.getHosts());
        }
    }

    /**
     * 在控制机初始化ansible的hosts文件, 返回文件路径
     */
    private String initPlaybookInv(String code,String taskName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for (String roleName : getGroups().keySet()) {
            buffer.append("[").append(roleName).append("]").append("\n");
            for (String ip : getGroups().get(roleName).keySet()) {
                getGroups().get(roleName).get(ip).initHostInventory(buffer);
            }
            buffer.append("\n");
        }

        String hostPath = Global.getWorkDir(code) +File.separator +"boots"+ File.separator + "hosts" + File.separator;
        File dir = new File(hostPath);
        if(!dir.exists()&&!dir.mkdirs()){
            LOG.error("主机目录不存在且创建失败！");
            throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_DIR_NOT_EXISTED,  new ArrayList<>(),"主机host文件目录不存在，且无法创建");
        }
        String invFilePath = hostPath + taskName+ "-" + playbook + ".host";
        FileWriter invWriter = new FileWriter(invFilePath, false);
        invWriter.write(buffer.toString());
        invWriter.close();

        return invFilePath;
    }

    /**
     * 在控制机初始化ansible的json变量文件, 返回文件路径
     */
    private String initPlaybookParam(String code,String taskName,String paramStr) throws IOException {
        String paramPath = Global.getWorkDir(code) +File.separator +"boots"+ File.separator + "param" + File.separator;
        File dir = new File(paramPath);
        if(!dir.exists()&&!dir.mkdirs()){
            LOG.error("主机目录不存在且创建失败！");
            throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_DIR_NOT_EXISTED,  new ArrayList<>(),"主机host文件目录不存在，且无法创建");
        }
        String paramFilePath = paramPath + taskName+ "-" + playbook + ".json";
        FileWriter invWriter = new FileWriter(paramFilePath, false);
        invWriter.write(paramStr);
        invWriter.close();

        return paramFilePath;
    }

    public String generateCmd(String code,String uuid, String paramStr) throws IOException {
        return PLAYBOOK_BIN + " " + filename + " -i " + initPlaybookInv(code,uuid) + " -e @" + initPlaybookParam(code,uuid,paramStr);
    }
}
