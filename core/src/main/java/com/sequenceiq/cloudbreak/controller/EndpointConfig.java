package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;
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

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.controller.v4.AuditEventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.AutoscaleV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintUtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudProviderServicesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudbreakInfoV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ClusterCO2V4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ClusterCostV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ClusterTemplateV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CustomConfigurationsV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CustomImageCatalogV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseConfigV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatalakeV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DiagnosticsV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DiskUpdateController;
import com.sequenceiq.cloudbreak.controller.v4.EncryptionV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.EventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.FileSystemV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ImageCatalogV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.OperationV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ProgressV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.RecipesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.RestartInstancesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.StackV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UserProfileV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.WorkspaceAwareUtilV4Controller;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyStructuredEventFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXCO2V1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXCostV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXDatabaseServerV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXInternalV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXKraftMigrationV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXUpgradeV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXV1EventController;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXV1RotationController;
import com.sequenceiq.distrox.v1.support.controller.SupportV1Controller;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            AuditEventV4Controller.class,
            BlueprintV4Controller.class,
            BlueprintUtilV4Controller.class,
            CustomConfigurationsV4Controller.class,
            EventV4Controller.class,
            DistroXV1EventController.class,
            ClusterTemplateV4Controller.class,
            DatabaseV4Controller.class,
            DatabaseConfigV4Controller.class,
            ImageCatalogV4Controller.class,
            CustomImageCatalogV4Controller.class,
            RecipesV4Controller.class,
            UserProfileV4Controller.class,
            FileSystemV4Controller.class,
            UtilV4Controller.class,
            WorkspaceAwareUtilV4Controller.class,
            AutoscaleV4Controller.class,
            RestartInstancesV4Controller.class,
            StackV4Controller.class,
            CloudbreakInfoV4Controller.class,
            DistroXV1Controller.class,
            DistroXV1RotationController.class,
            DistroXKraftMigrationV1Controller.class,
            DistroXInternalV1Controller.class,
            DatalakeV4Controller.class,
            DiskUpdateController.class,
            DiagnosticsV4Controller.class,
            ProgressV4Controller.class,
            OperationV4Controller.class,
            CloudProviderServicesV4Controller.class,
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            DistroXUpgradeV1Controller.class,
            DistroXDatabaseServerV1Controller.class,
            AuthorizationUtilEndpoint.class,
            ClusterCostV4Controller.class,
            DistroXCostV1Controller.class,
            ClusterCO2V4Controller.class,
            DistroXCO2V1Controller.class,
            EncryptionV4Controller.class,
            SupportV1Controller.class,
            OpenApiController.class
    );

    @Value("${info.app.version:unspecified}")
    private String cbVersion;

    @Value("${cb.structuredevent.rest.enabled}")
    private Boolean auditEnabled;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    private void init() throws IOException {
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() throws IOException {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Cloudbreak API",
                FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"),
                cbVersion,
                "https://localhost" + contextPath + CoreApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
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
            register(LegacyStructuredEventFilter.class);
        }
    }
}
