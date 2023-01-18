package com.sequenceiq.datalake.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
import com.sequenceiq.datalake.controller.SdxEventController;
import com.sequenceiq.datalake.controller.diagnostics.DiagnosticsController;
import com.sequenceiq.datalake.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.datalake.controller.operation.OperationController;
import com.sequenceiq.datalake.controller.progress.ProgressController;
import com.sequenceiq.datalake.controller.sdx.DatabaseConfigController;
import com.sequenceiq.datalake.controller.sdx.DatabaseServerController;
import com.sequenceiq.datalake.controller.sdx.SdxBackupController;
import com.sequenceiq.datalake.controller.sdx.SdxController;
import com.sequenceiq.datalake.controller.sdx.SdxCostController;
import com.sequenceiq.datalake.controller.sdx.SdxFlowController;
import com.sequenceiq.datalake.controller.sdx.SdxInternalController;
import com.sequenceiq.datalake.controller.sdx.SdxRecipeController;
import com.sequenceiq.datalake.controller.sdx.SdxRecoveryController;
import com.sequenceiq.datalake.controller.sdx.SdxRestoreController;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeController;
import com.sequenceiq.datalake.controller.util.UtilController;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.sdx.api.SdxApi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(SdxApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            SdxController.class,
            SdxUpgradeController.class,
            SdxInternalController.class,
            DatabaseConfigController.class,
            UtilController.class,
            SdxFlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            DiagnosticsController.class,
            ProgressController.class,
            OperationController.class,
            AuthorizationUtilEndpoint.class,
            DatabaseServerController.class,
            SdxBackupController.class,
            SdxRestoreController.class,
            SdxRecipeController.class,
            CDPStructuredEventV1Controller.class,
            SdxRecoveryController.class,
            SdxEventController.class,
            SdxCostController.class
    );

    private static final Set<String> OPENAPI_RESOURCE_PACKAGES = Stream.of(
            "com.sequenceiq.sdx.api",
                    "com.sequenceiq.flow.api",
                    "com.sequenceiq.authorization")
            .collect(Collectors.toSet());

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${datalake.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    private void init() {
        register(CDPRestAuditFilter.class);
        registerEndpoints();
        registerExceptionMappers();
        registerOpenApi();
    }

    private void registerOpenApi() {
        OpenApiResource openApiResource = new OpenApiResource();
        register(openApiResource);
    }

    @PostConstruct
    public void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Data Lake API",
                "API for working with Data Lakes",
                applicationVersion,
                "https://localhost" + contextPath + SdxApi.API_ROOT_CONTEXT
        );
        openAPI.setComponents(openApiProvider.getComponents());
        openApiProvider.createConfig(openAPI, OPENAPI_RESOURCE_PACKAGES);
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
