package com.sequenceiq.periscope.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.utils.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;

@Configuration
public class ApiDocConfig {

    @Value("${info.app.version:}")
    private String cbVersion;

    @Bean
    public BeanConfig swaggerBeanConfig() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Auto-scaling API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction"));
        beanConfig.setVersion(cbVersion);
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setBasePath(AutoscaleApi.API_ROOT_CONTEXT);
        beanConfig.setResourcePackage("com.sequenceiq.periscope.api");
        beanConfig.setScan(true);
        return beanConfig;
    }
}
