package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.model.CmdK8SPo;
import com.bonc.bcos.service.service.CmdService;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/v1/cmd")
public class CmdController {

    private final CmdService cmdService;

    @Autowired
    public CmdController(CmdService cmdService) {
        this.cmdService = cmdService;
    }

    /**
     * 查询k8s yaml 资源
     */
    @GetMapping("/k8s/yaml")
    @ApiOperation(value = "查询k8s yaml 资源", notes = "查询k8s yaml 资源")
    @ApiImplicitParams(@ApiImplicitParam(name = "po", value = "k8s查询参数"))
    public ApiResult yaml(HttpSession session,CmdK8SPo po) {
        return ApiHandle.handle(() -> cmdService.k8sYaml(po,SessionCache.getCode(session)),po);
    }

    /**
     * describe k8s yaml 资源
     */
    @GetMapping("/k8s/detail")
    @ApiOperation(value = "describe k8s ", notes = "describe k8s ")
    @ApiImplicitParams(@ApiImplicitParam(name = "po", value = "k8s查询参数"))
    public ApiResult detail(HttpSession session, CmdK8SPo po) {
        return ApiHandle.handle(() -> cmdService.k8sDetail(po,SessionCache.getCode(session)),po);
    }

    /**
     * describe k8s 日志 资源
     */
    @GetMapping("/k8s/logs")
    @ApiOperation(value = "logs k8s ", notes = "logs k8s ")
    @ApiImplicitParams(@ApiImplicitParam(name = "po", value = "k8s查询日志"))
    public ApiResult logs(HttpSession session, CmdK8SPo po) {
        return ApiHandle.handle(() -> cmdService.k8sLogs(po,SessionCache.getCode(session)),po);
    }

    /**
     * 查询k8s 资源列表
     */
    @GetMapping("/k8s/resource")
    @ApiOperation(value = "查询k8s 资源列表", notes = "查询k8s 资源列表")
    @ApiImplicitParams(@ApiImplicitParam(name = "po", value = "k8s查询参数"))
    public ApiResult resource(HttpSession session,CmdK8SPo po) {
        return ApiHandle.handle(() -> cmdService.k8sTable(po, SessionCache.getCode(session)),po);
    }

    /**
     * 删除k8s 资源信息
     */
    @DeleteMapping("/k8s/resource")
    @ApiOperation(value = "删除k8s 资源信息", notes = "删除k8s 资源信息")
    @ApiImplicitParams(@ApiImplicitParam(name = "po", value = "k8s查询参数"))
    public ApiResult delResource(HttpSession session,CmdK8SPo po) {
        return ApiHandle.handle(() -> cmdService.k8sDelete(po, SessionCache.getCode(session)),po);
    }


}
