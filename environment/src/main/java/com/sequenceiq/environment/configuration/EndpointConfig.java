package com.sequenceiq.environment.configuration;

import java.util.List;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.sequenceiq.environment.env.api.EnvironmentController;
import com.sequenceiq.environment.env.api.model.EnvironmentApi;
import com.sequenceiq.environment.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.environment.exception.mapper.WebApplicaitonExceptionMapper;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(EnvironmentApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(EnvironmentController.class);

    private static final String VERSION_UNAVAILABLE = "unspecified";

    private final String applicationVersion;

    private final Boolean auditEnabled;

    private final List<ExceptionMapper<?>> exceptionMappers;

    public EndpointConfig(@Value("${info.app.version:}") String applicationVersion,
            @Value("${environment.structuredevent.rest.enabled:false}") Boolean auditEnabled,
            List<ExceptionMapper<?>> exceptionMappers) {

        this.applicationVersion = applicationVersion;
        this.auditEnabled = auditEnabled;
        this.exceptionMappers = exceptionMappers;
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Environment API");
        swaggerConfig.setDescription("Environment operation related API.");
        if (StringUtils.isEmpty(applicationVersion)) {
            swaggerConfig.setVersion(VERSION_UNAVAILABLE);
        } else {
            swaggerConfig.setVersion(applicationVersion);
        }
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(EnvironmentApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.environment.api");
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
