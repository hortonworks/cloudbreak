package com.sequenceiq.consumption.configuration.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPStructuredEventFilter;
import com.sequenceiq.consumption.api.v1.ConsumptionApi;
import com.sequenceiq.consumption.endpoint.ConsumptionInternalV1Controller;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.models.OpenAPI;

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

    private static final Set<String> OPENAPI_RESOURCE_PACKAGES = Stream.of(
            "com.sequenceiq.consumption.api",
                    "com.sequenceiq.flow.api",
                    "com.consumption.authorization",
                    "com.sequenceiq.consumption.structuredevent.rest.endpoint")
            .collect(Collectors.toSet());

    private final String contextPath;

    private final String applicationVersion;

    private final boolean auditEnabled;

    private final List<ExceptionMapper<?>> exceptionMappers;

    private final OpenApiProvider openApiProvider;

    public EndpointConfig(@Value("${info.app.version:unspecified}") String applicationVersion,
            @Value("${consumption.structuredevent.rest.enabled:true}") boolean auditEnabled,
            @Value("${server.servlet.context-path:}") String contextPath,
            List<ExceptionMapper<?>> exceptionMappers,
            OpenApiProvider openApiProvider) {
        this.applicationVersion = applicationVersion;
        this.auditEnabled = auditEnabled;
        this.contextPath = contextPath;
        this.exceptionMappers = exceptionMappers;
        this.openApiProvider = openApiProvider;
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
        registerOpenApi();
    }

    private void registerOpenApi() {
        OpenApiResource openApiResource = new OpenApiResource();
        register(openApiResource);
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Consumption API",
                "Consumption operation related API.",
                applicationVersion,
                "https://localhost" + contextPath + ConsumptionApi.API_ROOT_CONTEXT
        );
        openAPI.setComponents(openApiProvider.getComponents());
        openApiProvider.createConfig(openAPI, OPENAPI_RESOURCE_PACKAGES);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);
    }

    private void registerFilters() {
        register(CDPRestAuditFilter.class);
        if (auditEnabled) {
            register(CDPStructuredEventFilter.class);
        }
    }
}
