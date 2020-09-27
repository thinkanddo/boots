package com.bonc.bcos.service.service.impl;

import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.*;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.repository.*;
import com.bonc.bcos.service.service.ClusterService;
import com.bonc.bcos.service.service.HostService;
import com.bonc.bcos.sys.Global;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("clusterService")
public class ClusterServiceImpl implements ClusterService {
	private final SysClusterInfoRepository clusterInfoDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterRoleRepository clusterRoleDao;
	private final SysClusterRoleNumRepository clusterRoleNumDao;
	private final SysClusterRolePolicyRepository clusterRolePolicyDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterHostRoleDevDao;
	private final SysClusterStoreCfgRepository clusterStoreCfgDao;
	private final SysClusterVersionImageRepository imageDao;

	private final HostService hostService;

	@Autowired
	public ClusterServiceImpl(SysClusterInfoRepository clusterInfoDao, SysClusterHostRepository clusterHostDao, SysClusterRoleNumRepository clusterRoleNumDao,
							  SysClusterStoreCfgRepository clusterStoreCfgDao, SysClusterHostRoleRepository clusterHostRoleDao,
							  HostService hostService, SysClusterRoleRepository clusterRoleDao, SysClusterRolePolicyRepository clusterRolePolicyDao,
							  SysClusterHostRoleDevRepository clusterHostRoleDevDao, SysClusterVersionImageRepository imageDao) {
		this.clusterInfoDao = clusterInfoDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterRoleNumDao = clusterRoleNumDao;
		this.clusterRoleDao = clusterRoleDao;
		this.clusterRolePolicyDao = clusterRolePolicyDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterHostRoleDevDao = clusterHostRoleDevDao;
		this.clusterStoreCfgDao = clusterStoreCfgDao;

		this.hostService = hostService;
		this.imageDao = imageDao;
	}

	@Override
	@Transactional
	public void saveRoles(List<SysClusterHost> hosts,String code) {
		// 1.涉及的主机状态必须都校验通过     (1)判断传入的ip是否存在于数据表中      (2)ip对应的主机是否已经校验通过    (3)ip是否已经锁住
		List<String> installedRole = new ArrayList<>();

		// (4)判断需要操作的角色主机是否包含已经安装好的主机     如果已安装好则不能进行操作
		HashMap<String,HashMap<String, SysClusterHostRole>> hostRoleMap = new HashMap<>();
		for (SysClusterHost host: hosts){
			List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findAllByCodeAndIp(host.getId().getCode(),host.getId().getIp());
			for (SysClusterHostRole role:hostRoles){
				if (!hostRoleMap.containsKey(role.getIp())){
					hostRoleMap.put(role.getIp(),new HashMap<>());
				}
				hostRoleMap.get(role.getIp()).put(role.getRoleCode(),role);
			}
		}

		for (SysClusterHost host: hosts){
			// 新增主机角色逻辑
			for (String roleCode: host.getRoles().keySet()){
				if (!hostRoleMap.containsKey(host.getId().getIp())||!hostRoleMap.get(host.getId().getIp()).containsKey(roleCode)){
					SysClusterHostRole role = clusterHostRoleDao.save(new SysClusterHostRole(host.getId().getCode(),host.getId().getIp(),roleCode));
					host.getRoles().put(roleCode,role);
				}
			}
			// 删除主机角色逻辑
			if (hostRoleMap.containsKey(host.getId().getIp())){
				for (String roleCode: hostRoleMap.get(host.getId().getIp()).keySet()){
					if (null!=host.getRoles()&&!host.getRoles().containsKey(roleCode)){
						SysClusterHostRole hostRole = hostRoleMap.get(host.getId().getIp()).get(roleCode);
						if (hostRole.isInstalled()){
							installedRole.add(hostRole.getIp()+":"+hostRole.getRoleCode());
						}else{
							clusterHostRoleDao.deleteById(hostRole.getId());
							clusterHostRoleDevDao.deleteByHostRoleId(hostRole.getId());
						}
					}
				}
			}
		}

		if (!installedRole.isEmpty())		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,installedRole,"已经安装的好的角色不可删除");}
	}

	@Override
	@Transactional
	public void saveGlobal(Map<String, String> global,String code)  {
		// 如果没有登陆使用默认的全局参数
		if (code==null){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}
		for(String key:global.keySet()){
			SysClusterKey clusterKey = new SysClusterKey(code,key);
			SysClusterInfo cfg = Global.getEntity(clusterKey);
			if (cfg == null)		{continue;}
			cfg.setCfgValue(global.get(key));
			clusterInfoDao.save(cfg);

			//增加特殊key 业务控制逻辑
			switch (key){
				case SysClusterInfo.COMPOSE_OPERATOR_INSTALL_FLAG:
					List<SysClusterRole> roles = clusterRoleDao.findByRoleCode(key.split("_")[1].toLowerCase());
					for(SysClusterRole role:roles){
                        role.setStatus(Boolean.parseBoolean(cfg.getCfgValue().trim()));
                        clusterRoleDao.save(role);
                    }
					break;
			}
		}
	}

	@Override
	public List<SysClusterInfo> findGlobal(String code) {
		// 如果没有登陆使用默认的全局参数
		if (code==null){
			code = SysClusterEnv.DEFAULT_ENV_CODE;
		}
		List<SysClusterInfo> globals = clusterInfoDao.findAllByIdCode(code);
		globals.removeIf(SysClusterInfo::isHidden);
		return globals;
	}

	@Override
	public List<SysClusterStoreCfg> storeCfg() {
		return clusterStoreCfgDao.findAll();
	}

	@Override
	public List<SysClusterRole> roleCfg() {
	    List<SysClusterRole> roles = clusterRoleDao.findAll();
	    // 刪除禁用的角色
        roles.removeIf(role -> !role.isEnable());
		return roles;
	}

	@Override
	public void unlockHost(SysClusterHost host,String code){
		if (null!=host && host.getHostLock()){
			host.setHostLock(false);
			host.encodePassword();
			if (!code.equals(host.getId().getCode())){
				throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"主机与登陆环境不匹配");
			}
			clusterHostDao.save(host);
		}
	}

	private class RolePolicyHelper {
		// 推荐主机信息
		private List<SysClusterHost> hosts;
		// 角色安装在那些机器上面
		Map<String,List<SysClusterHost>> roleHost = new HashMap<>();
		// 角色亲和性控制
		Map<String,List<SysClusterRolePolicy>> policyCtl = new HashMap<>();
		// 数量亲和性控制
		Map<String,List<SysClusterRoleNum>> numCtl = new HashMap<>();

		// 角色推荐顺序
        List<SysClusterRole> roles ;

        boolean defaultType = false;

		RolePolicyHelper(List<SysClusterHost> hosts,boolean defaultType) {
			this(hosts);
			this.defaultType = defaultType;
		}

		RolePolicyHelper(List<SysClusterHost> hosts) {
			this.hosts = hosts;
			for (SysClusterHost host: hosts){
				for (String roleCode: host.getRoles().keySet()){
					if(!roleHost.containsKey(roleCode)){
						roleHost.put(roleCode,new ArrayList<>());
					}
					roleHost.get(roleCode).add(host);
				}
			}
//			roleHost.put(SysClusterRole.DEFAULT_ROLE,hosts);
			roles = roleCfg();
		}

		private void initRolePolicyCtl() {
			List<SysClusterRolePolicy> policyList = clusterRolePolicyDao.findAllByOrderById();
			for (SysClusterRolePolicy policy: policyList){
				if(!policyCtl.containsKey(policy.getRoleCode())){
					policyCtl.put(policy.getRoleCode(),new ArrayList<>());
				}
				policyCtl.get(policy.getRoleCode()).add(policy);
			}
		}

		private void initRoleNumCtl() {
			List<SysClusterRoleNum> numList = clusterRoleNumDao.findAllByOrderByRefNum();
			for (SysClusterRoleNum num: numList){
				if(!numCtl.containsKey(num.getRoleCode())){
					numCtl.put(num.getRoleCode(),new ArrayList<>());
				}
				numCtl.get(num.getRoleCode()).add(num);
			}
		}

		void initRoleCtl(){
			initRolePolicyCtl();
			initRoleNumCtl();

			// 填充默认角色控制数据
			for (SysClusterRole role: roles){
				String roleCode = role.getRoleCode();
				if (!roleHost.containsKey(roleCode)){
					roleHost.put(roleCode,new ArrayList<>());
				}
				if (!policyCtl.containsKey(roleCode)){
					policyCtl.put(roleCode,new ArrayList<>());
				}
				if(!numCtl.containsKey(roleCode)){
					numCtl.put(roleCode,new ArrayList<>());
				}
			}
		}

		void doPolicy(){
			for(SysClusterRole role: roles){
				if (defaultType&&role.getRoleType()!=SysClusterRole.HIDE_ROLE){
					continue;
				}
				policyCheck(role.getRoleCode());
			}
		}

		/**
		 *  判断一个角色还差几个机器没满足，如果没有配置数目控制，将返回一个很大的值，标识始终使用策略控制
		 * @param roleCode 角色信息
		 * @return 差额主机数
		 */
		private int checkNum(String roleCode) {
			List<SysClusterRoleNum> roleNumCtl = numCtl.get(roleCode);
			int roleNum = roleNumCtl.size()==0? Integer.MAX_VALUE:0;
			int roleSize = roleHost.get(roleCode).size();
			for (SysClusterRoleNum ctl : roleNumCtl){
				int refNum = roleHost.get(ctl.getRef()).size();
				if(refNum >= ctl.getRefNum()){
					if (ctl.getRefType() == SysClusterRoleNum.TYPE_NUM){
						roleNum = ctl.getRoleNum();
					}
					if(ctl.getRefType() == SysClusterRoleNum.TYPE_PERCENT ){
						roleNum = refNum*ctl.getRoleNum()/100;
					}
				}
			}
			return roleNum-roleSize;
		}

		/**
		 * 根据数量
		 * @param roleCode 需要推荐的角色编码
		 */
		private void policyCheck(String roleCode) {
            boolean hasForce = false;

            Set<SysClusterHost> bindHost = new HashSet<>();

			// 如果策略是强控制，强控制的角色一般就一个控制规则并且和数量一般没有关系了
			for (SysClusterRolePolicy policy: policyCtl.get(roleCode)){
				if (policy.getForce()==SysClusterRolePolicy.FORCE){
					hasForce = true;
					for (SysClusterHost host: hosts){
						// 强制亲和依赖的角色，并且主机上包含依赖的角色 => 增加添加该主机 || 强制反亲和依赖的角色，并且主机上不包含依赖的角色 => 增加添加该主机
						if ((policy.getAffine()&&host.getRoles().containsKey(policy.getRef()))||(!policy.getAffine()&&!host.getRoles().containsKey(policy.getRef()))){
							bindHost.add(host);
						}
					}
				}
			}

			// 根据强制逻辑，将角色绑定到特定的节点上面
			if (hasForce){
				for (SysClusterHost host: hosts){
					if (bindHost.contains(host)&&!host.getRoles().containsKey(roleCode)){
						host.getRoles().put(roleCode,new SysClusterHostRole(host.getId().getCode(),host.getId().getIp(),roleCode));
						roleHost.get(roleCode).add(host);
					} else if (!bindHost.contains(host)&&host.getRoles().containsKey(roleCode)&&!host.getRoles().get(roleCode).isInstalled()){
						host.getRoles().remove(roleCode);
						roleHost.get(roleCode).remove(host);
					}
				}
			}else{
				List<SysClusterHost> candidates = new ArrayList<>();

				// 目标数量只是参考，真正的角色选择主要还是靠策略
				int num = checkNum(roleCode);

				boolean hasWeakForce = false;

				// 根据弱控制选择候选主机
				for (SysClusterRolePolicy policy: policyCtl.get(roleCode)){
					if (policy.getForce()==SysClusterRolePolicy.WEAK_FORCE||policy.getForce()==SysClusterRolePolicy.NO_FORCE){
						if (policy.getForce()==SysClusterRolePolicy.WEAK_FORCE){
							hasWeakForce = true;
						}
						for (SysClusterHost host:hosts){
							if (((policy.getAffine()&&host.getRoles().containsKey(policy.getRef()))||(!policy.getAffine()&&!host.getRoles().containsKey(policy.getRef())))){
								candidates.add(host);
							}
						}
					}
				}

				// 如果弱依赖的话，将候选人以外的节点剔去
				if (hasWeakForce){
					for (SysClusterHost host: hosts){
						if (!candidates.contains(host)&&host.getRoles().containsKey(roleCode)&&!host.getRoles().get(roleCode).isInstalled()){
							host.getRoles().remove(roleCode);
							roleHost.get(roleCode).remove(host);
							num++;
						}
					}
				}

				if (num>0&&candidates.size()>0){
                    num = policyRole(roleCode, num, candidates);
				}

				if(num>0&&!hasWeakForce){
					candidates =new ArrayList<>();
					for (SysClusterHost host: hosts){
						if (!host.getRoles().containsKey(roleCode)){
							candidates.add(host);
						}
					}
					if (candidates.size()>0){
						policyRole(roleCode, num, candidates);
					}
				}
            }
		}

        private int policyRole(String roleCode, int num, List<SysClusterHost> candidates) {
		    int size = candidates.size()>num?num:candidates.size();
            List<SysClusterHost> targets = new ArrayList<>(candidates.subList(0,size));
            for (int index=num;index<candidates.size();index++){
                for (int min=0;min<targets.size();min++){
                    if (targets.get(min).getRoles().keySet().size()>candidates.get(index).getRoles().keySet().size()&&!candidates.get(index).getRoles().containsKey(roleCode)){
                        targets.set(min,candidates.get(index));
                        break;
                    }
                }
            }
            for (SysClusterHost host: targets){
                host.getRoles().put(roleCode,new SysClusterHostRole(host.getId().getCode(),host.getId().getIp(),roleCode));
                roleHost.get(roleCode).add(host);
                num--;
            }
            return num;
        }
    }

	/**
	 *  角色安装推荐的核心逻辑
	 * @param hosts 推荐主机的列表
	 * @return  推荐的安装结果
	 */
	@Override
	@Transactional
	public List<SysClusterHost> rolePolicy(List<SysClusterHost> hosts,String code) {
	    if (hosts.isEmpty()){
	        hosts = hostService.findHosts(code);
        }
		RolePolicyHelper policyHelper = new RolePolicyHelper(hosts);
		policyHelper.initRoleCtl();
		policyHelper.doPolicy();
		saveRoles(hosts,code);
		return hosts;
	}

	/**
	 *  角色安装推荐的核心逻辑
	 * @return  推荐的安装结果
	 */
	@Override
	@Transactional
	public List<SysClusterHost> rolePolicy(String code) {
		List<SysClusterHost> hosts = hostService.findHosts(code);
		RolePolicyHelper policyHelper = new RolePolicyHelper(hosts,true);
		policyHelper.initRoleCtl();
		policyHelper.doPolicy();
		saveRoles(hosts,code);
		return hosts;
	}

	@Override
	public List<SysClusterVersionImage> findImage(String version) {
		return imageDao.findByVersion(version);
	}

	@Override
	public List<SysClusterHostRole> getRoles(String ip, String code) {
		return clusterHostRoleDao.findByCodeAndIp(code,ip);
	}
}
