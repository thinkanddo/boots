var $modal = $("#installModel");

var is_show = false;
var latest_task ;
var InstallCluster = function (play_code,finish) {
    var $log_container = $("#log-container");
    var _this = this;
    var is_finish = false;

    var scroll_flag = true;
    var top = -1;
    $log_container.scroll(function () {
        var cur_top = $log_container.scrollTop();
        scroll_flag = cur_top>=top;
        top = cur_top;
    });

    var init = function () {
        // 初始化 滚动日志
        $log_container.css({
            height: $(window).height()*6/10+"px"
        });
        this.scroll = $log_container.niceScroll({
            cursorcolor:"gray",
            cursorwidth:"16px",
            enablescrollonselection: true  // 当选择文本时激活内容自动滚动
        });

        var play = new Play(play_code,{
            read_last: true,
            interval_time: 1000,
            start: function(play,task){
                top = -1;
                scroll_flag=true;
                action_btn(play.status());
                if (task.status!=='2' && task.status!=='7'){
                    if(!is_show||task.createDate>=latest_task._play.create_date){
                        _this.show();
                        if(!is_show){
                            is_show = true;
                        }
                        latest_task = _this;
                    }
                }
            },
            resume: function(play,data){
                action_btn(play.status());
            },
            callback: function (play,data) {
                if (data.code === 200){
                    if (latest_task!==undefined&&play.task_id()===latest_task._play.task_id()){
                        refreshLog(data.data);
                    }
                }
            }
        });

        // 将任务的启动接口暴露给install 对象
        _this.start = play.start;
        _this._play = play;
        return _this;
    };

    var get_pause_btn = function () {
        return  $("#"+play_code+"_pause")
    };

    var action_btn = function (status) {
        get_pause_btn().html(status?"暂停执行":"继续执行");
    };

    var refreshLog =function (data) {
        // 更新执行日志
        $(".logInsert").html(data.stdout);
        if (scroll_flag){
            $log_container.getNiceScroll().resize();
            var div = document.getElementById('log-container');
            div.scrollTop = div.scrollHeight;
        }

        // 滚动进度
        active_show(_this.status(),data.size,data.status);

        if (data.status === '2'){
            is_finish = true;
            get_pause_btn().html("执行完成");
            $("#installPercent div").removeClass("progress-bar-striped");
        }else{
            action_btn(_this._play.status());
        }
    };

    // open任务界面操作
    this.show = function () {
        $modal.find(".modal-footer").html('<button type="button" id="close_task" data-dismiss="modal" class="btn btn-primary" data-loading-text="关闭任务">关闭任务</button> <button id="'+play_code+'_pause'+'" type="button" class="btn btn-primary" data-loading-text="加载中....">加载中....</button>');
        get_pause_btn().on('click',function () {
            if (is_finish){
                if (finish!==undefined){
                    finish();
                }
                close();
            }else{
                _this._play.resume(_this.status());
            }
        });
        
        $("#close_task").on('click',function () {
            _this._play.close();
        });
        $modal.modal('show');
    };
    
    // close任务界面操作
    var close = function () {
        $modal.modal('hide');
    };

    var active_show = function (task_status,size,status) {
        var playbooks = _this._play.playbooks();
        var length= playbooks.length;
        var percents = parseInt(size*100/length) ;
        var _class= "progress-bar progress-bar-striped "+(task_status?"bg-success progress-bar-animated ": (status==='2'?"bg-success ":(status==='3'?"bg-danger ":" ")));
        $("#installPercent div").attr('class',_class).attr('style','width: '+percents+"%").html(percents+"%");
        playbooks.forEach(function (playbook) {
            if(playbook.index === size){
                $("#processTitle").html(playbook.playbookName);
            }
        });
    };

    /**
     *  判断 play 的执行状态，用于安装集群的时候做判断是否调出弹出窗体
     * @returns {boolean}
     */
    this.status = function () {
        return _this._play.status();
    };

    return init();
};