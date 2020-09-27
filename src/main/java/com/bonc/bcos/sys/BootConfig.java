package com.bonc.bcos.sys;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "boots")
public class BootConfig {
    private Boolean environment;

    private List<String> resources;

    private HashMap<String,String[]> merges;

}
