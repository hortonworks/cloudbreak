package com.sequenceiq.remoteenvironment.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.remoteenvironment.api.RemoteEnvironmentApi;
import com.sequenceiq.remoteenvironment.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.remoteenvironment.controller.v1.controller.PrivateControlPlaneController;
import com.sequenceiq.remoteenvironment.controller.v1.controller.RemoteEnvironmentController;

import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(RemoteEnvironmentApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            RemoteEnvironmentController.class,
            PrivateControlPlaneController.class,
            FlowPublicController.class,
            OpenApiController.class
    );

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${remoteenvironment.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    private void init() {
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(WebApplicaitonExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Remote Environment API",
                "API for working with Remote Environment related operations",
                applicationVersion,
                "https://localhost" + contextPath + RemoteEnvironmentApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);
    }
}
