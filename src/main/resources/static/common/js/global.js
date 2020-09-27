/**
 * 主要完成 主机角色策略的生成，角色变更后磁盘的分配
 *
 * @constructor
 */

var Global = function (table) {
    var $vip = $("#k8s_virtual_ip");
    var $tools_group = $("#tools_group");
    var $global_tab = $("#global_tab");
    var $global_content = $("#global_content");
    var $global_form = $("#global_form");
    var $save_global = $("#save_global");
    var $package = $("#package");
    var $env_code = $("#env_code");

    var cfg_type_class = {'0': 'readonly','1':'readonly','2':''};
    var cfg_group={'SYSTEM':"系统配置",'COMPOSE':"组件配置",'PORT': "端口配置"};

    // 可选择的角色节点
    var roles_show = [];
    var roles_base = [];
    var roles_extend = [];

    var upload_flag = false;
    var global = {};
    var _this = this;
    var _table = table;
    // 存储每个角色对应的主机
    var roles_all = [];

    var change_scroll = function () {
        $global_form.getNiceScroll().resize();
    };

    var init = function () {
        var active_flag = true;
        for (var key in cfg_group){
            $global_tab.append('<li class="nav-item"><a class="nav-link '+(active_flag?'active':'')+'" id="'+key+'-tab" data-toggle="tab" href="#'+key+'" role="tab" aria-controls="'+key+'" aria-selected="'+active_flag+'">'+cfg_group[key]+'</a></li>');
            if (active_flag === true){
                active_flag = false;
            }
        }

        active_flag = true;
        for (key in cfg_group){
            $global_content.append('<div class="tab-pane fade '+(active_flag?'show active':'')+'" id="'+key+'" role="tabpanel" aria-labelledby="'+key+'-tab"><br/></div>')
            if (active_flag === true){
                active_flag = false;
            }
        }

        $.ajax({
            url:  "v1/global",
            async: false,
            success: function (data) {
                if (data.code===200){
                    data.data.forEach(function (cfg) {
                        var key = cfg.id.cfgKey;
                        global[key]=cfg.cfgValue;
                        var cfg_seg = key.split('_');
                        if (cfg_group[cfg_seg[0]]!==undefined){
                            var suffix = cfg_seg[cfg_seg.length-1];
                            if (suffix==='FLAG'){
                                $('#'+cfg_seg[0]).append(
                                    '                     <div class="form-group " >\n' +
                                    '                        <label class="control-label" for="'+key+'">'+key+': '+cfg.memo+'</label>\n' +
                                    '                        <div class="input-group">\n' +
                                    '                            <div class="input-group-prepend"><span class="input-group-text fa '+cfg.icon+'"></span></div>\n' +
                                    '                            <select id="'+key+'" name="'+key+'" class="form-control '+cfg_seg[0]+' '+cfg_type_class[cfg.cfgType]+'"  >' +
                                    '                                 <option value="true" '+((cfg.cfgValue==='true')?'selected':'')+'>true</option>' +
                                    '                                 <option value="false" '+((cfg.cfgValue==='true')?'':'selected')+'>false</option>' +
                                    '                             </select>\n' +
                                    '                        </div>\n' +
                                    '                    </div>');
                            }else{
                                $('#'+cfg_seg[0]).append(
                                    '                     <div class="form-group " >\n' +
                                    '                        <label class="control-label" for="'+key+'">'+key+': '+cfg.memo+'</label>\n' +
                                    '                        <div class="input-group">\n' +
                                    '                            <div class="input-group-prepend"><span class="input-group-text fa '+cfg.icon+'"></span></div>\n' +
                                    '                            <input id="'+key+'" name="'+key+'" class="form-control '+cfg_seg[0]+' '+cfg_type_class[cfg.cfgType]+'"  type="text" value="'+cfg.cfgValue+'"/>\n' +
                                    '                        </div>\n' +
                                    '                    </div>');
                            }

                            if(cfg_type_class[cfg.cfgType]===''){
                                if(cfg_seg[cfg_seg.length-1]==='HA'){
                                    bind_valid($("#"+key),"端口范围(0-65535)",valid_empty_port);
                                }else if (cfg_seg[0]==='PORT'){
                                    bind_valid($("#"+key),"端口范围(0-65535)",valid_port);
                                }else if(key==='SYSTEM_CONTROL_IP'){
                                    bind_valid($("#"+key),cfg.memo+"格式不对",valid_ip);
                                }else if(cfg_seg[cfg_seg.length-1]==='IP'){
                                    bind_valid($("#"+key),cfg.memo+"格式不对",valid_empty_ip);
                                }else if(cfg_seg[cfg_seg.length-1]==='FLAG'){
                                    bind_valid($("#"+key),"只能输入true/false",valid_bool);
                                }else if(key!=='SYSTEM_ENV_SUFFIX' && key!=='SYSTEM_TIMEZONE'){
                                    bind_valid($("#"+key),"参数不能为空",valid_empty);
                                }
                            }
                        }
                    });

                    if (_this.isInstalled()){
                        $global_content.find(".COMPOSE,.PORT").attr("disabled","disabled");
                    }else{
                        $global_content.find('.COMPOSE.readonly,.PORT.readonly').attr("disabled","disabled");
                    }

                    $vip.val(global.COMPOSE_K8S_VIRTUAL_IP);
                    $env_code.html(global.SYSTEM_ENV_CODE);
                }else{
                    console.log("全局配置加载失败！");
                }
            }
        });

        $global_form.css({height: $(window).height()*6/10+"px"}).niceScroll({
            cursorcolor:"gray",
            cursorwidth:"16px",
            enablescrollonselection: true  // 当选择文本时激活内容自动滚动
        });
        $('a[data-toggle="tab"]').on('shown.bs.tab', change_scroll);
        $("#global").on('shown.bs.modal',change_scroll);

        var inter = 100;
        var times = 1000/inter;
        var index = 0;
        var speed_show = "0 KB/s";
        var speed = 0;
        setInterval(function (args) {
            if (upload_flag){
                $.get("v1/version/upload",function (data) {
                    if (data.code === 200 ){
                        if (data.data!==null){
                            index++;
                            var _class = !data.data.finish?"bg-success progress-bar-animated ": (data.data.success?"bg-success ":"bg-danger");
                            var percents = data.data.send*100/data.data.size;

                            speed += data.data.speed;

                            if (index===times){
                                if (speed<1024){
                                    speed_show = speed+"byte/s"
                                }else if (speed/1024<1024){
                                    speed_show = (speed/1024).toFixed(2)+"KB/s"
                                }else if(speed/1024/1024<1024){
                                    speed_show = (speed/1024/1024).toFixed(2)+"MB/s"
                                }
                                index=0;
                                speed=0;
                            }

                            show_upload_process(_class,speed_show,percents);
                            upload_flag = !data.data.finish;
                        }
                    }else{
                        upload_flag = false;
                    }
                });
            }
        },inter);

        $package.on('change',package_upload);

        var confirm_play;
        var confirm_targets;
        
        $("#confirm_exec").on("click",function () {
            var status = confirm_play.start(confirm_targets);
            if(!status){
                alert("创建任务失败！");
            }
        });

        var alloc_store = function (play,targets) {
            $.ajax({
                url: "v1/dev/allocate",
                type:"post",
                async: false,
                contentType:'application/json',
                data: JSON.stringify(targets),
                success:function(data){
                    if (data.code === 200 ){
                        confirm_play =play;
                        confirm_targets = targets;
                        showModel("confirmModel");

                    }else{
                        alert("存储分配失败！"+data.message);
                    }
                }
            });
        };

        var execute_tools = {
            cluster_install:{
                show: _this.isInstalled()===false,
                click: function () {
                    if(!table._policy.saveVip()){
                        return;
                    }
                    var targets = table.getSelectFields("ip");
                    if (targets.length === 0){
                        targets = table.getFields("ip") ;
                    }
                    alloc_store(plays["cluster_install"].handle,targets);
                },
                finish: function () {
                    top.location.reload();
                }
            },
            cluster_extend:{
                show: _this.isInstalled()===true,
                click: function () {
                    var targets = table.getSelectFields("ip");
                    if (targets.length===0){
                        alert("请选择拓展的主机节点");
                        return;
                    }
                    alloc_store(plays["cluster_extend"].handle,targets);
                }
            },
            cluster_upgrade:{
                show:_this.isInstalled()===true,
                click: function () {
                    $package.data("play","cluster_upgrade").click();
                }
            },
            pack_pull_code:{
                show: false
            },
            pack_make_install:{
                show: false
            },
            pack_make_update:{
                show: false
            }
        };

        for (var playCode in plays){
            var play = plays[playCode];

            var tool_flag = play.playCode.split("_")[0]==='tools';
            var show = execute_tools[play.playCode]!==undefined&&execute_tools[play.playCode].show;

            // 展示菜单
            if (play.playType===0){
                if(tool_flag){
                    tools_menus(play,$tools_group);
                }else if (show){
                    tools_menus(play,$("#template-form"));
                }
            }else{
                tools_menus(play,$("#"+play.playCode.split("_").slice(0,play.playType).join("_")));
            }

            // 加载
            if(tool_flag||show){
                if (play.status==='1'){
                    play.handle =  new HostHandle(playCode,play.lockType);
                    if (playCode.split("_")[0]==="tools"){
                        $("#"+playCode).on('click',play.handle.handleAll);
                    }
                }
                if (play.status === '3'){

                    var play_click = function () {
                        var $this = $(this);
                        var play = plays[$this.attr("id")];
                        var targets=[];
                        if (play.lockType){
                            targets = table.getFields("ip");
                            if (targets.length === 0) {
                                alert(" 集群内没有可执行的主机！");
                                return;
                            }
                        }else{
                            targets = table.getSelectFields("ip");
                            if (targets.length === 0){
                                alert(" 请选择要操作的主机节点！主机操作请谨慎");
                                return;
                            }
                        }
                        play.handle.start(targets);
                    };

                    if (undefined!==execute_tools[play.playCode]){
                        var play_opt = execute_tools[play.playCode];

                        // 绑定finish 操作
                        if(undefined!==play_opt.finish){
                            play.handle =  new InstallCluster(playCode,play_opt.finish);
                        }else{
                            play.handle =  new InstallCluster(playCode);
                        }

                        // 绑定click 操作
                        if (undefined!==play_opt.click){
                            $("#"+playCode).on('click',execute_tools[playCode].click);
                        }else{
                            $("#"+playCode).on('click',play_click);
                        }
                    }else{
                        play.handle =  new InstallCluster(playCode);
                        $("#"+playCode).on('click',play_click);
                    }
                }
            }

        }

        $('[data-submenu]').submenupicker();

        $.ajax({
            url:  "v1/roles_cfg",
            async: false,
            success: function (data) {
                if (data.code===200){
                    data.data.forEach(function (role) {
                        switch (role.roleType) {
                            case '0':
                                roles_base.push(role.roleCode);
                                break;
                            case '1':
                                roles_show.push(role.roleCode);
                                roles_extend.push(role);
                                break;
                            case '2':
                                roles_extend.push(role);
                                break;
                        }
                        roles_all[roles_all.length] = role.roleCode;
                    });
                }else{
                    console.log("角色配置加载失败！")
                }
            }
        });

        $save_global.on('click',saveAllGlobal);
    };

    // 角色格式化展示
    this.roles_show = function () {
        return roles_show;
    };

    // 角色格式化展示
    this.roles_extend = function () {
        return roles_extend;
    };

    this.isInstalled = function () {
        return _this.getCfg("SYSTEM_INSTALL_FLAG").toLowerCase()==="true";
    };

    this.getCfg = function (key) {
        return global[key];
    };

    /**
     *  根据表数据生成推荐策略，每种角色都有一个对象方法， 如：master对应master 的函数，为了以后方便角色的拓展
     */
    this.policyRole = function(targets){
        $.ajax({
            type:"post",
            url: 'v1/policy',
            contentType:'application/json',
            data: JSON.stringify(targets===undefined?[]:targets),
            success:function(data){
                if (data.code === 200){
                    _table.reload(data.data);
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    this.changeRole = function(host){
        $.ajax({
            type:"post",
            url: 'v1/roles',
            contentType:'application/json',
            data: JSON.stringify([host]),
            success:function(data){
                if (data.code === 200){
                    _table.reload();
                }else {
                    alert(data.message);
                    console.log(data.code);
                }
            }
        });
    };

    this.saveVip = function(){
        // 获取master 节点数量
        var masterNum = 0;
        _table.getHosts().forEach(function (host) {
            if (host.roles.master!==undefined){
                masterNum++;
            }
        });

        var vip = $vip.val().trim();
        if (vip!==""){
            if (valid_ip(vip)){
                if(_table.getFields("ip").indexOf(vip) !== -1){
                    alert("VIP 不能添加到主机列表中！");
                    return false;
                }
                return submitGlobal({COMPOSE_K8S_VIRTUAL_IP: vip});
            }else{
                alert("VIP 校验不合法 请重新设置！");
                return false;
            }
        }else if(masterNum>1){
            alert("多节点master 请设置VIP！");
            return false;
        }else{
            return true;
        }
    };

    // 提交表单用于检查输入
    var doValid = function (data){
        if (data === undefined){
            data = formJson($global_form);
        }
        checkReset();
        var check_flag = true;
        for (var key in data){
            check_flag = checkActive($("#"+key),data[key])&&check_flag;
        }
        return check_flag;
    };

    var saveAllGlobal = function () {
        var data = formJson($global_form);
        if (doValid(data)){
            if(submitGlobal(data)){
                alert("全局配置保存成功！");
                top.location.reload();
            }else{
                alert("全局配置保存失败！");
            }
            checkReset();
        }
    };

    var show_upload_process = function (_class,show,percents) {
        var $process = $("#uploadPercent div");
        $process.attr('class',"progress-bar progress-bar-striped "+_class).html(show);
        if (percents>0){
            $process.attr('style','width: '+percents+"%")
        }
    };

    var finish_upload = function (msg,success) {
        alert(msg);
        hideModel("uploadModel");
        upload_flag=false;
    };

    var package_upload  = function () {
        var playCode = $package.data("play");
        var form = new FormData(document.getElementById("template-form"));
        upload_flag=true;
        showModel("uploadModel");

        var error_msg;
        $.ajax({
            url:  "v1/version/upload",
            type: "post",
            data: form,
            processData:false,
            contentType:false,
            success:function(data){
                if(data.code===200){
                    show_upload_process("bg-success","上传成功",100);
                    setTimeout(function () {
                        finish_upload("上传成功！",true);
                        plays[playCode].handle.start([]);
                    },1000);
                }else{
                    error_msg=data.message;
                    show_upload_process("bg-danger",data.data.message,-1);
                    setTimeout(function () {
                        finish_upload(error_msg,false);
                    },1000);
                }
            },
            error:function (data) {
                console.log(JSON.stringify(data));
                error_msg=data.message;
                show_upload_process("bg-danger","上传失败",-1);
                setTimeout(2,function () {
                    finish_upload(error_msg,false);
                },1000);
            }
        });
        $package.replaceWith($package.prop("outerHTML"));
        $package = $("#template");
        $package.on('change',package_upload);
    };
    return init();
};
