package com.bonc.bcos.service.service.impl;

import com.bonc.bcos.service.entity.*;
import com.bonc.bcos.service.repository.*;
import com.bonc.bcos.service.service.CallService;
import com.bonc.bcos.service.service.ClusterService;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("callbackService")
public class CallServiceImpl implements CallService {

	private final SysInstallPlayExecRepository installPlayExecDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterHostRoleDevDao;
    private final SysClusterVersionImageRepository imageDao;
	private final SysClusterEnvRepository envDao;

	private final ClusterService clusterService;

	@Autowired
	public CallServiceImpl(SysInstallPlayExecRepository installPlayExecDao,
                           SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao,
                           SysClusterHostRoleDevRepository clusterHostRoleDevDao, SysClusterVersionImageRepository imageDao,
                           SysClusterEnvRepository envDao, ClusterService clusterService) {
		this.installPlayExecDao = installPlayExecDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterHostRoleDevDao = clusterHostRoleDevDao;
        this.imageDao = imageDao;
        this.envDao = envDao;

		this.clusterService = clusterService;
	}

	@Override
	@Transactional
	public void start(SysInstallPlayExec exec)  {
		exec.setStatus(SysInstallPlayExec.RUNNING);
		installPlayExecDao.save(exec);
	}

	@Override
	@Transactional
	public void finish(SysInstallPlayExec finish) {
		//保存任务状态
		installPlayExecDao.save(finish);

		// 解锁主机
		for(String ip:finish.getTargetIps()){
			SysClusterHost host = clusterHostDao.findByIdCodeAndIdIp(finish.getCode(),ip);
			if  (null!=host){
				clusterService.unlockHost(host, finish.getCode());
			}
		}
	}

	@Override
	@Transactional
	public void saveHost(SysClusterHost host) {
		SysClusterHost hostInfo = clusterHostDao.findByIdCodeAndIdIp(host.getId().getCode(),host.getId().getIp());
		if (null!=hostInfo){
			host.setCreateDate(hostInfo.getCreateDate());
			host.setHostLock(hostInfo.getHostLock());
			host.setPassword(hostInfo.getPassword());
			host.setUsername(hostInfo.getUsername());
			host.setSshPort(hostInfo.getSshPort());
			host.setHasGpu(hostInfo.getHasGpu());
			host.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));

			SysClusterHostRole hostRole = clusterHostRoleDao.findByCodeAndIpAndRoleCode(hostInfo.getId().getCode(),hostInfo.getId().getIp(), SysClusterRole.DEFAULT_ROLE);

			List<SysClusterHostRoleDev> roleDevList = clusterHostRoleDevDao.findByHostRoleIdOrderByDevName(hostRole.getId());
			HashMap<String, SysClusterHostRoleDev> devMap = new HashMap<>();
			for (SysClusterHostRoleDev roleDev:roleDevList){
				devMap.put(roleDev.getDevName(),roleDev);
			}

			// 保存主机信息
			clusterHostDao.save(host);

			//保存设备信息
			if (null!=host.getDevs()&&!host.getDevs().isEmpty()){
				for (SysClusterHostRoleDev roleDev:host.getDevs()){
					roleDev.setIp(hostInfo.getId().getIp());
					roleDev.setCode(hostInfo.getId().getCode());
					roleDev.setHostRoleId(hostRole.getId());

					// 如果设备已经存在，更新就行了
					if (devMap.containsKey(roleDev.getDevName())){
					    SysClusterHostRoleDev dev = devMap.get(roleDev.getDevName());
						roleDev.setId(dev.getId());
						roleDev.setCreateDate(dev.getCreateDate());
						roleDev.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));

						// 设备状态保留原始的状态
                        roleDev.setStatus(dev.getStatus());
					}else{
						roleDev.setCreateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
						roleDev.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
					}

					roleDev.setDevParent(roleDev.getDevName());

					// 如果主机分区类型不支持或者初始化的主机剩余可用空间太小默认不可用
					if (roleDev.getEnableSize()<20||SysClusterHostRoleDev.UNKNOWN_PART_TYPE.equals(roleDev.getPartType())){
						roleDev.disable();
					}
					clusterHostRoleDevDao.save(roleDev);
				}
			}
		}

	}

	@Override
	@Transactional
	public void saveHostRole(SysClusterHostRole hostRole) {
		Optional<SysClusterHostRole> optional =  clusterHostRoleDao.findById(hostRole.getId());
		if (optional.isPresent()){
			SysClusterHostRole hostRoleInfo = optional.get();
			hostRoleInfo.setStatus(hostRole.getStatus());

			// 如果主机经过清理，上面所有的角色设备信息删除掉，角色状态设置为未安装
			if (hostRole.getStatus()==SysClusterHostRole.PRE_HANDLE){
				List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByCodeAndIp(hostRoleInfo.getCode(),hostRoleInfo.getIp());
				for (SysClusterHostRole role: hostRoles){
					if (!SysClusterRole.DEFAULT_ROLE.equals(role.getRoleCode())){
						clusterHostRoleDevDao.deleteByHostRoleId(role.getId());
						role.setStatus(SysClusterHostRole.UNINSTALL);
						clusterHostRoleDao.save(role);
					}
				}
			}
			clusterHostRoleDao.save(hostRoleInfo);
		}
	}

	/**
	 * 只能根据ID更新
	 * @param roleDev 设备信息
	 */
	@Override
	@Transactional
	public void saveRoleDev(SysClusterHostRoleDev roleDev) {
		Optional<SysClusterHostRoleDev> optional = clusterHostRoleDevDao.findById(roleDev.getId());
		if (optional.isPresent()){
			SysClusterHostRoleDev devInfo = optional.get();
			devInfo.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
			devInfo.setStatus(roleDev.getStatus());
			devInfo.setDevSizeUsed(roleDev.getDevSizeUsed());
			devInfo.setDevName(roleDev.getDevName());
			devInfo.setPartType(roleDev.getPartType());
			clusterHostRoleDevDao.save(devInfo);
		}
	}

	@Override
	@Transactional
	public void saveGlobal(Map<String, String> info) {
		clusterService.saveGlobal(info, info.get(Global.SYSTEM_ENV_CODE));
	}

	@Override
	public void saveTags(List<String> info) {
		Global.setTags(info);
	}

	@Override
	public void saveEnv(String code, String tag) {
		List<SysClusterEnv> envList = envDao.findAllByCode(code);
		if (envList.size()>0){
			SysClusterEnv env = envList.get(0);
			env.setVersion(tag);
			env.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
			envDao.save(envList.get(0));
		}
	}

    @Override
	@Transactional
    public void saveImage(List<String> images,String version) {
		List<SysClusterVersionImage> imageList = imageDao.findByVersion(version);
		imageDao.deleteAll(imageList);
		for (String image: images){
			imageDao.save(new SysClusterVersionImage(version,image));
		}
    }
}
