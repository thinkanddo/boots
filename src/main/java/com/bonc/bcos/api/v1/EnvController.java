package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.entity.SysClusterEnv;
import com.bonc.bcos.service.service.EnvService;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;

@RestController
@RequestMapping("/v1/env")
public class EnvController {
    private final EnvService envService;

    public EnvController(EnvService envService) {
        this.envService = envService;
    }

    /**
     * 环境保存接口
     */
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "环境保存接口", notes = "环境保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "env", value = "环境信息"))
    public ApiResult saveEnv(@RequestBody @Validated SysClusterEnv env) {
        return ApiHandle.handle(() -> envService.saveEnv(env),env);
    }

    /**
     * 添加删除接口
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ApiOperation(value = "环境删除接口", notes = "环境删除接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "code", value = "环境IP"))
    public ApiResult delEnv(@RequestBody SysClusterEnv env) {
        return ApiHandle.handle(() -> {
            envService.deleteEnv(env);
            return new ArrayList<>();
        },env);
    }

    /**
     * 环境查询接口
     */
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "环境查询接口", notes = "环境查询接口")
    public ApiResult findEnv() {
        return ApiHandle.handle(envService::findEnv);
    }

    /**
     * 环境查询接口
     */
    @RequestMapping(value = "/type",method = RequestMethod.GET)
    @ApiOperation(value = "环境查询接口", notes = "环境查询接口")
    public ApiResult env() {
        return ApiHandle.handle(envService::checkEnv);
    }

    /**
     * 进入进群环境
     */
    @RequestMapping(value = "/entry",method = RequestMethod.POST)
    @ApiOperation(value = "进入进群环境", notes = "进入进群环境")
    public ApiResult entryEnv(@RequestBody SysClusterEnv env, HttpSession session) {
        return ApiHandle.handle(() -> {
            Boolean entry = envService.entryEnv(env);
            if (entry){
                SessionCache.initSession(session,env.getCode());
            }
            return entry;
        },env);
    }

    /**
     * 判断是否登陆环境
     */
    @RequestMapping(value = "/entry/{code}",method = RequestMethod.GET)
    @ApiOperation(value = "进入进群环境", notes = "进入进群环境")
    public ApiResult entryEnv(@PathVariable String code, HttpSession session) {
        return ApiHandle.handle(() -> code.equals(SessionCache.getCode(session)),code);
    }

    /**
     * 退出进群环境
     */
    @RequestMapping(value = "exit",method = RequestMethod.GET)
    @ApiOperation(value = "进入进群环境", notes = "进入进群环境")
    public ApiResult exitEnv(HttpSession session) {
        return ApiHandle.handle(() -> {
            SessionCache.removeSession(session);
            return new ArrayList<>();
        },SessionCache.getCode(session));
    }

    /**
     * 退出进群环境
     */
    @RequestMapping(value = "license",method = RequestMethod.POST)
    @ApiOperation(value = "检查license", notes = "检查license")
    public ApiResult checkLicense(@RequestBody SysClusterEnv env) {
        return ApiHandle.handle(() -> envService.checkLicense(env),env);
    }

    /**
     * 退出进群环境
     */
    @RequestMapping(value = "license",method = RequestMethod.GET)
    @ApiOperation(value = "", notes = "进入进群环境")
    public ApiResult license(HttpSession session) {
        return ApiHandle.handle(() -> envService.getLicense((String) SessionCache.getCode(session)),"");
    }

    /**
     * 获取faq markdown 文件内容
     */
    @GetMapping(value ="faqs/**//{fileName}")
    @ApiOperation(value = "", notes = "查询faqs内容")
    public void license(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        envService.getFaqs(URLDecoder.decode(request.getRequestURI(),"utf-8").split("faqs")[1],resp);
    }
}
