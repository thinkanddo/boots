package com.bonc.bcos.service.tasks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.bonc.bcos.consts.ReturnCode;
import com.bonc.bcos.service.entity.SysInstallLogLabel;
import com.bonc.bcos.service.entity.SysInstallPlayExec;
import com.bonc.bcos.service.entity.SysInstallPlaybook;
import com.bonc.bcos.service.exception.ClusterException;
import com.bonc.bcos.service.service.CallService;
import com.bonc.bcos.sys.Global;
import com.bonc.bcos.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 主要负责调用OS CMD任务的执行
 */
public class PlaybookExecutor extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(PlaybookExecutor.class);

    private static final String SHELL_NAME = "/bin/bash";
    private static final String SHELL_PARAM = "-c";

    private static CallService CALL_BACK;

    // 缓存标签
    private static final HashMap<String, List<SysInstallLogLabel>> LABEL_CACHE = new HashMap<>();
    private static final List<SysInstallLogLabel> DEFAULT_LABEL = new ArrayList<>();

    public static void init(CallService callback, List<SysInstallLogLabel> labelList, List<SysInstallPlaybook> playbooks) {
        // 设置回调类
        PlaybookExecutor.CALL_BACK = callback;

        // 初始化playbook 标签空缓存
        for (SysInstallPlaybook playbook : playbooks) {
            if (!PlaybookExecutor.LABEL_CACHE.containsKey(playbook.getFilename())) {
                PlaybookExecutor.LABEL_CACHE.put(playbook.getFilename(), new ArrayList<>());
            }
        }

        // 加载标签关系
        for (SysInstallLogLabel label : labelList) {
            label.setLabelRegex(label.getLabelRegex());
            label.setGroupOrder(label.getGroupOrder());

            // 有限加载默认标签
            if (SysInstallLogLabel.DEFAULT_PLAYBOOK.equals(label.getPlaybook())) {
                DEFAULT_LABEL.add(label);
            } else if (LABEL_CACHE.containsKey(label.getPlaybook())) {
                LABEL_CACHE.get(label.getPlaybook()).add(label);
            }
        }
    }

    private final SysInstallPlayExec exec;
    private final String execPath;
    private final StringBuffer buffer;
    private final List<String> cmdList = new ArrayList<>();
    private final List<String> message = new ArrayList<>();
    private Process process;
    private boolean destroy = false;

    // 记录执行时间
    private Long execTime;

    PlaybookExecutor(SysInstallPlayExec exec) {
        this.exec = exec;
        this.execPath = Global.getAnsibleDir(exec.getCode());
        this.buffer = new StringBuffer(exec.getStdout());
        if (exec.getCurIndex() == 0) {
            handleLine(null, "开始处理主任务： " + exec.getPlayName() + "  任务ID: " + exec.getUuid());
        }
        if (null != exec.getCmd()) {
            this.cmdList.add(exec.getCmd());
        }
    }

    /**
     * 将命令集合中的参数转化为OS Runtime中的参数
     *
     * @return 返回Runtime调用参数列表
     */
    private List<String> build(String cmd) {
        return Arrays.asList(SHELL_NAME, SHELL_PARAM, cmd);
    }

    /**
     * 调用{@link ProcessBuilder#start()}执行命令
     */
    public void exec() throws IOException, InterruptedException {
        LOG.info("开始执行任务： {}", exec.getUuid());
        for (SysInstallPlaybook playbook : exec.getPlaybooks()) {

            // 继续执行的时候前面的playbook都跳过
            if (exec.getCurIndex() > playbook.getIndex()) {
                continue;
            }
            List<SysInstallLogLabel> playbookLabel = LABEL_CACHE.get(playbook.getFilename());
            handleLine(playbookLabel, "开始执行子任务模块： " + playbook.getPlaybookName());

            // 设置当前执行的playbook标识
            exec.setCurIndex(playbook.getIndex());
            LOG.info("当前任务索引: {}", exec.getCurIndex());

            HashMap<String, Object> param = new HashMap<>();
            param.put("boots_global", Global.getCfgMap(exec.getCode()));
            param.put("boots_roles", playbook.getGroups());

            // 根据任务生成playbook 的host文件
            String cmd = playbook.generateCmd(exec.getCode(), exec.getUuid(), JSON.toJSONString(param,SerializerFeature.DisableCircularReferenceDetect));

            LOG.info("执行命令: {}", cmd);
            cmdList.add(cmd);

            List<String> cmdList = build(cmd);

            // 构造进程执行器
            ProcessBuilder pb = new ProcessBuilder(cmdList.toArray(new String[0])).redirectErrorStream(true).directory(new File(execPath));

            // 启动进程
            process = pb.start();

            // 获取进程输出流stdout读取器
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            LOG.info("获取进程输出流stdout读取成功");
            // 监听标准输出
            String line;
            while ((line = br.readLine()) != null) {
                handleLine(playbookLabel, line);
            }

            // 等待进程执行结果
            int exitCode = process.waitFor();

            // 只有上一个playbook 执行成功才执行下一个playbook
            if (exitCode != 0) {
                // process.destroy();
                throw new ClusterException(ReturnCode.CODE_TASK_EXEC_FAILED, playbook.getPlaybookName() + " 执行失败!");
            }
        }
    }

    // 增加超时处理机制，太长时间不打印 处理就会超时
    private void handleLine(List<SysInstallLogLabel> playbookLabel, String line) {
        // playbook Handle
        boolean labelSuc = handleLabel(playbookLabel, line);

        // default Handle
        boolean defaultSuc = handleLabel(DEFAULT_LABEL, line);

        // 如果都处理失败，采用默认的处理方式
        if (!labelSuc && !defaultSuc) {
            defaultHandle(line);
        }
    }

    // 所有正则都没匹配上，采用默认处理方式
    private void defaultHandle(String line) {
        // 记录执行时间
        execTime = DateUtil.getCurrentTimeMillis();

        if (StringUtils.isEmpty(line)) {
            buffer.append("<br/>");
        } else {
            buffer.append("<div>[ ").append(DateUtil.getCurrentDateTime()).append(" ]  ").append(line.replace("\\r\\n", "<br/>").replace("\\n", "<br/>")).append("</div>");
        }
        buffer.append("\n");
    }

    /**
     * 根据标签配置信息 处理行信息，如果没有正则匹配则失败，如果又则成功
     *
     * @param labelList 标签处理列表
     * @param line      行数据
     * @return 是否对信息行进行处理处理
     */
    private boolean handleLabel(List<SysInstallLogLabel> labelList, String line) {
        boolean flag = false;

        if (null == labelList) {
            return false;
        }
        for (SysInstallLogLabel label : labelList) {
            try {
                Matcher m = label.getPattern().matcher(line);
                if (m.find()) {
                    String handle = label.getLabelHandle();
                    List<Integer> orders = label.getOrders();

                    Object[] args = new Object[orders.size()];
                    for (int i = 0, len = args.length; i < len; i++) {
                        try {
                            args[i] = m.group(label.getOrders().get(i));
                        } catch (Exception e) {
                            e.printStackTrace();
                            args[i] = "描述信息提取失败！";
                        }
                    }

                    switch (label.getLabelType()) {
                        // 处理行样式
                        case SysInstallLogLabel.STYLE:
                            buffer.append(String.format(handle, args).replace("\\r\\n", "<br/>").replace("\\n", "<br/>")).append("\n");
                            flag = true;
                            break;
                        // 处理异常提示信息
                        case SysInstallLogLabel.MESSAGE:
                            message.add(String.format(handle, args));
                            break;
                        // 处理展示进度
                        case SysInstallLogLabel.PERCENT:
                            exec.setPercent(Integer.parseInt(handle));
                            break;
                        default:
                            break;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return flag;
    }

    @Override
    public void run() {
        try {
            synchronized (exec) {
                // 设置开始执行的时间
                execTime = DateUtil.getCurrentTimeMillis();

                // 开始执行任务
                exec.setBeginDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
                CALL_BACK.start(exec);
                exec();
                // 设置成功
                exec.setStatus(SysInstallPlayExec.SUCCESS);
            }
        } catch (ClusterException e) {
            LOG.error("任务执行失败：{}", e.getMsg());
            exec.setStatus(SysInstallPlayExec.FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            exec.setStatus(SysInstallPlayExec.FAILED);
            for (String ip : exec.getTargetIps()) {
                HashMap<String,String> msg = new HashMap<>();
                msg.put("ip",ip);
                msg.put("message","系统异常");
                msg.put("code",exec.getCode());
                message.add(JSON.toJSONString(msg));
            }
        } finally {
            // 如果是被程序破坏的是设置保存信息
            if (!destroy) {
                exec.setCmd(String.join("\n", cmdList));
                exec.setStdout(buffer.toString());
                exec.setMessage(JSON.toJSONString(message));
                exec.setEndDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
                CALL_BACK.finish(exec);
            }
            TaskManager.remove(exec.getUuid(), exec.isFailed() ? "任务执行失败" : "任务执行完成");
        }
    }

    SysInstallPlayExec getTask() {
        exec.setStdout(buffer.toString());
        exec.setMessage(JSON.toJSONString(message));
        return exec;
    }

    /**
     * 实现任务终止逻辑
     */
    void destroyTask(char status, String message) {
        // 对可用终止的进程输出相应的处理原因
        defaultHandle(message);

        // 1. 暂停线程
        destroy = true;
        process.destroy();

        // 2.设置保存信息
        exec.setStatus(status);
        exec.setCmd(String.join("\n", cmdList));
        exec.setStdout(buffer.toString());
        exec.setMessage(JSON.toJSONString(message));
        exec.setEndDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
        CALL_BACK.finish(exec);

        // 3. 接数线程
        super.interrupt();
    }

    public boolean isTimeout() {
        return null != execTime && (DateUtil.getCurrentTimeMillis() - execTime) / 1000 > exec.getTimeout();
    }
}
