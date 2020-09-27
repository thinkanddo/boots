package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.entity.SysClusterHostRoleDev;
import com.bonc.bcos.service.service.DevService;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/v1/dev")
public class DevController {
    private static final Logger LOG = LoggerFactory.getLogger(DevController.class);

    private final DevService devService;

    @Autowired
    public DevController(DevService devService) {
        this.devService = devService;
    }

    @GetMapping
    @ApiOperation(value = "设备查询接口", notes = "设备查询接口")
    public ApiResult findDev(@RequestParam String ip,String roleId, HttpSession session) {
        return ApiHandle.handle(() -> devService.findDev(SessionCache.getCode(session),ip,roleId),LOG);
    }

    @PostMapping
    @ApiOperation(value = "设备保存接口", notes = "设备保存接口")
    public ApiResult saveDev(SysClusterHostRoleDev dev, HttpSession session) {
        return ApiHandle.handle(() -> devService.saveDev(SessionCache.getCode(session),dev),LOG);
    }

    @DeleteMapping
    @ApiOperation(value = "设备删除接口", notes = "设备删除接口")
    public ApiResult delDev(SysClusterHostRoleDev dev, HttpSession session) {
        return ApiHandle.handle(() -> devService.delDev(SessionCache.getCode(session),dev),LOG);
    }

    /**
     * 修改设备状态接口
     */
    @RequestMapping(value = { "/devchange" }, method = RequestMethod.POST)
    @ApiOperation(value = "修改设备状态接口", notes = "修改设备状态接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "changeDev",value = "修改设备状态接口",required = true)})
    public ApiResult devChange(@RequestBody SysClusterHostRoleDev changeDev) {
        return ApiHandle.handle(() -> {
            devService.changeDevStatus(changeDev);
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 设备计算接口
     */
    @RequestMapping(value = { "/allocate" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备计算接口", notes = "设备计算接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "targets",value = "目标主机（必须已经校验通过）"))
    public ApiResult allocateDev(@RequestBody Set<String> targets, HttpSession session) {
        return ApiHandle.handle(() -> {
            devService.allocate(targets,SessionCache.getCode(session));
            return new ArrayList<>();
        },LOG);
    }
}
