/**
 *  form 表单封装的类
 * @param table
 * @constructor
 */

ipRegex = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
valid_ip = function (ip) {
    return ipRegex.test(ip);
};
valid_empty_ip = function (ip) {
    return ip===''||ipRegex.test(ip);
};
valid_empty = function(value){
    return '' !== value;
};
valid_license = function(license){
    var code=$("#code").val();
    var valid = false;
    $.ajax({
        url: 'v1/env/license',
        type:"post",
        contentType:'application/json',
        async: false,
        data: JSON.stringify({license:license,code:code}),
        success:function(data) {
            valid = data.code===200&&data.data;
        },
        error:function () {
            valid = false;
        }
    });
    return valid;
};
valid_ips = function (ip) {
    var ips = ip.split(",");
    var ip_valid = true;
    if (ips.length>0){
        ips.forEach(function (cur_ip) {
            ip_valid = ip_valid && ipRegex.test(polishIp(ips[0],cur_ip));
        });
    }else{
        ip_valid = false;
    }
    return ip_valid;
};
valid_len = function (value) {
    return  value.length<32&&value.length>3;
};
valid_port = function (port) {
    return port>0&&port<65535;
};
valid_empty_port = function (port) {
    return port===''||port>0&&port<65535;
};
valid_bool = function(flag){
    flag = flag.toLowerCase();
    return flag==='true'||flag==='false';
};

bind_valid= function ($input,memo,invoke) {
    $input.parent().append('<div class="invalid-feedback">'+memo+'!</div>');
    $input.valid_invoke = invoke;
    $input.bind('input propertychange',function () {
        checkReset($input);
        checkActive($input);
    });
};

// 清空input 样式
checkReset = function ($this) {
    // 清楚单个节点的校验展示
    if ($this!==undefined){
        if($this.hasClass("is-valid")){$this.removeClass("is-valid");}
        if($this.hasClass("is-invalid")){$this.removeClass("is-invalid");}
    }else{ // 清楚所有input 的校验展示
        $(".is-valid").each(function () {$(this).removeClass("is-valid");});
        $(".is-invalid").each(function () {$(this).removeClass("is-invalid");})
    }
};

// 校验IP 是否合法
checkActive = function($this,value){
    if(value === undefined){
        value = $this.val();
    }
    var valid = true;
    if (typeof $this.valid_invoke==='function'){
        valid = $this.valid_invoke(value);
    }
    if (valid&&!$this.hasClass("is-valid")){
        $this.addClass("is-valid");
    }
    if(!valid&&!$this.hasClass("is-invalid")){
        $this.addClass("is-invalid");
    }
    return valid;
};

//IP转数字
ip2int = function(ip) {
    ip = ip.split(".");
    var num = Number(ip[0]) * 256 * 256 * 256 + Number(ip[1]) * 256 * 256 + Number(ip[2]) * 256 + Number(ip[3]);
    num = num >>> 0;
    return num;
};

//数字转IP
int2ip = function(num) {
    var str;
    var tt = [];
    tt[0] = (num >>> 24) >>> 0;
    tt[1] = ((num << 8) >>> 24) >>> 0;
    tt[2] = (num << 16) >>> 24;
    tt[3] = (num << 24) >>> 24;
    str = String(tt[0]) + "." + String(tt[1]) + "." + String(tt[2]) + "." + String(tt[3]);
    return str;
};

// 根据第一个ip 段补齐剩余的ip
polishIp = function(example,ip) {
    if (example.split(".").length!==4){
        return example;
    }
    var ip_seg = example.split(".").slice(0,4-ip.split(".").length);
    ip_seg.push(ip);
    return ip_seg.join(".");
};

formJson = function ($form) {
    var data = {};
    $form.serializeArray().forEach(function (row) {
        data[row.name] = row.value.trim();
    });
    return data;
};

valid_code = function(value){
    return '' !== value;
};

dateFtt = function(fmt,date)
{ //author: meizz
    var o = {
        "M+" : date.getMonth()+1,                 //月份
        "d+" : date.getDate(),                    //日
        "h+" : date.getHours(),                   //小时
        "m+" : date.getMinutes(),                 //分
        "s+" : date.getSeconds(),                 //秒
        "q+" : Math.floor((date.getMonth()+3)/3), //季度
        "S"  : date.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt))
        fmt=fmt.replace(RegExp.$1, (date.getFullYear()+"").substr(4 - RegExp.$1.length));
    for(var k in o)
        if(new RegExp("("+ k +")").test(fmt))
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
    return fmt;
};

submitGlobal = function(data){
    var flag = true;
    $.ajax({
        url: "v1/global",
        type:"post",
        async: false,
        contentType:'application/json',
        data: JSON.stringify(data),
        success:function(data){
            if (data.code !== 200 ){
                flag = false;
            }
        }
    });
    return flag;
};

tools_menus = function(play,$parent){
    if ($parent.is("button")){
        return;
    }
    var drop_class={true:'dropleft',false:'dropright'};
    var left_flag = false;
    var playCode = play.playCode;
    var top_flag = $parent.hasClass("top-tool");
    var item_class = top_flag?('btn '+play.color):'dropdown-item';
    var item_icon = 'fa '+play.icon+' '+(top_flag?'':play.color);
    var item_divide = top_flag?'':'<div class="dropdown-divider"></div>';
    var icon_item={true:play.playName,false:'<i class="'+item_icon+'" ></i> '};

    if (play.status==='2'){
        if (top_flag){
            $parent.append([
                '<button  class="dropdown-toggle '+item_class+'" type="button" data-toggle="dropdown" data-submenu>',
                icon_item[false],' ',icon_item[true],
                '</button>',
                '<div class="dropdown-menu" id="',playCode,'" >',
                '</div>',item_divide
            ].join(''));
        }else{
            $parent.append([
                '<div class="dropdown ',drop_class[left_flag],' dropdown-submenu" >',
                '   <button  class="dropdown-item dropdown-toggle" type="button" data-toggle="dropdown">',
                icon_item[left_flag], left_flag?'&nbsp;&nbsp;&nbsp;':' ',icon_item[!left_flag],
                '   </button>',
                '   <div class="dropdown-menu" id="',playCode,'" ></div>',
                '</div>',item_divide
            ].join(''));
        }
    }else if (play.status!=='0'){
        if (top_flag){
            $parent.append([
                '<button class="',item_class,'"  id="',playCode,'" type="button" data-loading-text="执行中...." title="',play.playDesc,'">',
                (left_flag?'&nbsp;&nbsp;':' '),icon_item[left_flag],top_flag?' ':'&nbsp;&nbsp;&nbsp;',icon_item[!left_flag],
                '</button>',item_divide
            ].join(''));
        }else{
            $parent.prepend([
                '<button class="',item_class,'"  id="',playCode,'" type="button" data-loading-text="执行中...." title="',play.playDesc,'">',
                (left_flag?'&nbsp;&nbsp;':' '),icon_item[left_flag],top_flag?' ':'&nbsp;&nbsp;&nbsp;',icon_item[!left_flag],
                '</button>',item_divide
            ].join(''));
        }
    }
    $parent.append("\n");
};

hideModel = function (id) {
    $("#"+id).modal('hide');
};

showModel = function (id) {
    $("#"+id).modal('show');
};

dateFormatter =function (value) {
    if (null===value || undefined === value){
        return "--"
    }
    return dateFtt("yyyy-MM-dd hh:mm:ss",new Date(value));
};