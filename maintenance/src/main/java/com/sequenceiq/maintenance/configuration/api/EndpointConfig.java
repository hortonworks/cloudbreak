package com.sequenceiq.maintenance.configuration.api;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.maintenance.api.MaintenanceApi;
import com.sequenceiq.maintenance.controller.MaintenanceWindowScheduleController;
import com.sequenceiq.maintenance.controller.MaintenanceWindowTaskInternalController;

import io.swagger.v3.oas.models.OpenAPI;

@Configuration
@ApplicationPath(MaintenanceApi.API_ROOT_CONTEXT)
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            MaintenanceWindowScheduleController.class,
            MaintenanceWindowTaskInternalController.class,
            AuthorizationInfoController.class,
            AuthorizationUtilEndpoint.class,
            OpenApiController.class
    );

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    private void init() {
        register(CDPRestAuditFilter.class);
        CONTROLLERS.forEach(this::register);
        exceptionMappers.forEach(this::register);
        register(DefaultExceptionMapper.class);
        registerSwagger();
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Maintenance API",
                "API for maintenance window schedules, task registration, and skip rules",
                applicationVersion,
                "https://localhost" + contextPath + MaintenanceApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
    }
}
