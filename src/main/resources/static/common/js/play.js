/**
 *  play 公共的play任务处理类
 * @param play_code  任务编码
 * @param opt  操作对象
 *      read_last = true/false  是否读取上次的任务ID
 * @constructor
 */

var plays = {};

$.ajax({
    url:  "v1/exec/plays",
    async: false,
    success: function (data) {
        if (data.code===200){
            data.data.forEach(function (play) {
                plays[play.playCode] = play;
            });
        }else{
            console.log("全局工具列表失败！");
        }
    }
});

var Play = function (play_code,opt) {
    this.create_date = undefined;

    Play.OPT_DEFAULTS = {
        read_last:false,
        interval_time: 1000,
        // 启动的回掉函数
        start: function(play,data){
            console.log(play.uuid());
        },
        // 暂停或者恢复后的回掉函数
        resume: function(play,data){
            console.log(play.uuid());
        },
        // 日志输出的回掉函数
        callback: function (play,data) {
            console.log(play.uuid());
        }
    };

    //  获取任务ID
    var task_name = play_code;
    var task_status = false;
    var _this = this;

    // 生成 最终的操作选项
    const _opt = $.extend({}, Play.OPT_DEFAULTS, opt);

    // 创建静态缓存
    var playbooks = [];

    var task_id = "";
    var interval;

    // play 当前的playbook 下标
    var cur_index=0;

    var init = function () {
        if (_opt.read_last){
            $.ajax({
                type : "get",
                url :   "v1/exec/task?playCode="+task_name,
                async: false,
                success : function(result) {
                    if(result.code===200&&result.data!==null){
                        var task = result.data;
                        _this.create_date = task.createDate;
                        task_id = task.taskId;
                        _this.status(true);
                        _opt.start(_this,task);
                    }else{
                        _this.status(false);
                    }
                }
            });
        }

        // 加载playbooks 执行步骤清单
        $.ajax({
            type : "get",
            url :   "v1/exec/playbooks?playCode="+task_name,
            async: false,
            success : function(result) {
                playbooks = result.data;
            }
        });

        interval = setInterval(get_log,_opt.interval_time);
        return _this;
    };

    var get_log = function(){
        if(!task_status||task_id===""){
            return;
        }

        $.ajax({
            type : "get",
            url :   "v1/exec/query?uuid="+task_id,
            contentType:'application/json',
            async: false,
            success : function(result) {
                if(result.code===200){
                    _this.status(result.data.status==='1'||result.data.status==='0');
                    _opt.callback(_this,result);
                    // 当执行下一个playbook 或者 任务结束的时候，需要刷新一下表格
                    if (cur_index!==result.data.size||!_this.status()){
                        _table.reload();
                        cur_index = result.data.size;
                    }
                }
            }
        });
    };

    this.uuid = function () {
        return task_id;
    };

    this.status = function (status) {
        if (status!==undefined){
            task_status = status;
        }
        return task_status;
    };

    this.playbooks = function () {
        return playbooks;
    };

    // 暂停或者继续执行当前任务
    this.resume=function (status) {
        // 校验任务是否存在
        if (task_id === ""){
            return false;
        }
        // 进行任务调度
        $.ajax({
            type:"post",
            url: "v1/exec/"+(status?"pause":"resume"),
            contentType:'application/json',
            data: task_id,
            async: false,
            success:function(result){
                _this.status(result.code===200) ;
                if(!task_status){
                    console.log(task_name+"继续执行失败!")
                }

                _opt.resume(_this,result);

                // 暂停或者继续执行的时候影响table 主机的状态
                _table.reload();
            }
        });
        return task_status;
    };

    this.start = function (targets) {
        // 如果有任务在执行，直接调出执行窗体
        if (task_status){
            // 更新当前任务的创建，一边直接展示
            _opt.start(_this,{
                status: '1',
                createDate: new Date().getTime()
            });
            return task_status;
        }

        $.ajax({
            type:"post",
            url: "v1/exec/"+task_name,
            contentType: 'application/json',
            data: JSON.stringify(targets),
            async: false,
            success:function(result){
                // 只要任务创建成功就将任务置为运行态
                _this.status(result.code===200) ;
                if(task_status){
                    task_id = result.data.taskId;
                    _this.create_date = result.data.createDate;
                    _opt.start(_this,result.data);
                }else{
                    _table.reload();
                    alert(result.message);
                    console.log(task_name+"任务创建失败!->"+result.message)
                }
            }
        });

        return task_status;
    };


    this.destroy = function(){
        if (interval!==undefined){
            window.clearInterval(interval);
        }
    };
    
    this.close = function () {
        // 校验任务是否存在
        if (task_id === ""){
            return false;
        }
        // 进行任务调度
        $.ajax({
            type:"post",
            url: "v1/exec/close",
            data: {
                uuid: task_id
            },
            async: false,
            success:function(result){
                _this.status(result.code===200) ;
                if(!task_status){
                    console.log(task_name+"结束任务失败!")
                }
            }
        });
        return task_status;
    };
    
    this.task_id = function () {
        return task_id;
    };

    return init();
};