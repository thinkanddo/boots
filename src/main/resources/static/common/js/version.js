var Table = function () {
    var $table = $('#version_table');

    var update_tag_list = function () {
        $.get("v1/version", function(result){
            $(".tag-list").each(function () {
                var $select = $(this);
                $select.html('');
                result.data.forEach(function (opt) {
                    $select.append('<option value="'+opt+'">'+opt+'</option>')
                })
            })
        });
    };

    // 制作完工具包之后 下载
    var download_tools = function () {
        $.get();
    };

    // 初始化样式
    var init = function () {
        var $version_tag = $('#version_tag');
        var $curl_version = $('#cur_tag');
        var $target_version = $('#target_tag');

        var cur_play_code;

        update_tag_list();

        var execute_tools = {
            pack_pull_code: {
                play: new InstallCluster('pack_pull_code',update_tag_list),
                click: function () {
                    execute_tools.pack_pull_code.play.start([]);
                }
            },
            pack_make_install:{
                play: new InstallCluster('pack_make_install'),
                click: function(){
                    if (execute_tools.pack_make_install.play.status()){
                        execute_tools.pack_make_install.play.start([]);
                    }else{
                        showModel("pack_make_install_modal");
                    }
                    cur_play_code = "pack_make_install";
                },
                submit:function () {
                    var flag = submitGlobal({SYSTEM_CUR_VERSION:$version_tag.val()});
                    if (flag){
                        execute_tools[cur_play_code].play.start([]);
                    }
                }
            },
            pack_make_update:{
                play: new InstallCluster('pack_make_update'),
                click: function(){
                    if (execute_tools.pack_make_update.play.status()){
                        execute_tools.pack_make_update.play.start([]);
                    }else{
                        showModel("pack_make_update_modal");
                    }
                },
                submit:function () {
                    var flag = submitGlobal({SYSTEM_CUR_VERSION:$curl_version.val(),SYSTEM_TARGET_VERSION:$target_version.val()});
                    if (flag){
                        execute_tools.pack_make_update.play.start([]);
                    }
                }
            },
            pack_make_tools: {
                play: new InstallCluster('pack_make_tools',download_tools),
                click: function(){
                    if (execute_tools.pack_make_tools.play.status()){
                        execute_tools.pack_make_tools.play.start([]);
                    }else{
                        showModel("pack_make_install_modal");
                    }
                    cur_play_code = "pack_make_tools";
                },
                submit:function () {
                    var flag = submitGlobal({SYSTEM_CUR_VERSION:$version_tag.val()});
                    if (flag){
                        execute_tools[cur_play_code].play.start([]);
                    }
                }
            }
        };

        // 实例化菜单
        for (var playCode in plays){
            var play = plays[playCode];
            var play_tool = execute_tools[playCode];
            if (play_tool!==undefined&&play.playType===0){
                tools_menus(play,$("#template-form"));
                $("#"+playCode).on('click',play_tool.click);
                if (play_tool.submit!==undefined){
                    $("#"+playCode+"_btn").on('click',play_tool.submit);
                }
            }
        }

        var show_columns = [
            {
                field: "packageName",
                title: "包名称"
            },{
                field: "curVersion",
                title: "当前版本"
            },{
                field: "targetVersion",
                title: "目标版本"
            },{
                field: "packageSize",
                title: "安装包大小"
            },{
                field: "type",
                title: "包类型",
                formatter: function (value, row, index) {
                    if (value==='0'){
                        return "安装包";
                    }
                    if (value==='1'){
                        return "升级包";
                    }
                    if (value==='2'){
                        return "工具包";
                    }
                }
            },{
                field: "packagePath",
                title: "安装包路径"
            },{
                field: "createDate",
                title: "创建时间",
                formatter: dateFormatter
            },{
                field: "operator",
                title: "操作",
                width:120,
                switchable:false,
                height: "32px",
                align: 'center',
                formatter:optFormatter,
                events: {
                    'click .delete': function (e, value, row, index) {
                        $.ajax({
                            url:  'v1/version/pack',
                            contentType: 'application/json',
                            type: "delete",
                            data: row.packageName,
                            success:function(data){
                                if (data.code === 200){
                                    $table.bootstrapTable('removeByUniqueId',row.packageName);
                                }else {
                                    alert("删除失败！");
                                }
                            }
                        });
                    }
                }
            }
        ];

        $table.bootstrapTable({
            url: "v1/version/pack",
            classes: "table",
            minimumCountColumns: 2,
            buttonsAlign:'right',
            searchAlign:'right',
            toolbarAlign:'left',
            pageList:[5, 10, 20, 50, 100, 200],
            columns: show_columns,
            uniqueId:"packageName",
            responseHandler:function(res){
                return res.code===200?res.data:[];
            }
        });
    };

    // 操作选项格式化展示
    var optFormatter = function (value,row,index) {
        return [
            '<a class="col-sm-offset-1 edit" href="'+row.ftpLink+row.packagePath+row.packageName+'" ><i title="下载" class="fa fa-download" ></i></a>',
            '<a class="col-sm-offset-1 delete"  href="javascript:void(0)" ><i title="删除环境" class="fa fa-trash"></i></a> '
        ].join(' ');
    };

    this.reload = function (data) {
        if (data === undefined){
            $.get("v1/version/pack", function(result){
                $table.bootstrapTable('load',result.data);
            });
        }else{
            $table.bootstrapTable('load',data);
        }
    };

    return init();
};
