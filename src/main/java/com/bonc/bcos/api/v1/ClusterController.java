package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.entity.SysClusterHost;
import com.bonc.bcos.service.service.ClusterService;
import com.bonc.bcos.service.service.HostService;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class ClusterController {

    private final HostService hostService;
    private final ClusterService clusterService;

    @Autowired
    public ClusterController(HostService hostService, ClusterService clusterService) {
        this.hostService = hostService;
        this.clusterService = clusterService;
    }

    /**
     * 主机保存接口
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.POST)
    @ApiOperation(value = "主机保存接口", notes = "主机保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "host", value = "主机信息"))
    public ApiResult saveHost(@RequestBody @Validated SysClusterHost host, HttpSession session) {
        return ApiHandle.handle(() -> {
            host.getId().setCode(SessionCache.getCode(session));
            hostService.saveHost(host.encodePassword());
            return new ArrayList<>();
        },host);
    }

    /**
     * 添加删除接口
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.DELETE)
    @ApiOperation(value = "主机删除接口", notes = "主机删除接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "ip", value = "主机IP"))
    public ApiResult delHost(@RequestBody String ip,HttpSession session) {
        return ApiHandle.handle(() -> {
            hostService.deleteHost(ip,SessionCache.getCode(session));
            return new ArrayList<>();
        },ip);
    }

    /**
     * 主机查询接口hostRole
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.GET)
    @ApiOperation(value = "主机查询接口", notes = "主机查询接口")
    public ApiResult findHost(HttpSession session) {
        return ApiHandle.handle(() -> hostService.findHosts(SessionCache.getCode(session)));
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = {"/store"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询存储配置信息", notes = "查询存储配置信息")
    public ApiResult store() {
        return ApiHandle.handle(clusterService::storeCfg);
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = {"/roles_cfg"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询角色配置", notes = "查询角色配置")
    public ApiResult getRoles() {
        return ApiHandle.handle(clusterService::roleCfg);
    }

    /**
     * 角色保存接口
     */
    @RequestMapping(value = {"/roles"}, method = RequestMethod.POST)
    @ApiOperation(value = "角色保存接口", notes = "角色保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "hosts", value = "主机角色信息"))
    public ApiResult saveRoles(@RequestBody List<SysClusterHost> hosts,HttpSession session) {
        return ApiHandle.handle(() -> {
            clusterService.saveRoles(hosts,SessionCache.getCode(session));
            return new ArrayList<>();
        },hosts);
    }

    /**
     * 角色查询接口
     */
    @GetMapping(value = {"/roles"})
    @ApiOperation(value = "角色查询接口", notes = "角色查询接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "ip", value = "角色查询接口"))
    public ApiResult getRoles(@RequestParam String ip,HttpSession session) {
        return ApiHandle.handle(() -> clusterService.getRoles(ip,SessionCache.getCode(session)),ip);
    }


    /**
     * 全局配置保存接口
     */
    @RequestMapping(value = {"/global"}, method = RequestMethod.POST)
    @ApiOperation(value = "全局配置保存接口", notes = "全局配置保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "global", value = "全局配置参数"))
    public ApiResult saveGlobal(@RequestBody HashMap<String, String> global,HttpSession session) {
        return ApiHandle.handle(() -> {
            clusterService.saveGlobal(global, SessionCache.getCode(session));
            return new ArrayList<>();
        },global);
    }

    /**
     * 全局配置查询接口
     */
    @RequestMapping(value = {"/global"}, method = RequestMethod.GET)
    @ApiOperation(value = "全局配置查询接口", notes = "全局配置查询接口")
    public ApiResult findGlobal(HttpSession session) {
        return ApiHandle.handle(()->clusterService.findGlobal(SessionCache.getCode(session)));
    }

    /**
     * 上传文件接口
     *
     * @param template 文件模板数据流
     * @return 解析结果
     */
    @RequestMapping(value = {"/upload"}, method = RequestMethod.POST)
    public ApiResult upload(@RequestParam("template") MultipartFile template,HttpSession session) {
        return ApiHandle.handle(() -> {
            hostService.saveTemplate(template.getInputStream(),SessionCache.getCode(session));
            return new ArrayList<>();
        });
    }

    /**
     * 角色推荐接口
     */
    @RequestMapping(value = {"/policy"}, method = RequestMethod.POST)
    @ApiOperation(value = "查询角色策略", notes = "角色查询接口")
    public ApiResult rolePolicy(@RequestBody List<SysClusterHost> hosts,HttpSession session) {
        return ApiHandle.handle(() -> clusterService.rolePolicy(hosts,SessionCache.getCode(session)));
    }

    /**
     * 角色推荐接口
     */
    @RequestMapping(value = {"/image"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询版本镜像", notes = "查询版本镜像")
    public ApiResult image(@RequestParam String version) {
        return ApiHandle.handle(() -> clusterService.findImage(version));
    }

    /**
     * 角色推荐接口
     */
    @RequestMapping(value = {"/dict"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询版本镜像", notes = "查询版本镜像")
    public ApiResult dict(HttpSession session,@RequestParam String dictName) {
        return ApiHandle.handle(() -> session.getAttribute(dictName));
    }
}
