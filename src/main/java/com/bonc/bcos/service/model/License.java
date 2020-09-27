package com.bonc.bcos.service.model;

import lombok.Data;

import java.util.HashSet;

@Data
public class License {
    private String createDate;
    private String invalidDate;
    private String code;
    private HashSet<String> ips;
}