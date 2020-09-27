$(function  () {
    var $k8s_table = $("#k8s_table");

    // var $k8s_group = $("#k8s_group");

    var $k8s_ns = $("#k8s_ns");

    var $k8s_type = $("#k8s_type");

    var $search_btn = $("#search_btn");

    var $content = $("#k8s_info_content");

    var defaultOpt ={
        classes: "table",
        minimumCountColumns: 2,
        buttonsAlign:'left',
        searchAlign:'left',
        exportDataType: "all",
        exportOptions: {
            fileName: "k8s资源",
            exportHiddenCells: true
        },
        exportTypes: ['csv','excel'],
        toolbarAlign:'right',
        uniqueId:"id",
        formatFullscreen: function(){ return "全屏"; },
        formatColumns: function(){ return "角色"; }
    };

    var optFormatter = function () {
        return ['<a class="col-sm-offset-1 yaml"  href="javascript:void(0)" data-toggle="modal" data-target="#k8s_info" ><i title="编辑资源" class="fa fa-eye " ></i></a>',
            '<a class="col-sm-offset-1 detail"  href="javascript:void(0)" data-toggle="modal" data-target="#k8s_info" ><i title="资源详细" class="fa fa-info-circle " ></i></a>'
        ].join(' ');
    };

    var poPptFormatter = function () {
        return ['<a class="col-sm-offset-1 yaml"  href="javascript:void(0)" data-toggle="modal" data-target="#k8s_info" ><i title="编辑资源" class="fa fa-eye " ></i></a>',
            '<a class="col-sm-offset-1 detail"  href="javascript:void(0)" data-toggle="modal" data-target="#k8s_info" ><i title="资源详细" class="fa fa-info-circle " ></i></a>',
            '<a class="col-sm-offset-1 logs"  href="javascript:void(0)" data-toggle="modal" data-target="#k8s_info" ><i title="查询日志" class="fa fa-info"></i></a> ',
            '<a class="col-sm-offset-1 delete"  href="javascript:void(0)" ><i title="删除po" class="fa fa-trash"></i></a> '
        ].join(' ');
    };

    var showYaml = function (e, value, row, index) {
        $content.val("");
        var opt = get_search (row.NAME);
        $.get("v1/cmd/k8s/yaml?"+$.param(opt),function (res) {
            $content.val(res.data.rows.join("\n"));
        })
    };

    var showDetail = function (e, value, row, index) {
        $content.val("");
        var opt = get_search (row.NAME);
        $.get("v1/cmd/k8s/detail?"+$.param(opt),function (res) {
            $content.val(res.data.rows.join("\n"));
        });
    };

    var showLog= function (e, value, row, index) {
        $content.val("");
        var opt = get_search (row.NAME);
        $.get("v1/cmd/k8s/logs?"+$.param(opt),function (res) {
            $content.val(res.data.rows.join("\n"));
        });
    };

    // 删除k8s 资源
    var deleteK8S = function (e, value, row, index) {
        $.ajax({
            type:"delete",
            url: 'v1/cmd/k8s/resource',
            timeout: 300*1000,
            data: get_search (row.NAME)
        });
    };

    var get_search = function (name) {
        return  {
            namespace: $k8s_ns.val(),
            resource: $k8s_type.val(),
            name: name
        }
    };
    
    var loadTable = function (func) {
        var opt = get_search();

        var url = "v1/cmd/k8s/resource?"+$.param(opt);

        $.get(url,function (res) {
            var columns = [];

            if (res.code===200){
                res.data.fields.forEach(function (col) {
                    columns.push({
                        field: col,
                        title: col
                    })
                });

                columns.push({
                    field: "row_opt",
                    title: "操作",
                    width:120,
                    switchable:false,
                    height: "32px",
                    align: 'center',
                    formatter: (opt.resource==="po"?poPptFormatter:optFormatter),
                    events: {
                        'click .yaml': showYaml,
                        'click .detail': showDetail,
                        'click .logs': showLog,
                        'click .delete': deleteK8S
                    }
                });

                // 合成表opt对象
                var table_opt = {
                    columns: columns,
                    ajax: function (params) {
                        res.rows = res.data.rows;
                        params.success(res);
                    }
                };

                func(table_opt);
            }
        });
    };
    
    var initTable = function () {
        loadTable(function (table_opt) {
            $k8s_table.bootstrapTable($.extend(defaultOpt,table_opt));
            $("#k8s_form").removeClass("install-hide");
        });
    };

    var refreshTable = function () {
        loadTable(function (table_opt) {
            $k8s_table.bootstrapTable("refreshOptions",table_opt);
        })
    };
    
    var loadSelect = function (_this,dictName) {
        $.ajax({
            url: "v1/dict?dictName="+dictName,
            type: "get",
            async: false,
            success:function (res) {
                if(res.code===200){
                    var flag = true;

                    var options = [];
                    res.data.forEach(function (item) {
                        var show = item==='默认'?"":item;
                        if (flag){
                            options.push("<option value='"+item+"' selected='selected'>"+show+"</option>");
                            flag = false;
                        }else{
                            options.push("<option value='"+item+"'>"+show+"</option>");
                        }
                    });

                    _this.html(options.join("\n"));
                }
            }
        });
    };

    var init = function () {
        $search_btn.on('click',refreshTable);

        // 初始化选择框
        $("select").each(function () {
            var _this = $(this);
            var dictName =_this.data("name");
            var target =_this.data("target");

            if (target !== undefined){
                var $target = $("#"+target);
                $target.on('change',function () {
                    // 清空 option 重新加载
                    _this.html("");
                    loadSelect( _this,_this.data("name") + $target.val());
                    _this.selectpicker("refresh");
                })
            }

            loadSelect( _this,dictName);

            _this.selectpicker();
        });

        // 初始化表数据
        initTable();
    };

    return init();
});