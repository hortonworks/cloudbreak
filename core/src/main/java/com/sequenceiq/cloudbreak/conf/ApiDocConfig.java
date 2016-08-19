package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;

@Configuration
public class ApiDocConfig {

    @Value("${info.app.version:}")
    private String cbVersion;

    @Bean
    public BeanConfig swaggerBeanConfig() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Cloudbreak API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        beanConfig.setVersion(cbVersion);
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setBasePath(CoreApi.API_ROOT_CONTEXT);
        beanConfig.setResourcePackage("com.sequenceiq.cloudbreak.api");
        beanConfig.setScan(true);
        return beanConfig;
    }
}
