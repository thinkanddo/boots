package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.service.VersionService;
import com.bonc.bcos.service.tasks.FTPListener;
import com.bonc.bcos.service.tasks.FTPUtil;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@RestController
@RequestMapping("/v1/version")
public class VersionController {

    private static final Logger LOG = LoggerFactory.getLogger(VersionController.class);

    private final VersionService versionService;

    @Autowired
    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * 版本查询接口
     */
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "版本", notes = "版本查询")
    public ApiResult tags() {
        return ApiHandle.handle(Global::getTags,LOG);
    }

    /**
     * 上传升级包
     */
    @RequestMapping(value = {"/upload"},method = RequestMethod.POST)
    @ApiOperation(value = "升级包", notes = "上传升级包")
    public ApiResult upload(@RequestParam("package") MultipartFile template, HttpSession session) {
        return ApiHandle.handle(() -> {
            versionService.saveUploadPackage(template,session, SessionCache.getCode(session));
            return new ArrayList<>();
        });
    }

    /**
     * 获取上传进度
     */
    @RequestMapping(value = {"/upload"},method = RequestMethod.GET)
    @ApiOperation(value = "升级包上传进度", notes = "升级包进度")
    public ApiResult uploadProcess(HttpSession session) {
        return ApiHandle.handle(() -> {
            FTPListener listener = (FTPListener) session.getAttribute(FTPUtil.PACKAGE_LISTENER_KEY);
            if(null==listener){
                return null;
            }
            return listener.getProcess();
        });
    }

    /**
     * 取消上传
     */
    @RequestMapping(value = {"/upload"},method = RequestMethod.DELETE)
    @ApiOperation(value = "取消上传", notes = "取消上传")
    public ApiResult stopUpload(HttpSession session) {
        return ApiHandle.handle(() -> {
            FTPListener listener = (FTPListener) session.getAttribute(FTPUtil.PACKAGE_LISTENER_KEY);
            listener.stop();
            return true;
        });
    }

    /**
     * 查询包列表
     */
    @RequestMapping(value = {"/pack"},method = RequestMethod.GET)
    @ApiOperation(value = "查询包列表", notes = "查询包列表")
    public ApiResult pack() {
        return ApiHandle.handle(versionService::getPack);
    }

    /**
     * 删除包
     */
    @RequestMapping(value = {"/pack"},method = RequestMethod.DELETE)
    @ApiOperation(value = "删除包", notes = "删除包")
    public ApiResult delete(@RequestBody String packageName) {
        return ApiHandle.handle(()->{versionService.deletePack(packageName);return null;});
    }
}
