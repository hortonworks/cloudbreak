package com.sequenceiq.environment.configuration.api;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPStructuredEventFilter;
import com.sequenceiq.environment.api.EnvironmentApi;
import com.sequenceiq.environment.credential.v1.AuditCredentialV1Controller;
import com.sequenceiq.environment.credential.v1.CredentialInternalV1Controller;
import com.sequenceiq.environment.credential.v1.CredentialV1Controller;
import com.sequenceiq.environment.environment.v1.EnvironmentController;
import com.sequenceiq.environment.environment.v1.EnvironmentDefaultComputeClusterController;
import com.sequenceiq.environment.environment.v1.EnvironmentInternalV1Controller;
import com.sequenceiq.environment.environment.v1.co2.EnvironmentCO2Controller;
import com.sequenceiq.environment.environment.v1.cost.EnvironmentCostController;
import com.sequenceiq.environment.marketplace.v1.controller.AzureMarketplaceTermsController;
import com.sequenceiq.environment.operation.v1.OperationController;
import com.sequenceiq.environment.platformresource.v1.CredentialPlatformResourceController;
import com.sequenceiq.environment.platformresource.v1.EnvironmentPlatformResourceController;
import com.sequenceiq.environment.proxy.v1.controller.ProxyController;
import com.sequenceiq.environment.tags.v1.controller.AccountTagController;
import com.sequenceiq.environment.telemetry.v1.controller.AccountTelemetryController;
import com.sequenceiq.environment.util.v1.UtilController;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.swagger.v3.oas.models.OpenAPI;

@Configuration
@ApplicationPath(EnvironmentApi.API_ROOT_CONTEXT)
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            CredentialV1Controller.class,
            CredentialInternalV1Controller.class,
            AuditCredentialV1Controller.class,
            AccountTagController.class,
            AccountTelemetryController.class,
            ProxyController.class,
            EnvironmentController.class,
            EnvironmentDefaultComputeClusterController.class,
            EnvironmentInternalV1Controller.class,
            OperationController.class,
            CredentialPlatformResourceController.class,
            EnvironmentPlatformResourceController.class,
            UtilController.class,
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            AuthorizationUtilEndpoint.class,
            CDPStructuredEventV1Controller.class,
            EnvironmentCostController.class,
            EnvironmentCO2Controller.class,
            AzureMarketplaceTermsController.class,
            CDPEventV1Endpoint.class,
            OpenApiController.class);

    private final String contextPath;

    private final String applicationVersion;

    private final Boolean auditEnabled;

    private final List<ExceptionMapper<?>> exceptionMappers;

    private final List<MessageBodyWriter<?>> messageBodyWriters;

    private final OpenApiProvider openApiProvider;

    public EndpointConfig(@Value("${info.app.version:unspecified}") String applicationVersion,
            @Value("${environment.structuredevent.rest.enabled}") Boolean auditEnabled,
            @Value("${server.servlet.context-path:}") String contextPath,
            List<ExceptionMapper<?>> exceptionMappers,
            List<MessageBodyWriter<?>> messageBodyWriters,
            OpenApiProvider openApiProvider) {
        this.applicationVersion = applicationVersion;
        this.auditEnabled = auditEnabled;
        this.contextPath = contextPath;
        this.exceptionMappers = exceptionMappers;
        this.messageBodyWriters = messageBodyWriters;
        this.openApiProvider = openApiProvider;
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerMessageBodyWriters();
        registerSwagger();
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Environment API",
                "API for working with Environment related operations",
                applicationVersion,
                "https://localhost" + contextPath + EnvironmentApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
    }

    private void registerMessageBodyWriters() {
        for (MessageBodyWriter<?> writer : messageBodyWriters) {
            register(writer);
        }
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
