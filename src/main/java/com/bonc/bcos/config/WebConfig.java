package com.bonc.bcos.config;

import com.bonc.bcos.sys.BootConfig;
import com.bonc.bcos.utils.MyDateFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import javax.annotation.Resource;
import java.text.DateFormat;

@Configuration
public class WebConfig {

    private final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;

    @Resource
    private BootConfig config;

    @Autowired
    public WebConfig(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        this.jackson2ObjectMapperBuilder = jackson2ObjectMapperBuilder;
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJsonpHttpMessageConverter() {

        ObjectMapper mapper = jackson2ObjectMapperBuilder.build();

        // ObjectMapper为了保障线程安全性，里面的配置类都是一个不可变的对象
        // 所以这里的setDateFormat的内部原理其实是创建了一个新的配置类
        DateFormat dateFormat = mapper.getDateFormat();
        mapper.setDateFormat(new MyDateFormat(dateFormat));

        return new MappingJackson2HttpMessageConverter(mapper);
    }

    @Bean
    public FilterRegistrationBean<EnvFilter> envFilter() {
        FilterRegistrationBean<EnvFilter> filter = new FilterRegistrationBean<>(new EnvFilter(config.getEnvironment()));
        filter.addUrlPatterns("/v1/*");
        filter.addUrlPatterns("/cluster.html");
        filter.addUrlPatterns("/disk.html");
        filter.addUrlPatterns("/files.html");
        filter.addUrlPatterns("/k8s.html");
        filter.addUrlPatterns("/logs.html");
        return filter;
    }
}
