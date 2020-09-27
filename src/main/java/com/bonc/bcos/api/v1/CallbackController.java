package com.bonc.bcos.api.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.entity.*;
import com.bonc.bcos.service.service.CallService;
import com.bonc.bcos.service.service.VersionService;
import com.bonc.bcos.sys.Global;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/v1/callback")
public class CallbackController {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackController.class);

    private final CallService callService;
    private final VersionService versionService;

    @Autowired
    public CallbackController(CallService callService, VersionService versionService) {
        this.callService = callService;
        this.versionService = versionService;
    }

    /**
     * 用户 执行脚本curl回调主机信息，保存主机的校验状态
     *
     * @param host 主机信息，里面可以有设备信息
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存主机信息", notes = "回调保存主机信息")
    public ApiResult callbackHost(@RequestParam String host) {
        return ApiHandle.handle(() -> {
            callService.saveHost(JSON.parseObject(host, SysClusterHost.class).encodePassword());
            return new ArrayList<>();
        }, host,LOG);
    }

    @RequestMapping(value = {"/role"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存角色信息", notes = "回调保存角色信息")
    public ApiResult callbackRole(@RequestParam String hostRole) {
        return ApiHandle.handle(() -> {
            callService.saveHostRole(JSON.parseObject(hostRole, SysClusterHostRole.class));
            return new ArrayList<>();
        }, hostRole,LOG);
    }

    @RequestMapping(value = {"/dev"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存设备信息", notes = "回调保存设备信息")
    public ApiResult callbackDev(@RequestParam String roleDev) {
        return ApiHandle.handle(() -> {
            callService.saveRoleDev(JSON.parseObject(roleDev, SysClusterHostRoleDev.class));
            return new ArrayList<>();
        }, roleDev,LOG);
    }

    @RequestMapping(value = {"/global"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存全局信息", notes = "回调保存全局信息")
    public ApiResult callbackGlobal(@RequestParam String global, @RequestHeader String code) {
        return ApiHandle.handle(() -> {
            JSONObject json = JSON.parseObject(global);
            HashMap<String, String> map = new HashMap<>();
            if (null != json) {
                for (String key : json.keySet()) {
                    map.put(key, json.get(key).toString());
                }
            }
            map.put(Global.SYSTEM_ENV_CODE,code);
            callService.saveGlobal(map);
            return new ArrayList<>();
        },global, LOG);
    }

    @RequestMapping(value = {"/tags"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存设备信息", notes = "回调保存设备信息")
    public ApiResult saveTags(@RequestParam String tags) {
        return ApiHandle.handle(() -> {
            callService.saveTags(JSON.parseArray(tags, String.class));
            return new ArrayList<>();
        }, tags,LOG);
    }

    /**
     * pack  回调安装/升级包信息
     *
     */
    @RequestMapping(value = {"/pack"}, method = RequestMethod.POST)
    @ApiOperation(value = "安装包回调", notes = "回调tag信息")
    public ApiResult callbackPack(@RequestParam String pack) {
        return ApiHandle.handle(() -> {
            versionService.saveVersion(JSON.parseObject(pack, SysClusterVersion.class));
            return new ArrayList<>();
        }, pack,LOG);
    }

    /**
     * upgrade  回调升级信息
     *
     */
    @RequestMapping(value = {"/env"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调升级env信息", notes = "回调升级env信息")
    public ApiResult callbackUpgrade(@RequestParam String tag, @RequestHeader String code) {
        return ApiHandle.handle(() -> {
            callService.saveEnv(code,tag);
            return new ArrayList<>();
        });
    }

    /**
     * pack  安装镜像信息
     */
    @RequestMapping(value = {"/image"}, method = RequestMethod.POST)
    @ApiOperation(value = "安装镜像信息", notes = "安装镜像信息")
    public ApiResult callbackImage(@RequestParam String image,@RequestHeader String version) {
        return ApiHandle.handle(() -> {
            callService.saveImage(JSON.parseArray(image, String.class),version);
            return new ArrayList<>();
        }, image,LOG);
    }
}
