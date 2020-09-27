package com.bonc.bcos.api.v1;

import com.bonc.bcos.common.ApiHandle;
import com.bonc.bcos.common.ApiResult;
import com.bonc.bcos.service.entity.SysInstallPlayExec;
import com.bonc.bcos.service.model.ExecProcess;
import com.bonc.bcos.service.service.ExecService;
import com.bonc.bcos.service.tasks.TaskManager;
import com.bonc.bcos.sys.SessionCache;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/exec")
public class ExecController {
    private static final Logger LOG = LoggerFactory.getLogger(ExecController.class);

    private final ExecService execService;

    @Autowired
    public ExecController(ExecService execService) {
        this.execService = execService;
    }


    /**
     * 任务初始化接口，服务重启之后会调用任务状态回退，主机锁失效
     */
    @RequestMapping(value = { "/reset" },method = RequestMethod.GET)
    @ApiOperation(value = "状态初始化接口", notes = "状态初始化接口")
    public ApiResult reset() {
        return ApiHandle.handle(() -> {
            execService.reset();
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 任务执行接口
     */
    @RequestMapping(value = { "/{playCode}" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务执行接口", notes = "任务执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "targets",value = "目标主机，如果为空视为所有主机",required = true),
            @ApiImplicitParam(name = "playCode",value = "任务编码")})
    public ApiResult exec(@RequestBody List<String> targets, @PathVariable String playCode, HttpSession session) {
        return ApiHandle.handle(() -> {
            ExecProcess task = execService.exec(targets,playCode,SessionCache.getCode(session));
            TaskManager.start(task.getTaskId());
            return task;
        },LOG);
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "/resume" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务继续执行接口", notes = "任务继续执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult resume(@RequestBody String uuid,HttpSession session) {
        return ApiHandle.handle(() -> {
            execService.resume(uuid, SessionCache.getCode(session));
            TaskManager.start(uuid);
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "/pause" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务暂停接口", notes = "任务暂停接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult pause(@RequestBody String uuid,HttpSession session) {
        return ApiHandle.handle(() -> {
            execService.pause(uuid,SessionCache.getCode(session));
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 停止任务
     */
    @RequestMapping(value = { "/close" }, method = RequestMethod.POST)
    @ApiOperation(value = "停止任务", notes = "停止任务")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult close(@RequestParam String uuid,HttpSession session) {
        return ApiHandle.handle(() -> {
            execService.stop(uuid,SessionCache.getCode(session));
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 查询所有的任务列表
     */
    @RequestMapping(value = { "/tasks" },method = RequestMethod.GET)
    @ApiOperation(value = "查询所有的任务列表", notes = "查询所有的任务列表")
    public ApiResult tasks() {
        return ApiHandle.handle(TaskManager::tasks,LOG);
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "/task" },method = RequestMethod.GET)
    @ApiOperation(value = "任务ID查询接口", notes = "任务ID查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "任务编码",required = true)})
    public ApiResult task(@RequestParam String playCode,HttpSession session) {
        return ApiHandle.handle(() -> execService.getLatestTask(playCode,SessionCache.getCode(session)),playCode,LOG);
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "/query" },method = RequestMethod.GET)
    @ApiOperation(value = "任务执行查询接口", notes = "任务执行查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult query(@RequestParam String uuid,HttpSession session) {
        return ApiHandle.handle(() -> execService.query(uuid,SessionCache.getCode(session)),uuid,LOG);
    }

    /**
     * 初始化playbook查询接口
     */
    @RequestMapping(value = { "/playbooks" }, method = RequestMethod.GET)
    @ApiOperation(value = "playbook查询接口", notes = "查询指定任务下的playbooks列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "业务编码",required = true)})
    public ApiResult getPlaybooks(@RequestParam String playCode) {
        return ApiHandle.handle(() -> execService.initPlaybooks(playCode),LOG);
    }

    /**
     * 查询通用化的任务工具
     */
    @RequestMapping(value = {"/plays"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询通用化的任务工具", notes = "查询通用化的任务工具")
    public ApiResult getTools() {
        return ApiHandle.handle(execService::findPlays);
    }

    /**
     * 查询全量日志
     */
    @RequestMapping(value = {"/logs"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询全量日志", notes = "查询全量日志")
    public ApiResult findLogs(HttpSession session) {
        return ApiHandle.handle(()->execService.findLogs(SessionCache.getCode(session)));
    }

    /**
     * 下载日志文件
     *
     *  注： 此处controller 返回值必须是void 类型，否则 由于service 层已经向 连接提交，将导致异常
     */
    @RequestMapping(value = {"/logs/{uuid}"}, method = RequestMethod.GET)
    @ApiOperation(value = "下载日志文件", notes = "下载日志文件")
    public void downloadLog(HttpSession session, HttpServletResponse res, @PathVariable String uuid) {
        ApiHandle.handle(() -> execService.downLog(res,uuid,SessionCache.getCode(session)),LOG);
    }

    /**
     * 修改任务状态
     *
     */
    @RequestMapping(value = {"/logs"}, method = RequestMethod.POST)
    @ApiOperation(value = "修改任务状态", notes = "修改任务状态")
    public ApiResult editTask(HttpSession session, SysInstallPlayExec exec) {
        return ApiHandle.handle(() -> execService.editTask(exec,SessionCache.getCode(session)),LOG);
    }

    /**
     * 删除任务
     *
     *  注： 此处controller 返回值必须是void 类型，否则 由于service 层已经向 连接提交，将导致异常
     */
    @RequestMapping(value = {"/logs/{uuid}"}, method = RequestMethod.DELETE)
    @ApiOperation(value = "删除任务", notes = "删除任务")
    public ApiResult deleteLog(@PathVariable String uuid) {
        return ApiHandle.handle(() -> execService.deleteTask(uuid),LOG);
    }

    /**
     * 查询任务状态
     *
     *  注： 此处controller 返回值必须是void 类型，否则 由于service 层已经向 连接提交，将导致异常
     */
    @GetMapping("/task/status")
    @ApiOperation(value = "查询任务状态", notes = "查询任务状态")
    public ApiResult taskStatus() {
        return ApiHandle.handle(SysInstallPlayExec::taskStatus,LOG);
    }
}
