package com.bonc.bcos.service.tasks;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.SysClusterHost;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.model.CmdDictPo;
import com.bonc.bcos.service.model.CmdPo;
import com.bonc.bcos.service.repository.SysClusterHostRepository;
import com.bonc.bcos.sys.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class CmdExecutor extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(CmdExecutor.class);

    private static final String SHELL_NAME = "/bin/bash";
    private static final String SHELL_PARAM = "-c";
    private static final String PLAYBOOK_BIN="source /etc/profile; ansible ";

    private final SysClusterHostRepository clusterHostDao ;
    
    public CmdExecutor(SysClusterHostRepository clusterHostDao) {
        this.clusterHostDao = clusterHostDao;
    }
    
    /**
     * 将命令集合中的参数转化为OS Runtime中的参数
     *
     * @return 返回Runtime调用参数列表
     */
    private List<String> build(String cmd) {
        return Arrays.asList(SHELL_NAME, SHELL_PARAM, cmd);
    }
    
    //生成hosts文件
    private String initPlaybookInv(CmdPo cmd,String code) throws IOException {
        StringBuffer buffer = new StringBuffer();
        
        SysClusterHost host = clusterHostDao.findByIdCodeAndIdIp(code, cmd.getIp());
        //密码解密
        host.decodePassword();
        
        initHostInventory(buffer,host);
        
        buffer.append("\n");

        String hostPath = Global.getWorkDir(code) +File.separator +"boots"+ File.separator + "hosts" + File.separator;
        File dir = new File(hostPath);
        if(!dir.exists()&&!dir.mkdirs()){
            LOG.error("主机目录不存在且创建失败！");
            throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_DIR_NOT_EXISTED,  new ArrayList<>(),"主机host文件目录不存在，且无法创建");
        }
        
        //host名称为  /bcos/boots/hosts/127_0_0_1.host
        String invFilePath = hostPath + host.getId().getIp().replace(".","-") + ".host";
        FileWriter invWriter = new FileWriter(invFilePath, false);
        invWriter.write(buffer.toString());
        invWriter.close();

        return invFilePath;
    }

    //组装hosts文件内容
    private void initHostInventory(StringBuffer buffer, SysClusterHost host){
    	buffer.append(host.getId().getIp()).append(" ansible_ssh_port=").append(host.getSshPort()).append(" ansible_ssh_user=").append(host.getUsername()).
				append(" ansible_ssh_pass='").append(host.getPassword()).append("' ansible_sudo_pass='").append(host.getPassword()).
				append("' this_hostname=").append(host.getId().getIp().replace(".","-")).append(" this_ip=").append(host.getId().getIp()).append("\n");
	}
    
    
    //拼接执行命令
    private String generateCmd(CmdPo cmdpo,String code) throws IOException {
        return PLAYBOOK_BIN + cmdpo.getIp() + " -i " + initPlaybookInv(cmdpo,code) + " -m shell -a " + "'source /etc/profile;  " + cmdpo.getCmd() + "'";
    }
    
    public CmdDictPo exec(CmdPo cmdpo,String code) throws IOException, InterruptedException {
    	
    	CmdDictPo cmddictpo = new CmdDictPo();
    	
    	// 根据任务生成playbook 的host文件
    	//1.拼接执行命令
    	// ansible master[0] -m shell -a 'source /etc/profile ; kubectl get po -n bcos' -i /bcos/boots/hosts/6216b.host
    	//(1)组装hosts文件
        String cmd = generateCmd(cmdpo,code);


        List<String> cmdList = build(cmd);
    	
    	// 2.构造进程执行器
        ProcessBuilder pb = new ProcessBuilder(cmdList.toArray(new String[0])).redirectErrorStream(true);
        
        // 3.启动进程
        Process process = pb.start();
        
        // 4.获取进程输出流stdout读取器
        LOG.info("命令执行结果{}",cmd);

        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        LOG.info("获取进程输出流stdout读取成功");

        // 解析ansible 输出
        String line = br.readLine();
        String[] str=line.split("\\|");

        if (str.length >= 3) {
            cmddictpo.getStdoutPo().setIp(str[0].trim());;
            cmddictpo.getStdoutPo().setResult(str[1].trim());
            cmddictpo.getStdoutPo().setRc(str[2].trim().substring(3,4));
        }else{
            cmddictpo.getStdoutPo().setResult("执行结果解析失败!");
            cmddictpo.getStdoutPo().setRc("500");
        }

        // 解析其他输出
        while ((line = br.readLine()) != null) {
            cmddictpo.getRows().add(line);
        }

        return cmddictpo;
    }

}
