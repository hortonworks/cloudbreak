package com.sequenceiq.datalake.configuration;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.sequenceiq.datalake.api.DatalakeApi;
import com.sequenceiq.datalake.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.datalake.controller.mapper.WebApplicaitonExceptionMapper;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(DatalakeApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of();

    private static final String VERSION_UNAVAILABLE = "unspecified";

    @Value("${info.app.version:}")
    private String applicationVersion;

    @Value("${datalake.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @PostConstruct
    private void init() {
        /* TODO Add StructuredEventFilter, preferably as a library
            if (auditEnabled) {
                register(StructuredEventFilter.class);
            }
         */
        registerEndpoints();
        registerExceptionMappers();
    }

    @PostConstruct
    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Datalake API");
        swaggerConfig.setDescription("");
        if (StringUtils.isEmpty(applicationVersion)) {
            swaggerConfig.setVersion(VERSION_UNAVAILABLE);
        } else {
            swaggerConfig.setVersion(applicationVersion);
        }
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(DatalakeApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.datalake.api");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(WebApplicaitonExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
