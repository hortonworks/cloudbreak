package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;

@Configuration
public class ApiDocConfig {

    @Bean
    public BeanConfig swaggerBeanConfig() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Cloudbreak API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        beanConfig.setVersion("1.2.0");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setBasePath("/api/v1");
        beanConfig.setResourcePackage("com.sequenceiq.cloudbreak.api");
        beanConfig.setScan(true);
        return beanConfig;
    }
}
