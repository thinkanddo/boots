var Table = function () {
    var _this = this;
    var $table = $('#host_table');
    // var $clean_cluster = $("#clean_cluster");

    var host_status_class={'1':"text-danger",'2':"text-success",'0':"text-secondary"};
    var dev_status_class={'0':"btn-success",'1':"btn-secondary",'2':"btn-primary",'3':"btn-warning"};
    var role_status_class={'-1':'change btn-secondary','0':'change btn-success','1':'change btn-success','2':'btn-primary role_installed'};

    // 0 未安装任何节点  1 所有节点都是启动状态  2部分节点未启动
    var host_lock_class={true:'fa-pulse',false:'tools_cluster_check'};
    var host_lock_title={true:'任务执行中...',false:'校验主机'};
    var pop_msg_data={true:'data-toggle="popover" data-trigger="focus" data-container="body" data-placement="right"',false:''};
    var dev_enable_swatch={'0':'UN_USED','1':'DISABLED','3':'UN_FORMAT'};

    var _click_time = null; //单击事件超时变量

    _this._policy = new Global(_this);
    _this._form = new HostForm(_this);

    var getHandle = function (playCode) {
        if (undefined!==plays[playCode]){
            return plays[playCode].handle;
        }else{
            return undefined;
        }
    };

    // 初始化样式
    var init = function () {
        $.get("v1/env/type",function (result) {
            if(result.data){
                $("#license").html("<small>license</small>").on('click',function () {
                    $.get('v1/env/license',function (result) {
                        $("#license_cnt").val(result.data);
                        showModel("license-modal");
                    });
                });
                $("#license-copy").on('click',function () {
                    var e = document.getElementById("license_cnt");
                    e.select(); // 选择对象
                    document.execCommand("Copy");
                })
            }
        });

        _this.dynamic_columns=[];
        _this.role_extend = _this._policy.roles_extend();
        var role_show = _this._policy.roles_show();
        var show_columns = [
            {
                field: "isSelect",
                checkbox: true,
                rowspan: 2,
                switchable:false,
                valign: "middle"
            },{
                field: "id",
                title: "主机IP",
                rowspan: 2,
                width: "165px",
                switchable:false,
                valign: "middle",
                formatter:IPFormatter
            },{
                field: "username",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "用户名"
            },{
                field: "password",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "密码"
            },{
                field: "os",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "操作系统",
                formatter: function (value, row, index) {
                    return (value===null||value===undefined)?value:(value+(row.osVersion===null?"":(" "+row.osVersion)));
                }
            },{
                field: "kernel",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "内核版本"
            },{
                field: "hasGpu",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "GPU",
                formatter:function (value, row, index) {
                    return value?"支持":"--";
                }
            },{
                field: "cpu",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "CPU核数"
            },{
                field: "memory",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "内存/GB"
            },{
                field: "devs",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "设备选择",
                formatter: devFormatter,
                events: {
                    'click .change-dev': clickDevEvent,
                    'dblclick .change-dev': DblclickDevEvent
                }
            },{
                field: "allSpace",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "可使存储",
                formatter: spaceFormatter
            },{
                field: "roles",
                title: "安装角色",
                align: 'center',
                colspan: _this.role_extend.length+1
            }
        ];

        _this.role_extend.forEach(function (role) {
            var visible=role_show.indexOf(role.roleCode)!==-1;
            _this.dynamic_columns.push({
                field: role.roleCode,
                title: role.roleDesc,
                visible: visible,
                formatter: function (value,row,index,field) {
                    var host_role = row.roles[field];
                    if(host_role === undefined){  host_role={'status':'-1'}; }
                    return  '<button type="button" class="btn btn-sm '+role_status_class[host_role.status]+' " data-role-code="'+field+'" ><i class="fa '+role.icon+'"> </i></button> '
                },
                width: "50px",
                align: 'center',
                events: {
                    'click .change': function (e, value, row, index) {
                        //更新对应的角色节点
                        var roleCode = role.roleCode;
                        if (row.roles[roleCode]===undefined){
                            row.roles[roleCode] = {status: '0'};
                        }else if(row.roles[roleCode].status==='2'){
                            alert("角色已经安装！");
                            return;
                        } else {
                            delete row.roles[roleCode];
                        }
                        _this._policy.changeRole(row);
                    }
                }
            });
        });

        var tools_handle = function(e, value, row, index){
            var tool_class = $(e.currentTarget).attr('class').split(" ")[2];
            var tool = getHandle(tool_class);

            // 删除的时候 如果 没安装直接删除
            if (tool_class === "tools_host_delete" ){
                if(row.hostname===null||row.hostname===""||!_this._policy.isInstalled()){
                    deleteHost(row);
                    return;
                }

                // 判断有没有角色已经安装了
                var roleStatus = false;
                for (var roleCode in row.roles){
                    if(row.roles[roleCode].status==='2'){
                        roleStatus = true;
                        break;
                    }
                }

                // 如果没有则删除该角色
                if (!roleStatus){
                    deleteHost(row);
                    return
                }
            }

            if (tool!==undefined){
                tool.handle([row.ip],function (data) {
                    // 任务执行完成之后的回调流程
                    switch (tool_class) {
                        case "tools_host_delete":
                            if (data.status==='2'){
                                deleteHost(row);
                            }else{
                                alert("节点卸载任务失败！");
                            }
                            break;
                    }
                });
            }else{
                alert("当前任务暂不支持！");
            }
        };

        var events = {};
        events['click .edit']=function (e, value, row, index) {
            _this._form.editHost(row);
        };
        for (var playCode in plays){
            if (plays[playCode].status==='1'){
                events['click'+" ."+playCode]=tools_handle;
            }
        }

        _this.dynamic_columns.push({
            field: "host_opt",
            title: "操作",
            width:120,
            switchable:false,
            height: "32px",
            align: 'center',
            formatter:optFormatter,
            events: events
        });

        $table.bootstrapTable({
            url: "v1/host",
            classes: "table",
            minimumCountColumns: 2,
            buttonsAlign:'right',
            searchAlign:'right',
            toolbar:"#toolbar",
            exportDataType: "all",
            exportOptions: {
                fileName: _this._policy.getCfg("SYSTEM_ENV_CODE")+"-角色分布",
                ignoreColumn:['username','password','devs','allSpace'],
                exportHiddenCells: true,
                onCellHtmlData: function ($cell, rowIndex, colIndex, htmlData) {
                    if($cell.find(".btn-primary").length>0){
                        return "已安装";
                    }else{
                        return htmlData;
                    }
                }
            },
            exportTypes: ['csv','excel'],
            toolbarAlign:'left',
            uniqueId:"ip",
            pageList:[5, 10, 20, 50, 100, 200],
            columns: [show_columns,_this.dynamic_columns],
            formatFullscreen: function(){ return "全屏"; },
            formatColumns: function(){ return "角色"; },
            responseHandler:function(res){
                return res.code===200?res.data:[];
            },
            onResetView: function () {
                $('.tooltip').remove();
                $('[data-toggle="tooltip"]').tooltip();
                $('.popover').remove();
                $('[data-toggle="popover"]').popover('show');
            }
        });

    };


    /**
     * @return {string}
     */
    var IPFormatter = function (value,row,index) {
        row.ip = value.ip;
        var msg = getHostMsg(row.ip);
        var has_msg = msg!==undefined && msg.length>0;
        return [' <div class="',host_status_class[row.status],'" ', pop_msg_data[has_msg] ,' data-container="body" data-content="',has_msg?msg.join(' ; '):'',' !" > ',
                        '<span><i title="校验主机" class="fa fa-desktop" ></i> ', row.ip,'</span>',
                    '</div>'].join('');
    };
    // 设备格式化展示
    var devFormatter = function(value,row,index){
        var dev_html = [];
        row.enableSize=0;
        row.devs.forEach(function (dev) {
            dev_html.push(' <button type="button" data-trigger="hover" data-toggle="tooltip" data-placement="top" title="可用大小 【'+dev.enableSize+' GB】" class="change-dev btn btn-sm ' + dev_status_class[dev.status] + '" data-id="'+dev.id+'" > ');
            dev_html.push('     <i class="fa fa-hdd" > ' + dev.devName.substr(5) +' </i>');
            dev_html.push(' </button> ');
            if (dev.status !== '1'){row.enableSize += dev.enableSize;}
        });
        return dev_html.join(' ');
    };
    // 可用空间格式化展示
    var spaceFormatter = function (value,row,index) {
        return ['<span>',row.enableSize," (GB)",'</span>'].join("");
    };
    // 操作选项格式化展示
    var optFormatter = function (value,row,index) {
        var del_handle = getHandle('tools_host_delete');
        return ['<a class="col-sm-offset-1 edit"  href="javascript:void(0)" data-toggle="modal" data-target="#host-add" ><i title="编辑主机" class="fa fa-edit " ></i></a>',
            '<a class="col-sm-offset-1 " href="javascript:void(0)" ><i title="'+host_lock_title[row.hostLock]+'" class="fa fa-sync '+host_lock_class[row.hostLock]+'"></i></a> ',
            '<a class="col-sm-offset-1 " href="disk.html?'+row.id.ip+'" target="_blank"><i title="设备分配" class="fa fa-hdd"></i></a> ',
            '<a class="col-sm-offset-1 '+(undefined!==del_handle&&del_handle.hasHost(row.ip)?'blink':'')+'"  href="javascript:void(0)" ><i title="删除主机" class="fa fa-trash tools_host_delete"></i></a> '
        ].join(' ');
    };

    // 删除主机事件
    var deleteHost= function (row) {
         $.ajax({
            type:"delete",
            url: 'v1/host',
            contentType:'application/json',
            data:row.id.ip,
            success:function(data){
                if (data.code === 200){
                    _this.delete(row.id.ip);
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    // 单击设备，切换设备状态(可用，不可用)
    var clickDevEvent = function (e, value, row, index) {
        clearTimeout(_click_time);
        _click_time = setTimeout(function(){ changeDevEvent(e, row); }, 300);
    };

    // 双击设备，切换设备状态(是否可格式化，前提设备为可用状态)
    var DblclickDevEvent = function (e, value, row, index) {
        clearTimeout(_click_time);
        changeDevEvent(e, row);
    };

    // 调接口改数据库设备状态，刷新_this
    var changeDevEvent = function (e, row) {
        var $this = $(e.target);
        var eventType = e.type;
        if ($this[0].type !== "button"){
            $this = $this.parents("button");
        }

        var dev;
        // $this.tooltip('hide');
        row.devs.forEach(function (_dev) {
            if( _dev.id === $this.data("id") ){
                dev = _dev;
            }
        });

        if (dev!==undefined){
            var devNextStatus;
            var devEvent = new Array(eventType , dev.status);

            // 单击或双击事件后判断接下来变更的状态
            switch (devEvent.join('')) {
                case 'click0':
                    devNextStatus = '1';
                    break;
                case 'click1':
                    devNextStatus = '0';
                    break;
                case 'dblclick0':
                    devNextStatus = '3';
                    break;
                case 'dblclick3':
                    devNextStatus = '0';
                    break;
                default:
                    devNextStatus = dev.status;
            }
            $.ajax({
                type:"post",
                url: 'v1/dev/devchange',
                contentType:'application/json',
                data: JSON.stringify({
                    "id": dev.id,
                    "status": devNextStatus
                }),
                success:function(data){
                    if (data.code === 200){
                        _this.reload();
                    }else{
                        console.log(data);
                    }
                }
            });
        }
    };


    this.refresh = function () {
        $table.bootstrapTable('refresh');
    };
    
    this.reload = function (data) {
        if (data === undefined){
            $.get("v1/host", function(result){
                $table.bootstrapTable('load',result.data);
            });
        }else{
            $table.bootstrapTable('load',data);
        }
    };

    this.getSelectHosts = function () {
        return $table.bootstrapTable('getAllSelections');
    };

    this.getHosts = function () {
        return $table.bootstrapTable('getData');
    };
    
    this.delete = function (ip) {
        $table.bootstrapTable('removeByUniqueId',ip);
    };
    
    this.getSelectFields = function (field) {
        return _this.getSelectHosts().map(function (row) {
            return row[field];
        });
    };

    this.getFields = function (field) {
        return _this.getHosts().map(function (row) {
            return row[field];
        });
    };

    return init();
};
