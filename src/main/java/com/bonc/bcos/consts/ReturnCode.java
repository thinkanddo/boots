/**
 *
 */
package com.bonc.bcos.consts;

/**
 * @author ruzz
 *
 */
public class ReturnCode {

    public static final int CODE_SUCCESS = 200; // 接口正常返回码

    // JSON转换异常，等其他形式异常， 范围：500-599

    // 公共错误（校验错误）， 范围 ：600-699
    public static final int CODE_CHECK_PARAM_IS_NULL = 600; // ***为空
    public static final int CODE_CHECK_PARAM_IS_NOT_FORMAT = 601; // ***不符合规范
    public static final int CODE_CHECK_PARAM_IS_NOT_EXIST = 602; // ***不存在
    public static final int CODE_CHECK_PARAM_CPU_LESSTHAN_USED = 603; // 修改CPU总配额小于等于已使用配额
    public static final int CODE_CHECK_PARAM_MEMORY_LESSTHAN_USED = 604; // 修改Memory总配额小于等于已使用配额
    public static final int CODE_CHECK_PARAM_STORAGE_LESSTHAN_USED = 605; // 修改Storage总配额小于等于已使用配额
    public static final int CODE_CHECK_PARAM_IS_EXIST = 606; // ***已存在
    public static final int CODE_CHECK_PARAM_NOT_UPDATE = 607; // ***不能修改
    public static final int CODE_CHECK_PARAM_DELETE_FAILED = 608; // ***删除失败
    public static final int CODE_CHECK_PARAM_QUOTA_INSUFFICIENT = 609; // CPU或Memory资源不足
    public static final int CODE_CHECK_PARAM_CONNECT_FTP_FAILED = 610; // 链接FTP失败

    // SQL异常 ，范围：700-799
    public static final int CODE_SQL_CONNECT_FAILED = 700; // 数据库连接失败
    public static final int CODE_SQL_FIND_ONE_RETURN_MORE_FAILED = 701; // 查询单条数据，多条数据返回错误
    public static final int CODE_SQL_SAVE_INFO_FAILED = 702; // 保存信息失败
    public static final int CODE_SQL_FIND_LIST_FAILED = 703; // 查询数据列表失败
    public static final int CODE_SQL_FIND_ONE_FAILED = 704; // 查询单条数据失败
    public static final int CODE_SQL_DELETE_FAILED = 705; // 删除失败

    // 操作限制异常，范围：1200-1299
    public static final int CODE_OPT_SERVICE_NOT_ALLOWED_FAILED = 1201; // 不允许进行服务操作
    public static final int CODE_OPT_NODE_NOT_ALLOWED_FAILED = 1202; // 不允许进行节点操作
    public static final int CODE_OPT_CRONJOBE_NOT_ALLOWED_FAILED = 1203; // 不允许进行定时任务操作
    public static final int CODE_OPT_OVERTIME_FAILED = 1204; // 操作超时
    public static final int CODE_OPT_SERVICEAFFINITY_MUTUAL = 1205; // 不允许两服务之间同时存在亲和与反亲和的操作
    public static final int CODE_OPT_POD_PODMUTEX = 1206; // 当前操作不满足pod互斥的前提条件
    public static final int CODE_OPT_CROSS_TENANT_NOT_ALLOWED = 1207; // 当前资源对象或行为动作不允许跨租户操作

    // 集群管理异常编码 1500-1599
    public static final int CODE_CLUSTER_HOST_CHECK = 1501; // 主机已经被锁住
    public static final int CODE_CLUSTER_ROLE_INSTALLED = 1502; // 主机上有安装的角色
    public static final int CODE_CLUSTER_DEV_NOT_EXIST = 1503; // 设备ID不存在
    public static final int CODE_CLUSTER_PLAY_RUNNING = 1504; // 存在任务正在安装
    public static final int CODE_CLUSTER_PARAM_IS_EMPTY = 1505; // 请求参数为空
    public static final int CODE_CLUSTER_PLAY_NOT_EXIST = 1506; // 任务不存在
    public static final int CODE_CLUSTER_PLAY_CANT_EXEC = 1507; // 任务不具备执行条件
    public static final int CODE_CLUSTER_BOOTSTRAP_CALL_FAIL = 1508; // 执行任务调度失败
    public static final int CODE_CLUSTER_TASK_NOT_EXIST = 1509; // 任务不存在
    public static final int CODE_CLUSTER_TASK_NOT_FAILED = 1510; // 任务状态不是failed
    public static final int CODE_CLUSTER_TASK_TOKEN_ERROR = 1511; // 任务token错误
    public static final int CODE_CLUSTER_TASK_ERROR_STATUS = 1512; // 任务错误状态
    public static final int CODE_CLUSTER_DEV_NOT_ENOUGH = 1513; // 设备空间不足
    public static final int CODE_CLUSTER_LOG_ERROR_STATUS = 1514; // 日志状态错误
    public static final int CODE_CLUSTER_LOG_NOE_EXIST = 1515; // 日志不存在
    public static final int CODE_CLUSTER_HOST_INV_FAILED = 1516; // 初始化主机host文件失败
    public static final int CODE_CLUSTER_HOST_DIR_FAILED = 1517; // 初始化主机hosts文件夹失败
    public static final int CODE_CLUSTER_DEV_IN_USERD = 1518; // 设备已被使用
    public static final int CODE_CLUSTER_SYSTEM_ERROR = 1519; // 系统错误
    public static final int CODE_CLUSTER_PLAY_NOT_RUNNING = 1520; // 当前任务未在运行中
    public static final int CODE_CLUSTER_HOST_DIR_NOT_EXISTED= 1521; // 主机已经被锁住
    public static final int CODE_DATA_CLONE_ERROR= 1522; // 数据clone 出现异常
    public static final int CODE_TASK_EXEC_FAILED= 1523; // 数据clone 出现异常
    public static final int CODE_TASK_HOST_LICENSE= 1524; // 数据clone 出现异常

    public static final int CODE_ENV_EXISTED= 1600; // 环境已经存在
    public static final int CODE_ENV_NOT_EXIST= 1601; // 环境不存在
    public static final int CODE_ENV_HOST_EXISTED= 1602; // 环境下面还存在主机，不能清理
    public static final int CODE_ENV_PASSWORD_ERROR= 1603; // 环境密码不对
    public static final int CODE_ENV_NOT_LOGIN= 1604; // 环境不存在
    public static final int CODE_ENV_NOT_MATCH= 1605; // 主机与登陆环境不匹配
    public static final int CODE_ENV_VERSION_MATCH= 1606; // 主机与登陆环境不匹配
    public static final int CODE_ENV_LICENSE_RESOLVE= 1607; // 环境的LICENSE 解析失败
    public static final int CODE_ENV_LICENSE_MATCH= 1608; // 环境的LICENSE 匹配失败


    public static final int CODE_GLOBAL_CFG_NOT_EXIST = 1700;  // 全局配置不存在

    public static final int CODE_FTP_CONNECT_EXCEPTION = 1800;  // FTP 链接失败
    public static final int CODE_FTP_LOGIN_EXCEPTION = 1801;  // FTP 上传失败
    public static final int CODE_FTP_UPLOAD_EXCEPTION = 1802;  // FTP 上传失败

    public static final int CODE_CMD_EXEC_ERROR= 1900;  // cmd 命令执行器，执行失败
}
