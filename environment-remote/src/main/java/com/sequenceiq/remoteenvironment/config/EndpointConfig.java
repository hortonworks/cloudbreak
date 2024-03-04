package com.sequenceiq.remoteenvironment.config;

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.remoteenvironment.api.RemoteEnvironmentApi;
import com.sequenceiq.remoteenvironment.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.remoteenvironment.controller.v1.controller.RemoteControlPlaneController;
import com.sequenceiq.remoteenvironment.controller.v1.controller.RemoteEnvironmentController;

@ApplicationPath(RemoteEnvironmentApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            RemoteEnvironmentController.class,
            RemoteControlPlaneController.class,
            FlowPublicController.class,
            OpenApiController.class
    );

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${remoteenvironment.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @PostConstruct
    private void init() {
        registerEndpoints();
        registerExceptionMappers();
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
    }
}
