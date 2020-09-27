package com.bonc.bcos.common;

import com.alibaba.fastjson.JSON;
import lombok.Data;


@Data
public class ApiResult {
    int code;
    Object data;
    String message;

    public ApiResult() {
    }

    ApiResult(int code, Object data, String message) {
        super();
        this.code = code;
        this.data = JSON.toJSON(data);
        this.message = message;
    }
}
