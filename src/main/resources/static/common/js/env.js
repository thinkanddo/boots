var Env = function () {
    var _this = this;
    var $table = $('#env_table');
    var $env_form = $("#env_form");
    var $env_save = $("#save_env");
    var $image = $("#image_form");

    var $code = $("#code");
    var $name = $("#name");
    var $company = $("#company");
    var $address = $("#address");
    var $password = $("#password");
    var $maintainer = $("#maintainer");
    var $phone = $("#phone");
    var $license = $("#license");

    var $hash = $("#hash");
    var $version = $("#version");
    var $createDate = $("#createDate");
    var $updateDate = $("#updateDate");
    var env = true;
    var version;

    // 初始化样式
    var init = function () {
        $.get("v1/version",function (data) {
            version = data.data[0];
        });

        // 为input 绑定校验控制
        bind_valid($code,"环境编码不能为空",valid_empty);
        bind_valid($name,"项目名称不能为空",valid_empty);
        bind_valid($company,"公司名称不能为空",valid_empty);
        bind_valid($address,"项目地址不能为空",valid_empty);
        bind_valid($password,"环境密码不能为空",valid_empty);
        bind_valid($maintainer,"负责人不能为空",valid_empty);
        bind_valid($phone,"负责人联系方式不能为空",valid_empty);

        $.get("v1/env/type",function (result) {
            env = result.data;
            if(env){
                $("#template-form").append('<a class="btn btn-primary" href="version.html"  id="version_manager" title="版本管理" target="_blank"><i  class="fa fa-code-branch"></i> 版本管理</a>');
                $(".image").remove();
            }else{
                bind_valid($license,"license校验失败",valid_license);
            }
        });

        var show_columns = [
            {
                field: "isSelect",
                checkbox: true
            },{
                field: "code",
                sortable: true,
                title: "集群编码"
            },{
                field: "name",
                title: "项目名称"
            },{
                field: "maintainer",
                sortable: true,
                title: "负责人"
            },{
                field: "phone",
                title: "联系方式"
            },{
                field: "company",
                title: "项目公司"
            },{
                field: "address",
                title: "项目地址"
            },{
                field: "version",
                sortable: true,
                title: "版本"
            },{
                field: "createDate",
                sortable: true,
                title: "开始时间",
                formatter: dateFormatter
            },{
                field: "updateDate",
                sortable: true,
                title: "更新时间",
                formatter: dateFormatter
            },{
                field: "memo",
                visible: false,
                title: "主机列表"
            },{
                field: "operator",
                title: "操作",
                align: 'center',
                formatter:optFormatter,
                events: {
                    'click .edit': function (e, value, row, index) {
                        _this.editEnv(row);
                    },
                    'click .delete': function (e, value, row, index) {
                        $("#delete_code").val(row.code);
                    },
                    'click .entry': function (e, value, row, index) {
                        $("#entry_code").val(row.code);
                        $.get("v1/env/entry/"+row.code,function (data) {
                            if (data.code===200 &&data.data){
                                location.href = 'cluster.html';
                            }else{
                                $("#env-entry").modal('show');
                            }
                        })
                    }
                }
            }
        ];

        $table.bootstrapTable({
            url: "v1/env",
            classes: "table",
            buttonsAlign:'right',
            searchAlign:'right',
            toolbar:"#toolbar",
            toolbarAlign:'left',
            uniqueId:"code",
            sortName: 'updateDate',
            sortOrder: 'desc',
            minimumCountColumns: 5,
            pageList:[5, 10, 20, 50, 100, 200],
            columns: show_columns,
            responseHandler:function(res){
                return res.code===200?res.data:[];
            }
        });

        $env_save.on('click',save);

        $('#image-modal').on('show.bs.modal', function (event) {
            var target = $(event.relatedTarget);
            $image.html("");
            $.get("v1/image?version="+target.data('version'),function(result){
                if (result.code===200){
                    result.data.forEach(function (image) {
                        $image.append("<div>"+image.image+"</div>");
                    })
                }else{
                    $image.append("未查询到当前版本的镜像信息！");
                }
            })
        });

        $("#add_btn").on('click', function (event) {
            _this.editEnv({version: version})
        });
    };

    var optFormatter = function (value,row,index) {
        return ['<a class="col-sm-offset-1 edit"  href="javascript:void(0)" data-toggle="modal" data-target="#env-add" ><i title="编辑环境" class="fa fa-edit" ></i></a>',
            '<a class="col-sm-offset-1 entry"  href="javascript:void(0)" ><i title="进入环境" class="fa fa-sign-in-alt "></i></a> ',
            env?('<a class="col-sm-offset-1 image"  href="javascript:void(0)" data-toggle="modal" data-target="#image-modal" data-version="'+row.version+'"><i title="镜像版本" class="fa fa-info-circle"></i></a> '):'',
            '<a class="col-sm-offset-1 delete"  href="javascript:void(0)" data-toggle="modal" data-target="#env-delete" ><i title="删除环境" class="fa fa-trash"></i></a> '
        ].join(' ');
    };

    // 提交表单用于检查输入
    var doValid = function (data){
        if (data === undefined){
            data = formJson($env_form);
        }

        checkReset();

        var check_flag = true;
        // 检查 用户密码
        check_flag = checkActive($code,data.code)&&check_flag;
        check_flag = checkActive($name,data.name)&&check_flag;
        check_flag = checkActive($password,data.password)&&check_flag;
        check_flag = checkActive($company,data.company)&&check_flag;
        check_flag = checkActive($address,data.address)&&check_flag;
        check_flag = checkActive($maintainer,data.maintainer)&&check_flag;
        check_flag = checkActive($phone,data.phone)&&check_flag;
        check_flag = checkActive($license,data.license)&&check_flag;
        return check_flag;
    };

    // 提交保存主机信息
    var save = function () {
        var data = formJson($env_form);
        if (doValid(data)){
            $.ajax({
                url: 'v1/env',
                type:"post",
                contentType:'application/json',
                data: JSON.stringify(data),
                success:function(data) {
                    if (data.code === 200){
                        $table.bootstrapTable('refresh');
                        hideModel('env-add');
                    }
                }
            });
            checkReset();
        }
    };


    this.editEnv = function (row) {
        checkReset();
        $code.val(row.code);
        $name.val(row.name);
        $company.val(row.company);
        $address.val(row.address);
        $maintainer.val(row.maintainer);
        $phone.val(row.phone);
        $license.val(row.license);

        $hash.val(row.hash);
        $version.val(row.version);
        $createDate.val(row.createDate);
        $updateDate.val(row.updateDate);
    };

    this.deleteEnv = function () {
        var data = formJson($("#delete_form"));
        $.ajax({
            url: 'v1/env',
            type:"delete",
            contentType:'application/json',
            data: JSON.stringify(data),
            success:function(data) {
                if (data.code === 200){
                    alert("删除成功！");
                    hideModel('env-delete');
                    $table.bootstrapTable('refresh');
                }else{
                    alert(data.message);
                }
            }
        });
    };

    this.entryEnv = function () {
        var data = formJson($("#entry_form"));
        $.ajax({
            url: 'v1/env/entry',
            type:"post",
            contentType:'application/json',
            data: JSON.stringify(data),
            success:function(data) {
                if (data.code === 200){
                    location.href = 'cluster.html';
                }else{
                    alert(data.message);
                }
            }
        });
    };

    return init();
};
