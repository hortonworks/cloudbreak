package com.sequenceiq.redbeams.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.redbeams.api.RedbeamsApi;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostConstruct
    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Redbeams API");
        swaggerConfig.setDescription("API for working with databases and database servers");
        swaggerConfig.setVersion(applicationVersion);
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(contextPath + RedbeamsApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.redbeams.api,com.sequenceiq.flow.api");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

}
