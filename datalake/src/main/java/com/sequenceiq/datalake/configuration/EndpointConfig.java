package com.sequenceiq.datalake.configuration;

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
import com.sequenceiq.datalake.controller.SdxEventController;
import com.sequenceiq.datalake.controller.diagnostics.DiagnosticsController;
import com.sequenceiq.datalake.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.datalake.controller.operation.OperationController;
import com.sequenceiq.datalake.controller.progress.ProgressController;
import com.sequenceiq.datalake.controller.sdx.DatabaseConfigController;
import com.sequenceiq.datalake.controller.sdx.DatabaseServerController;
import com.sequenceiq.datalake.controller.sdx.SdxBackupController;
import com.sequenceiq.datalake.controller.sdx.SdxCO2Controller;
import com.sequenceiq.datalake.controller.sdx.SdxController;
import com.sequenceiq.datalake.controller.sdx.SdxCostController;
import com.sequenceiq.datalake.controller.sdx.SdxFlowController;
import com.sequenceiq.datalake.controller.sdx.SdxInternalController;
import com.sequenceiq.datalake.controller.sdx.SdxRecipeController;
import com.sequenceiq.datalake.controller.sdx.SdxRecoveryController;
import com.sequenceiq.datalake.controller.sdx.SdxRestoreController;
import com.sequenceiq.datalake.controller.sdx.SdxRotationController;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeController;
import com.sequenceiq.datalake.controller.sdx.SupportV1Controller;
import com.sequenceiq.datalake.controller.util.UtilController;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.sdx.api.SdxApi;

import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(SdxApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            SdxController.class,
            SdxRotationController.class,
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
            SdxRecoveryController.class,
            SdxEventController.class,
            SdxCostController.class,
            SdxCO2Controller.class,
            SupportV1Controller.class,
            OpenApiController.class
    );

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
        registerSwagger();
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Data Lake API",
                "API for working with Data Lakes",
                applicationVersion,
                "https://localhost" + contextPath + SdxApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
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
