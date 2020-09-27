package com.bonc.bcos.service.service.impl;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.*;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.model.ExecProcess;
import com.bonc.bcos.service.repository.*;
import com.bonc.bcos.service.service.ClusterService;
import com.bonc.bcos.service.service.DevService;
import com.bonc.bcos.service.service.EnvService;
import com.bonc.bcos.service.service.ExecService;
import com.bonc.bcos.service.tasks.TaskManager;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Service("execService")
public class ExecServiceImpl implements ExecService {

	private final SysInstallPlayRepository installPlayDao;
	private final SysInstallPlaybookRepository installPlaybookDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysInstallPlayExecRepository installPlayExecDao;
	private final SysInstallHostControlRepository installHostControlDao;
	private final SysClusterHostRoleDevRepository clusterHostRoleDevDao;

	private EntityManager em;

//	private final PlayExecFactory factory;

	private final ClusterService clusterService;
	private final DevService devService;
	private final EnvService envService;

	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.em = entityManager;
	}

	@Autowired
	public ExecServiceImpl(SysInstallPlayRepository installPlayDao, SysInstallPlaybookRepository installPlaybookDao,
						   SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao,
						   SysInstallPlayExecRepository installPlayExecDao, SysInstallHostControlRepository installHostControlDao,
						   SysClusterHostRoleDevRepository clusterHostRoleDevDao, ClusterService clusterService, DevService devService, EnvService envService) {
		this.installPlayDao = installPlayDao;
		this.installPlaybookDao = installPlaybookDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.installPlayExecDao = installPlayExecDao;
		this.installHostControlDao = installHostControlDao;
		this.clusterHostRoleDevDao = clusterHostRoleDevDao;

		this.clusterService = clusterService;
		this.devService = devService;
		this.envService = envService;
	}

	/**
	 *  playExec 执行数据构造器
	 *
	 *  通过构造playExec 构造playbook 构造hostControl 三级完成playExec 参数的组装
	 *
	 *  注： 每一步构造如果失败都会向上级设置失败，并可以通过playExec 的 getConstructErrs 方法获取失败的详细原因
	 */
	class PlayExecFactory{
		private final String code;
		private final SysInstallPlay play;  // play配置参数
		private final HashMap<String, SysClusterHost> hostMap; // 全量的主机信息
		private final List<String> targets = new ArrayList<>(); //目标机器的ID
		private List<SysInstallHostControl> playbookError = new ArrayList<>();  //返回的错误信息列表
		private final String playName;

		/**
		 * 当play 是部分锁的时候 targets必须有效
		 * @param play		执行计划
		 * @param targets		目标主机
		 */
		PlayExecFactory(String code,SysInstallPlay play,Collection<String> targets,HashMap<String,SysClusterHost> hostMap,String playName) throws ClusterException {
			this.code = code;
			this.play = play;
			//lockType为增量主机  targets目标主机为空，返回error
			if (!play.getLockType()&&(null == targets || targets.isEmpty()))			{throw new ClusterException(ReturnCode.CODE_CLUSTER_PARAM_IS_EMPTY,"目标主机为空");}
			this.hostMap = hostMap;
			this.targets.addAll(targets);
			this.playName = playName;
		}
		
        /**
		 *  构造playExec参数报文
		 */
		void constructPlayExec(SysInstallPlayExec exec) {
			// 生成32位token  组装SysInstallPlayExec exec
			exec.setStatus(SysInstallPlayExec.INIT);
			exec.setTargetsJson(this.targets);
			exec.setPlayName(playName);
			
			//获取执行play的所有的playbook
			List<SysInstallPlaybook> playbooks = installPlaybookDao.findByPlayCodeOrderByIndex(this.play.getPlayCode());

			// 包含继续执行逻辑0
			for (SysInstallPlaybook playbook: playbooks){
			    
			    
				if (playbook.getIndex()<exec.getCurIndex())		{continue;}

				// 构造playbook
				boolean flag = constructPlaybook(playbook);

				// 如果有playbook 构造失败，设置playExec 构造失败
				if (!flag && exec.getFlag())  {exec.setFlag(false);}

				// 添加playbook
				exec.addPlaybook(playbook);
			}
		}

		private boolean constructPlaybook(SysInstallPlaybook playbook) {
		    // 构造playbook
			List<SysInstallHostControl> controls = installHostControlDao.findByPlaybook(playbook.getPlaybook());

			// 构造主机控制列表
			for (SysInstallHostControl control:controls){
				// 获取控制主机列表并构造主机控制列表
				boolean flag = constructGroup(control);

				// 如果主机控制不满足  设置playbook构造失败
				if (!flag&&playbook.isFlag()) {playbook.setFlag(false);}

				// 将主机控制对象添加到playbook里面
				playbook.addGroups(control);
			}

			return playbook.isFlag();
		}
		
		private SysClusterHost constructHost(SysClusterHostRole hostRole) {
		    List<SysClusterHostRoleDev> devs = clusterHostRoleDevDao.findByHostRoleIdOrderByDevName(hostRole.getId());
		    SysClusterHost host = hostMap.get(hostRole.getIp()).clone();
		    for (SysClusterHostRoleDev dev : devs){
		    	for (SysClusterStoreCfg cfg: devService.getStoreCfg(hostRole.getRoleCode())){
		    		if (dev.getName().equals(cfg.getName())){
		    			dev.setStoreType(cfg.getStoreType());
					}
				}
			}
			host.setDevs(devs);
			host.setRoleId(hostRole.getId());
			host.setRoleStatus(hostRole.getStatus());
            return host;
		}

		/**
		 *  构造hostControl 中的主机列表 ，如果构造成功
		 *
		 * @param control  主机控制的限制逻辑配置对象
		 * @return  主机是否具备 true 具备 false 不具备
		 */
		private boolean constructGroup(SysInstallHostControl control) {
			List<String> errorHost = new ArrayList<>();

		    //  1： 查询角色对应的所有机器
			List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByCodeAndRoleCode(code,control.getRoleCode());

			// 2：如果target 生效的话，就剔除不在target 目标当中的主机
			if(control.getTargetEnable()){
				hostRoles.removeIf(hostRole -> !this.targets.contains(hostRole.getIp()));
			}

			// 3： 根据控制方式和状态，筛选出可用的机器
			switch (control.getControl()){
				case SysInstallHostControl.EXACT_MATCH_CTL:
					for (SysClusterHostRole hostRole: hostRoles){
						if (hostRole.getStatus() != control.getStatus()){
							control.setFlag(false);
							errorHost.add(hostRole.getIp());
							continue;
						}
						control.addHost(constructHost(hostRole));
					}
					break;
				case SysInstallHostControl.STATUS_MATCH_CTL:
					for (SysClusterHostRole hostRole: hostRoles){
						if (hostRole.getStatus() == control.getStatus()){
							control.addHost(constructHost(hostRole));
						}
					}
					break;
				case SysInstallHostControl.NONE_MATCH_CTL:
					for (SysClusterHostRole hostRole: hostRoles){
						control.addHost(constructHost(hostRole));
					}
					break;
			}
			
			// 4：如果有状态不对的主机，将control 的hosts设定为异常的主机清单用于数据组装   清空control
			if (!control.getFlag()) {
				control.clearHosts();
				for (String hostId:errorHost){
					control.addHost(this.hostMap.get(hostId));
				}
				putError(control);
			}

			return control.getFlag();
		}
		
		
        private void putError(SysInstallHostControl control) {
            playbookError.add(control);
        }
        
        /**
         * 获取playExec 组装失败的原因
         * @return  组装失败的原因清单
         */
        List<String> getErrorMsg(){
            List<String> errorMsg = new ArrayList<>();
            for(SysInstallHostControl con:this.playbookError) {
                errorMsg.add(con.getConstructMsg());
            }
            return errorMsg;
        }
	}

	/**
	 * if targets is null checkAllHost  or check targets host
	 * @param targets 目标机器  如果为[] ,视为全部主机加锁
	 * @return all host map
	 */
	private synchronized HashMap<String,SysClusterHost> lockHosts(Set<String> targets,String code) throws ClusterException {
		List<SysClusterHost> hosts = clusterHostDao.findAllByIdCode(code);

		em.clear();

		boolean flag = targets.isEmpty();

		// 获取有冲突的主机，先查询当前锁住的主机，再和targets取交集
		List<String> lockHostIps = new ArrayList<>();
		HashMap<String,SysClusterHost> hostMap=new HashMap<>();
		for (SysClusterHost host : hosts){
			if(flag){
				targets.add(host.getId().getIp());
			}
			//dqy  如果targets包含hostip，并且hostLock主机锁为true，代表有其他play操作当前targets主机，此次play不能执行
			if (targets.contains(host.getId().getIp())&&host.getHostLock()){
				lockHostIps.add(host.getId().getIp());
			}
			hostMap.put(host.getId().getIp(),host);
		}

		if (!lockHostIps.isEmpty()) 		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK, lockHostIps,"部分主机已锁住");}

		if (!hostMap.keySet().containsAll(targets))				{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK, targets,"主机不存在");}

		// 对主机加锁
		for (String ip:targets){
			SysClusterHost host = hostMap.get(ip);
			host.setHostLock(true);
			clusterHostDao.save(host);

			if(!envService.checkEnv()&&!Global.isInstall(host.getId().getCode())&&!Global.containIp(host.getId().getIp())){
				throw new ClusterException(ReturnCode.CODE_ENV_LICENSE_MATCH,"LICENSE 没有匹配主机"+host.getId().getIp());
			}

			// 加上锁之后解码主机密码，然后执行任务
			host.decodePassword();
		}
		return hostMap;
	}

	@Override
	@Transactional
	public ExecProcess exec(List<String> targets, String playCode,String code)  {
		SysInstallPlayExec exec = new SysInstallPlayExec(playCode,code);
		playExec(exec,targets);
		return new ExecProcess(exec);
	}

	private void playExec(SysInstallPlayExec exec, List<String> targets) throws ClusterException {
		//获取play
		List<SysInstallPlay> plays = installPlayDao.findByPlayCode(exec.getPlayCode());
		if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
		SysInstallPlay play = plays.get(0);

		if (play.isAdmin()){
			exec.setCode(SysClusterEnv.DEFAULT_ENV_CODE);
		}

		if (StringUtils.isEmpty(exec.getCode())){
			throw new ClusterException(ReturnCode.CODE_ENV_NOT_EXIST,"请登陆环境执行任务操作！");
		}

		if (null==exec.getTimeout()){
			exec.setTimeout(play.getTimeout());
		}

		//根据play中的lockType，获得需要操作的目标主机    true全量主机   false当前targets主机
		HashSet<String> ips = play.getLockType() || null == targets? new HashSet<>():new HashSet<>(targets);

		// step 1 主机校验，主机锁校验   {ip:SysClusterHost}
		HashMap<String,SysClusterHost> hashMap = lockHosts(ips,exec.getCode());

		// step 2  constructPlayExec  通过playExec构造器完成 执行参数的构造
		PlayExecFactory playExecFactory = new PlayExecFactory(exec.getCode(),play,ips,hashMap,play.getPlayName());
		playExecFactory.constructPlayExec(exec);

		if (!exec.getFlag())                                {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_CANT_EXEC, playExecFactory.getErrorMsg(),"当前的集群状态不具备执行此任务！");}

		// 为当前操作计次
		exec.setTimes(exec.getTimes()+1);

		// step 3 insertPlayExec  确保exec的主键自动生成
		installPlayExecDao.save(exec);

		// step 4 callDriver
		try{
			TaskManager.create(exec);
		}catch (Exception e){
			e.printStackTrace();
			throw new ClusterException(ReturnCode.CODE_CLUSTER_BOOTSTRAP_CALL_FAIL,"执行任务调度失败！");
		}
	}

	@Override
	@Transactional
	public void resume(String uuid,String code) {
		Optional<SysInstallPlayExec> execOpt = installPlayExecDao.findById(uuid);
		if (!execOpt.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}

		SysInstallPlayExec exec = execOpt.get();
		if (!exec.isStop())					{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_FAILED,"该任务状态不可恢复执行");}

		List<SysInstallPlay> plays = installPlayDao.findByPlayCode(exec.getPlayCode());
		if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
		SysInstallPlay play = plays.get(0);

		if (play.isAdmin()){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}

		if (!code.equals(exec.getCode()))					{throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"该任务与当前环境不匹配");}

		playExec(exec,exec.getTargetIps());
	}

	@Override
    @Transactional
	public void pause(String uuid,String code) {
        Optional<SysInstallPlayExec> execOpt = installPlayExecDao.findById(uuid);
        if (!execOpt.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}
        SysInstallPlayExec exec = execOpt.get();
        
        List<SysInstallPlay> plays = installPlayDao.findByPlayCode(exec.getPlayCode());
		if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
		SysInstallPlay play = plays.get(0);

		if (play.isAdmin()){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}

		if (!SysClusterEnv.DEFAULT_ENV_CODE.equals(code)&&!code.equals(exec.getCode())){
			throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"该任务与当前环境不匹配");
		}

        if (!exec.isRun())					{throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_RUNNING,"当前任务未在运行中");}

		TaskManager.destroy(uuid,SysInstallPlayExec.PAUSE,"任务暂停");
	}

	@Override
	@Transactional
	public void stop(String uuid, String code) {
		Optional<SysInstallPlayExec> execOpt = installPlayExecDao.findById(uuid);
		if (!execOpt.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}
		SysInstallPlayExec exec = execOpt.get();

		List<SysInstallPlay> plays = installPlayDao.findByPlayCode(exec.getPlayCode());
		if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
		SysInstallPlay play = plays.get(0);

		if (play.isAdmin()){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}

		if (!SysClusterEnv.DEFAULT_ENV_CODE.equals(code)&&!code.equals(exec.getCode())){
			throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"该任务与当前环境不匹配");
		}

		if (exec.isRun()){
			TaskManager.destroy(uuid,SysInstallPlayExec.CLOSE,"结束任务");
		}else{
			exec.setStatus(SysInstallPlayExec.CLOSE);
			exec.setMessage("手动任务结束！\n"+exec.getMessage());
			installPlayExecDao.save(exec);
		}
	}

	@Override
	public ExecProcess query(String uuid,String code)  {
		SysInstallPlayExec exec = TaskManager.get(uuid);
		if (null == exec){
			Optional<SysInstallPlayExec> optional = installPlayExecDao.findById(uuid);
			if (!optional.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}

			exec = optional.get();

			//获取play
			List<SysInstallPlay> plays = installPlayDao.findByPlayCode(exec.getPlayCode());
			if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
			SysInstallPlay play = plays.get(0);

			if (play.isAdmin()){
				code = SysClusterEnv.DEFAULT_ENV_CODE;
			}
			
			if (!SysClusterEnv.DEFAULT_ENV_CODE.equals(code)&&!code.equals(exec.getCode())){
				throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"该任务与当前环境不匹配");
			}

			exec.setPlaybooks(initPlaybooks(exec.getPlayCode()));
		}
		return new ExecProcess(exec);
	}

    @Override
    public List<SysInstallPlaybook> initPlaybooks(String playCode) {
        return installPlaybookDao.findByPlayCodeOrderByIndex(playCode);
    }

	@Override
	public ExecProcess getLatestTask(String playCode,String code) {
		List<SysInstallPlay> plays = installPlayDao.findByPlayCode(playCode);
		if (plays.size()==0||!plays.get(0).isEnable())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不支持该任务类型");}
		SysInstallPlay play = plays.get(0);

		if (play.isAdmin()){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}

		List<SysInstallPlayExec> execList = installPlayExecDao.findAllByPlayCodeAndCodeOrderByCreateDateDesc(playCode,code);
		if (execList.size()>0){
			return new ExecProcess(execList.get(0));
		}
		return null;
	}

	@Override
	@Transactional
	public void reset() {
		// 暂停所有任务
		TaskManager.reset();

		// 将所有运行中的PLAY状态置为失败
		List<SysInstallPlayExec> execs = installPlayExecDao.findByStatus(SysInstallPlayExec.RUNNING);
		for (SysInstallPlayExec exec:execs){
			exec.setStatus(SysInstallPlayExec.FAILED);
		}
		installPlayExecDao.saveAll(execs);

		// 注意 如果 playboot 执行进度字段=100 表示任务虽然失败了，但是实际是成功的，将会更新为成功状态，比如升级boots 重启boots 之类的任务，将在任务内提前将percent 更新为100
		List<SysInstallPlayExec> failedExecs = installPlayExecDao.findByStatus(SysInstallPlayExec.FAILED);
		for (SysInstallPlayExec exec:failedExecs){
			if (exec.getPercent()==100){
				exec.setStatus(SysInstallPlayExec.SUCCESS);
			}
		}
		installPlayExecDao.saveAll(failedExecs);

		// 解锁所有主机
		List<SysClusterHost> hosts = clusterHostDao.findAll();
		for(SysClusterHost host:hosts){
			clusterService.unlockHost(host,host.getId().getCode());
		}
	}

	@Override
	public List<SysInstallPlay> findPlays() {
		return installPlayDao.findAll();
	}

	@Override
	public List<SysInstallPlayExec> findLogs(String code) {
		return installPlayExecDao.findAllByCode(code);
	}

	@Override
	public Object downLog(HttpServletResponse res, String uuid,String code) throws IOException {
		res.setContentType("text/html;charset=utf-8");
		ExecProcess process = query(uuid,code);
		PrintWriter out=res.getWriter();
		out.println(process.getStdout());
		return process.getTaskId();
	}

	@Override
	public Boolean deleteTask(String uuid) {
		TaskManager.destroy(uuid,SysInstallPlayExec.INIT,"手动删除任务");
		installPlayExecDao.deleteById(uuid);
		return true;
	}

	@Override
	public SysInstallPlayExec editTask(SysInstallPlayExec exec, String code) {
		Optional<SysInstallPlayExec> opt = installPlayExecDao.findById(exec.getUuid());
		if (opt.isPresent()){
			SysInstallPlayExec task = opt.get();
			task.setCurIndex(exec.getCurIndex());
			task.setStatus(exec.getStatus());
			installPlayExecDao.save(task);
			return task;
		}
		throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");
	}

}
