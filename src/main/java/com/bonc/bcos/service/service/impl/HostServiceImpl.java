package com.bonc.bcos.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.*;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.repository.SysClusterHostRepository;
import com.bonc.bcos.service.repository.SysClusterHostRoleDevRepository;
import com.bonc.bcos.service.repository.SysClusterHostRoleRepository;
import com.bonc.bcos.service.service.HostService;
import com.bonc.bcos.utils.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

@Service("hostService")
public class HostServiceImpl implements HostService {

	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterHostRoleDevDao;

    private EntityManager em;

	@Autowired
	public HostServiceImpl(SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao, SysClusterHostRoleDevRepository clusterHostRoleDevDao) {
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterHostRoleDevDao = clusterHostRoleDevDao;
	}

	private void checkHostLock(SysClusterHostKey id) throws ClusterException {
        SysClusterHost host = clusterHostDao.findByIdCodeAndIdIp(id.getCode(),id.getIp());
		if(null!=host){
		    if (host.getHostLock()){
                throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,"有正在执行的任务使用该主机，请稍后更新集群状态");
            }
            if (!id.getCode().equals(host.getId().getCode())){
                throw new ClusterException(ReturnCode.CODE_ENV_NOT_MATCH,"该主机在其他环境已经存在！");
            }
		}
	}

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.em = entityManager;
    }

	@Override
	@Transactional
	public void saveHost(SysClusterHost host)  {
		//1.校验      有任务在执行，主机处于锁住状态，无法进行更新
		//dqy
		checkHostLock(host.getId());

		clusterHostDao.save(host);
		
		//初始化默认角色 ，如果存在默认角色更新角色状态
        SysClusterHostRole defaultRole = clusterHostRoleDao.findByCodeAndIpAndRoleCode(host.getId().getCode(),host.getId().getIp(), SysClusterRole.DEFAULT_ROLE);
        if (null==defaultRole){
            SysClusterHostRole hostRole = new SysClusterHostRole(host.getId().getCode(),host.getId().getIp(),SysClusterRole.DEFAULT_ROLE);
            clusterHostRoleDao.save(hostRole);
        }else{
            defaultRole.setStatus(host.getStatus());
            clusterHostRoleDao.save(defaultRole);
        }
	}

	@Override
	@Transactional
	public void deleteHost(String ip,String code)  {
		// 校验1： 主机是否有锁
		checkHostLock(new SysClusterHostKey(code,ip));

		clusterHostRoleDevDao.deleteByCodeAndIp(code,ip);
		clusterHostRoleDao.deleteByCodeAndIp(code,ip);
		clusterHostDao.deleteByIdCodeAndIdIp(code,ip);
	}

	@Override
	public List<SysClusterHost> findHosts(String code) {
		//1.获取SysClusterHost   主机信息
		List<SysClusterHost> hosts = clusterHostDao.findAllByIdCode(code);

		//2.根据主机列表获取   SysClusterHostRole  主机角色信息
		for(SysClusterHost host : hosts) {
            host.decodePassword();

			//查询主机所有的角色
			List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByCodeAndIp(code,host.getId().getIp());

            HashMap<String,Integer> devUsed = new HashMap<>();

            HashMap<String,Integer> devUnUsed = new HashMap<>();

            boolean installFlag = false;
            boolean powerFlag = false;
			//3.遍历所有角色
			for(SysClusterHostRole hostRole: hostRoles) {
				List<SysClusterHostRoleDev> devList = clusterHostRoleDevDao.findByHostRoleIdOrderByDevName(hostRole.getId());

				// 3.1 role == default  将设备添加到主机信息里
				if(SysClusterRole.DEFAULT_ROLE.equals(hostRole.getRoleCode())) {
					host.setDevs(devList);
				} else {
				    // 叠加设备使用量
                    for (SysClusterHostRoleDev dev:devList){
                        String devName = dev.getDevParent();
                        if (!devUsed.containsKey(devName)){
                            devUsed.put(devName,0);
                        }
                        if (!devUnUsed.containsKey(devName)){
                            devUnUsed.put(devName,0);
                        }

                        if (hostRole.isInstalled()){
                            devUsed.put(devName, devUsed.get(devName)+dev.getDevSize());
                        }else{
                            devUnUsed.put(devName, devUnUsed.get(devName)+dev.getDevSize());
                        }
                    }

					//3.2   查询角色对应的设备 添加到角色中
					hostRole.setDevs(devList);

                    // 判断主机启动状态
                    if (hostRole.isInstalled()){
                        installFlag = true;
                        if (!hostRole.getRunning()){
                            powerFlag = true;
                        }
                    }
				}

				// 设置主机启动状态
                host.setPower(installFlag?(powerFlag?SysClusterHost.POWER_OFF:SysClusterHost.POWER_OFF):SysClusterHost.POWER_CLOSE);

				host.addRole(hostRole);
			}

			// 重新分配已经使用掉的磁盘空间
			for (SysClusterHostRoleDev dev: host.getDevs()){
			    if (devUsed.containsKey(dev.getDevName())){
			        dev.accessAlloc(devUsed.get(dev.getDevName()));

			        if (devUnUsed.containsKey(dev.getDevName())){
                        dev.setUnUsedSize(devUnUsed.get(dev.getDevName()));
                    }
                }
            }
		}

        em.clear();
		return hosts;
	}

    @Override
    @Transactional
    public void saveTemplate(InputStream is,String code) throws IOException {
        //HSSFWorkbook代表整个Excle
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook(is);
        String[] cols = new String[]{"ip","username","password","sshPort","hasGpu"};

        //循环每一页，并处理当前页
        for(int sheetCur=0,sheetNum=hssfWorkbook.getNumberOfSheets(); sheetCur<sheetNum ; sheetCur++){
            HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(sheetCur); //获得HSSFSheet的某一页
            if(hssfSheet == null){
                continue;
            }
            //处理当前页,循环读取每一行
            for(int rowCur = 0,rowNum = hssfSheet.getLastRowNum(); rowCur<=rowNum; rowCur++){
                try{
                    if (rowCur==0){
                        continue;
                    }

                    HSSFRow hssfRow = hssfSheet.getRow(rowCur);  //HSSFRow表示行
                    int minCol = hssfRow.getFirstCellNum();
                    int maxCol = hssfRow.getLastCellNum();

                    JSONObject hostJson = new JSONObject();
                    //遍历每一行
                    for(int colCur = 0,width = maxCol-minCol; colCur<cols.length&&colCur<width; colCur++){
                        HSSFCell hssfCell = hssfRow.getCell(colCur+minCol);
                        if(hssfCell == null){
                            continue;
                        }
                        switch (cols[colCur]){
                            case "ip":
                                JSONObject id = new JSONObject();
                                id.put(cols[colCur],getCell(hssfCell));
                                hostJson.put("id",id);
                                break;
                            case "hasGpu":
                                Object hasGpu = getCell(hssfCell);
                                if (hasGpu!=null){
                                    String gpu = hasGpu.toString();
                                    hostJson.put(cols[colCur],!gpu.contains("不")&&!gpu.contains("false")&&!gpu.contains("否"));
                                }else{
                                    hostJson.put(cols[colCur],false);
                                }
                                break;
                            default:
                                hostJson.put(cols[colCur],getCell(hssfCell));
                        }
                    }
                    SysClusterHost host = JSON.toJavaObject(hostJson,SysClusterHost.class);
                    host.getId().setCode(code);
                    host.encodePassword();
                    saveHost(host);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    private static Object getCell(HSSFCell hssfCell){
        switch (hssfCell.getCellType()) {
        case Cell.CELL_TYPE_BOOLEAN:
            return hssfCell.getBooleanCellValue();
        case Cell.CELL_TYPE_FORMULA:
            return hssfCell.getCellFormula();
        case Cell.CELL_TYPE_NUMERIC:
            return hssfCell.getNumericCellValue();
        case Cell.CELL_TYPE_STRING:
            return hssfCell.getStringCellValue();
        default:
            return null;
        }
    }

    @Override
    public void handleData(String code) {
        try {
            List<SysClusterHost> hosts = findHosts(code);

            for (SysClusterHost host: hosts){

                // 如果主设备 devParent 未空，初始化一下
                for (SysClusterHostRoleDev masterDev: host.getDevs()){
                    if (StringUtils.isEmpty(masterDev.getDevParent())){
                        masterDev.setDevParent(masterDev.getDevName());
                        clusterHostRoleDevDao.save(masterDev);
                    }
                }

                // 如果主机上的其他设备 devParent 为空，初始化一下
                List<SysClusterHostRoleDev> hostDev = clusterHostRoleDevDao.findAllByCodeAndIp(code,host.getId().getIp());
                for (SysClusterHostRoleDev dev: hostDev){
                    if (!StringUtils.isEmpty(dev.getDevParent())){
                        continue;
                    }

                    for (SysClusterHostRoleDev masterDev : host.getDevs()){
                        // 找到分区对应的主设备，初始化一下设备信息
                        if (dev.getDevName().startsWith(masterDev.getDevName())){
                            dev.setDevParent(masterDev.getDevName());
                            break;
                        }
                    }

                    clusterHostRoleDevDao.save(dev);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
