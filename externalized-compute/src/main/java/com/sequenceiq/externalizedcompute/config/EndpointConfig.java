package com.sequenceiq.externalizedcompute.config;

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.externalizedcompute.api.ExternalizedComputeClusterApi;
import com.sequenceiq.externalizedcompute.controller.ExternalizedComputeController;
import com.sequenceiq.externalizedcompute.controller.ExternalizedComputeInternalController;
import com.sequenceiq.externalizedcompute.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.flow.controller.FlowPublicController;

@ApplicationPath(ExternalizedComputeClusterApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            ExternalizedComputeController.class,
            ExternalizedComputeInternalController.class,
            FlowPublicController.class
    );

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${externalizedcompute.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @PostConstruct
    private void init() {
        registerEndpoints();
        registerExceptionMappers();
        setProperties();
    }

    private void setProperties() {
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
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
