package com.sequenceiq.consumption.configuration.api;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPStructuredEventFilter;
import com.sequenceiq.consumption.api.v1.ConsumptionApi;
import com.sequenceiq.consumption.endpoint.ConsumptionInternalV1Controller;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@Configuration
@ApplicationPath(ConsumptionApi.API_ROOT_CONTEXT)
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            AuthorizationUtilEndpoint.class,
            CDPStructuredEventV1Controller.class,
            ConsumptionInternalV1Controller.class);

    private final String applicationVersion;

    private final boolean auditEnabled;

    private final List<ExceptionMapper<?>> exceptionMappers;

    public EndpointConfig(@Value("${info.app.version:unspecified}") String applicationVersion,
            @Value("${consumption.structuredevent.rest.enabled:true}") boolean auditEnabled,
            List<ExceptionMapper<?>> exceptionMappers) {

        this.applicationVersion = applicationVersion;
        this.auditEnabled = auditEnabled;
        this.exceptionMappers = exceptionMappers;
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    @PostConstruct
    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Consumption API");
        swaggerConfig.setDescription("Consumption operation related API.");
        swaggerConfig.setVersion(applicationVersion);
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(ConsumptionApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.consumption.api,com.sequenceiq.flow.api,com.consumption.authorization," +
                "com.sequenceiq.consumption.structuredevent.rest.endpoint");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }

    private void registerFilters() {
        register(CDPRestAuditFilter.class);
        if (auditEnabled) {
            register(CDPStructuredEventFilter.class);
        }
    }
}
