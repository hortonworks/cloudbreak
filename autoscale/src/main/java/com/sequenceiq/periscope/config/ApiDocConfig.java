package com.sequenceiq.periscope.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.utils.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;

@Configuration
public class ApiDocConfig {

    @Bean
    public BeanConfig swaggerBeanConfig() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Auto-scaling API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction"));
        beanConfig.setVersion("1.2.0");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setBasePath(AutoscaleApi.API_ROOT_CONTEXT);
        beanConfig.setResourcePackage("com.sequenceiq.periscope.api");
        beanConfig.setScan(true);
        return beanConfig;
    }
}
